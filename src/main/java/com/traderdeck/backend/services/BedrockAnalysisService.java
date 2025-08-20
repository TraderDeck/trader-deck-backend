package com.traderdeck.backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traderdeck.backend.dto.AnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

import jakarta.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
@Slf4j
public class BedrockAnalysisService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${aws.region}")
    private String awsRegion;

    @Value("${aws.bedrock.flow-id}")
    private String flowId;

    @Value("${aws.bedrock.flow-alias-id}")
    private String flowAliasId;

    @Value("${aws.access-key-id}")
    private String awsAccessKeyId;

    @Value("${aws.secret-access-key}")
    private String awsSecretAccessKey;

    @Value("${bedrock.mock.enabled:false}")
    private boolean bedrockMockEnabled;

    @PostConstruct
    public void logMockFlag() {
        log.info("bedrock.mock.enabled={} (service will use: {})", bedrockMockEnabled, bedrockMockEnabled ? "MOCK" : "HTTP");
    }

    public AnalysisResponse analyzeStock(String tickerSymbol, String userPrompt, UUID userId) {
        try {
            String inputText = tickerSymbol;
            if (userPrompt != null && !userPrompt.trim().isEmpty()) {
                inputText += ". Additional context: " + userPrompt;
            }

            log.info("Invoking Bedrock flow {} for ticker: {} via HTTP", flowId, tickerSymbol);

            // Always call real Bedrock - remove mock branching
            String responseBody = invokeBedrockFlowHttp(flowId, flowAliasId, inputText);

            log.info("Bedrock flow response received for ticker: {} ({} chars)", tickerSymbol, responseBody != null ? responseBody.length() : 0);
            return parseFlowResponse(responseBody, tickerSymbol);

        } catch (Exception e) {
            log.error("Error calling Bedrock flow for ticker {}: {}", tickerSymbol, e.getMessage(), e);
            return createFallbackResponse(tickerSymbol, e.getMessage());
        }
    }

    private String invokeBedrockFlowHttp(String flowId, String flowAliasId, String inputData) {
        try {
            // Bedrock Flow API endpoint
            String url = String.format("https://bedrock-agent-runtime.%s.amazonaws.com/flows/%s/aliases/%s/invoke", 
                                     awsRegion, flowId, flowAliasId);
            
            // Request body: inputs as an array of nodes, each with a content array of text blocks
            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> inputs = new ArrayList<>();

            Map<String, Object> inputNode = new HashMap<>();
            inputNode.put("nodeName", "FlowInputNode");
            inputNode.put("nodeOutputName", "document");

            List<Map<String, Object>> contentBlocks = new ArrayList<>();
            Map<String, Object> textBlock = new HashMap<>();
            textBlock.put("text", inputData);
            contentBlocks.add(textBlock);

            inputNode.put("content", contentBlocks);
            inputs.add(inputNode);

            requestBody.put("inputs", inputs);
            
            String requestJson = objectMapper.writeValueAsString(requestBody);
            log.info("Sending input to Bedrock Flow: {}", requestJson);
            
            // Create HTTP headers with AWS signature
            HttpHeaders headers = createAwsSignedHeaders(url, requestJson);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            
            // Make the HTTP request
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Bedrock Flow Response: {}", response.getBody());
                return response.getBody();
            } else {
                throw new RuntimeException("HTTP request failed with status: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Error making HTTP request to Bedrock Flow: {}", e.getMessage(), e);
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                org.springframework.web.client.HttpClientErrorException httpError = 
                    (org.springframework.web.client.HttpClientErrorException) e;
                log.error("Bedrock Flow Error: Status={}, Body={}", 
                         httpError.getStatusCode(), httpError.getResponseBodyAsString());
            }
            throw new RuntimeException("Failed to invoke Bedrock Flow via HTTP", e);
        }
    }

    private HttpHeaders createAwsSignedHeaders(String url, String requestBody) {
        try {
            String service = "bedrock";
            String method = "POST";
            String canonicalUri = extractPath(url);
            String host = extractHost(url);
            
            // Create timestamp
            Date now = new Date();
            SimpleDateFormat amzDateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            amzDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String amzDate = amzDateFormat.format(now);
            
            SimpleDateFormat dateStampFormat = new SimpleDateFormat("yyyyMMdd");
            dateStampFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String dateStamp = dateStampFormat.format(now);
            
            // Create payload hash
            String payloadHash = sha256Hex(requestBody != null ? requestBody : "");
            
            // Create canonical headers (sorted)
            Map<String, String> headers = new TreeMap<>();
            headers.put("content-type", "application/json");
            headers.put("host", host);
            headers.put("x-amz-content-sha256", payloadHash);
            headers.put("x-amz-date", amzDate);
            
            StringBuilder canonicalHeaders = new StringBuilder();
            StringBuilder signedHeaders = new StringBuilder();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                canonicalHeaders.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
                if (signedHeaders.length() > 0) signedHeaders.append(";");
                signedHeaders.append(entry.getKey());
            }
            
            // Create canonical request
            String canonicalRequest = method + "\n" +
                    canonicalUri + "\n" +
                    "" + "\n" + // Query string (empty)
                    canonicalHeaders + "\n" +
                    signedHeaders + "\n" +
                    payloadHash;
            
            // Create string to sign
            String credentialScope = dateStamp + "/" + awsRegion + "/" + service + "/aws4_request";
            String stringToSign = "AWS4-HMAC-SHA256\n" +
                    amzDate + "\n" +
                    credentialScope + "\n" +
                    sha256Hex(canonicalRequest);
            
            // Calculate signature
            byte[] signingKey = getSignatureKey(awsSecretAccessKey, dateStamp, awsRegion, service);
            String signature = bytesToHex(hmacSha256(stringToSign, signingKey));
            
            // Create authorization header
            String authorization = "AWS4-HMAC-SHA256 " +
                    "Credential=" + awsAccessKeyId + "/" + credentialScope + ", " +
                    "SignedHeaders=" + signedHeaders + ", " +
                    "Signature=" + signature;
            
            // Build final headers
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.set("Host", host);
            httpHeaders.set("X-Amz-Date", amzDate);
            httpHeaders.set("X-Amz-Content-Sha256", payloadHash);
            httpHeaders.set("Authorization", authorization);
            
            return httpHeaders;
            
        } catch (Exception e) {
            log.error("Error creating AWS signed headers: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to sign AWS request", e);
        }
    }

    private String extractHost(String url) {
        return url.split("://")[1].split("/")[0];
    }

    private String extractPath(String url) {
        String[] parts = url.split("://")[1].split("/", 2);
        return parts.length > 1 ? "/" + parts[1] : "/";
    }

    private String sha256Hex(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    private byte[] hmacSha256(String data, byte[] key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception {
        byte[] kDate = hmacSha256(dateStamp, ("AWS4" + key).getBytes(StandardCharsets.UTF_8));
        byte[] kRegion = hmacSha256(regionName, kDate);
        byte[] kService = hmacSha256(serviceName, kRegion);
        return hmacSha256("aws4_request", kService);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private AnalysisResponse parseFlowResponse(String responseBody, String tickerSymbol) {
        try {
            log.debug("Parsing flow response for ticker: {}", tickerSymbol);
            log.info("=== RAW BEDROCK RESPONSE ===");
            log.info("Full response: {}", responseBody);
            log.info("=== END RAW RESPONSE ===");

            Map<String, String> agentResponses = new HashMap<>();

            // First try: parse as structured JSON { outputs: [ { nodeName, content: [{text|document}...] }, ... ] }
            boolean parsedStructured = false;
            if (responseBody != null && responseBody.trim().startsWith("{")) {
                try {
                    JsonNode root = objectMapper.readTree(responseBody);
                    JsonNode outputs = root.get("outputs");
                    if (outputs != null && outputs.isArray()) {
                        for (JsonNode out : outputs) {
                            String nodeName = out.path("nodeName").asText("");
                            if (nodeName.isEmpty()) continue;
                            StringBuilder contentBuilder = new StringBuilder();
                            JsonNode contentArr = out.get("content");
                            if (contentArr != null && contentArr.isArray()) {
                                for (JsonNode block : contentArr) {
                                    if (block.has("text")) contentBuilder.append(block.get("text").asText());
                                    else if (block.has("document")) contentBuilder.append(block.get("document").asText());
                                    if (contentBuilder.length() > 0) contentBuilder.append("\n");
                                }
                            }
                            String content = contentBuilder.toString().trim();
                            if (!content.isEmpty()) agentResponses.put(nodeName, content);
                        }
                        parsedStructured = !agentResponses.isEmpty();
                    }
                } catch (Exception ignore) {
                    // Fall back to event-stream parsing
                }
            }

            // Fallback: parse event-stream style text
            if (!parsedStructured) {
                agentResponses = extractAgentResponsesFromText(responseBody != null ? responseBody : "");
            }

            log.info("Extracted {} agent responses", agentResponses.size());

            String tradeContent = agentResponses.getOrDefault("Trades_agent_output", "");
            String technicalContent = agentResponses.getOrDefault("Technicals_agent_output", "");
            String fundamentalsContent = agentResponses.getOrDefault("Fundamentals_agent_output", "");
            String newsContent = agentResponses.getOrDefault("News_agent_output", "");

            AnalysisResponse.TradeAgentResponse tradeAgent = parseTradeAgent(tradeContent);
            AnalysisResponse.TechnicalAgentResponse technicalAgent = parseTechnicalAgent(technicalContent);
            AnalysisResponse.FundamentalsAgentResponse fundamentalsAgent = parseFundamentalsAgent(fundamentalsContent);
            AnalysisResponse.NewsAgentResponse newsAgent = parseNewsAgent(newsContent);

            return AnalysisResponse.builder()
                    .tickerSymbol(tickerSymbol)
                    .tradeAgent(tradeAgent)
                    .technicalAgent(technicalAgent)
                    .fundamentalsAgent(fundamentalsAgent)
                    .newsAgent(newsAgent)
                    .build();

        } catch (Exception e) {
            log.error("Error parsing Bedrock flow response: {}", e.getMessage(), e);
            return createFallbackResponse(tickerSymbol, "Error parsing flow response");
        }
    }
    
    private Map<String, String> extractAgentResponsesFromText(String response) {
        Map<String, String> agentResponses = new HashMap<>();
        
        try {
            // Clean the response of control characters first
            String cleanedResponse = response.replaceAll("[\\x00-\\x1F\\x7F]", " ");
            
            // Look for the pattern: "nodeName":"agent_name" followed by "document":"content"
            String[] patterns = {
                "Technicals_agent_output",
                "News_agent_output", 
                "Trades_agent_output",
                "Fundamentals_agent_output"
            };
            
            for (String pattern : patterns) {
                String content = extractContentForAgent(cleanedResponse, pattern);
                if (!content.isEmpty()) {
                    agentResponses.put(pattern, content);
                    log.debug("Extracted content for {}: {} characters", pattern, content.length());
                }
            }
            
        } catch (Exception e) {
            log.warn("Error extracting agent responses: {}", e.getMessage());
        }
        
        return agentResponses;
    }
    
    private String extractContentForAgent(String response, String agentName) {
        try {
            // Find the agent name in the response
            int agentIndex = response.indexOf(agentName);
            if (agentIndex == -1) return "";
            
            // Look for the document content after the agent name
            int documentIndex = response.indexOf("\"document\":\"", agentIndex);
            if (documentIndex == -1) return "";
            
            // Start after the "document":"
            int contentStart = documentIndex + 12;
            
            // Find the end of the document content (look for the closing quote before next field)
            int contentEnd = findDocumentEnd(response, contentStart);
            
            if (contentEnd > contentStart) {
                String content = response.substring(contentStart, contentEnd);
                // Clean up escaped characters
                return content.replace("\\n", "\n")
                            .replace("\\\"", "\"")
                            .replace("\\t", "\t")
                            .trim();
            }
            
        } catch (Exception e) {
            log.warn("Error extracting content for {}: {}", agentName, e.getMessage());
        }
        
        return "";
    }
    
    private int findDocumentEnd(String response, int start) {
        int pos = start;
        boolean inEscape = false;
        
        while (pos < response.length()) {
            char c = response.charAt(pos);
            
            if (inEscape) {
                inEscape = false;
            } else if (c == '\\') {
                inEscape = true;
            } else if (c == '"') {
                // Check if this is the end of document content
                if (pos + 1 < response.length() && 
                    (response.charAt(pos + 1) == ',' || response.charAt(pos + 1) == '}')) {
                    return pos;
                }
            }
            pos++;
        }
        
        return response.length();
    }
    
    private String extractFundamentalsFromResponse(String response) {
        // Look for fundamentals content that might be embedded differently
        if (response.contains("Red Flags") || response.contains("Green Flags")) {
            int start = Math.max(
                response.indexOf("Red Flags"),
                response.indexOf("Green Flags")
            );
            if (start != -1) {
                int end = response.indexOf("Based on the provided", start);
                if (end == -1) end = start + Math.min(2000, response.length() - start);
                return response.substring(start, end).trim();
            }
        }
        return "";
    }
    
    private AnalysisResponse.TradeAgentResponse parseTradeAgent(String content) {
        // Extract key information from the trade agent response
        String recommendation = extractValue(content, "Decision:", "\n");
        String reasoning = extractValue(content, "Why:", "Trade Plan:");
        String confidence = extractValue(content, "buy_score:", "/");
        
        return AnalysisResponse.TradeAgentResponse.builder()
                .recommendation(recommendation.isEmpty() ? "N/A" : recommendation)
                .reasoning(reasoning.isEmpty() ? content : reasoning) // Remove 500 char limit
                .confidence(confidence.isEmpty() ? "N/A" : confidence + "/10")
                .build();
    }
    
    private AnalysisResponse.TechnicalAgentResponse parseTechnicalAgent(String content) {
        // Extract technical analysis information
        String analysis = content; // Remove 1000 char limit
        String signals = extractValue(content, "Investment Insights", "");
        String indicators = extractValue(content, "RSI", "Volume");
        
        return AnalysisResponse.TechnicalAgentResponse.builder()
                .analysis(analysis.isEmpty() ? "N/A" : analysis)
                .signals(signals.isEmpty() ? "See full analysis" : signals)
                .indicators(indicators.isEmpty() ? "RSI, Moving Averages, Volume" : indicators)
                .build();
    }
    
    private AnalysisResponse.FundamentalsAgentResponse parseFundamentalsAgent(String content) {
        String analysis = content; // Remove 1000 char limit
        String valuation = "See fundamentals analysis";
        String metrics = extractValue(content, "ROIC:", "Earnings");
        
        return AnalysisResponse.FundamentalsAgentResponse.builder()
                .analysis(analysis.isEmpty() ? "N/A" : analysis)
                .valuation(valuation)
                .metrics(metrics.isEmpty() ? "ROIC, P/E, Cash Flow" : metrics)
                .build();
    }
    
    private AnalysisResponse.NewsAgentResponse parseNewsAgent(String content) {
        log.info("=== PARSING NEWS AGENT ===");
        log.info("Input content: {}", content);
        
        String sentiment = content.contains("Bullish") ? "Bullish" : 
                          content.contains("Bearish") ? "Bearish" : "Neutral";
        String summary = content; // Remove 800 char limit
        String impact = extractValue(content, "Confidence Level:", "");
        
        log.info("Parsed sentiment: {}", sentiment);
        log.info("Parsed summary: {}", summary);
        log.info("Parsed impact: {}", impact);
        
        return AnalysisResponse.NewsAgentResponse.builder()
                .sentiment(sentiment)
                .summary(summary.isEmpty() ? "N/A" : summary)
                .impact(impact.isEmpty() ? "Moderate" : impact)
                .build();
    }
    
    private String extractValue(String text, String startMarker, String endMarker) {
        int startIndex = text.indexOf(startMarker);
        if (startIndex == -1) return "";
        
        startIndex += startMarker.length();
        
        if (endMarker.isEmpty()) {
            return text.substring(startIndex).trim();
        }
        
        int endIndex = text.indexOf(endMarker, startIndex);
        if (endIndex == -1) {
            return text.substring(startIndex).trim();
        }
        
        return text.substring(startIndex, endIndex).trim();
    }

    private AnalysisResponse createFallbackResponse(String tickerSymbol, String errorMessage) {
        return AnalysisResponse.builder()
                .tickerSymbol(tickerSymbol)
                .tradeAgent(AnalysisResponse.TradeAgentResponse.builder()
                        .recommendation("ERROR")
                        .reasoning("Service temporarily unavailable: " + errorMessage)
                        .confidence("Low")
                        .build())
                .technicalAgent(AnalysisResponse.TechnicalAgentResponse.builder()
                        .analysis("Service temporarily unavailable")
                        .signals("N/A")
                        .indicators("N/A")
                        .build())
                .fundamentalsAgent(AnalysisResponse.FundamentalsAgentResponse.builder()
                        .analysis("Service temporarily unavailable")
                        .valuation("N/A")
                        .metrics("N/A")
                        .build())
                .newsAgent(AnalysisResponse.NewsAgentResponse.builder()
                        .sentiment("Neutral")
                        .summary("Service temporarily unavailable")
                        .impact("Unknown")
                        .build())
                .build();
    }
}