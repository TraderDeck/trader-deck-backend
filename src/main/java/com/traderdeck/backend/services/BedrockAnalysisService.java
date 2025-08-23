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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Value("${bedrock.mock.enabled}")
    private boolean bedrockMockEnabled = false;

    @Value("${bedrock.raw.log-file:bedrock-flow-response-log.txt}")
    private String rawLogFile;

    public AnalysisResponse analyzeStock(String tickerSymbol, String userPrompt, UUID userId) {
        System.out.println("ENTER analyzeStock ticker=" + tickerSymbol + " userId=" + userId + " promptLen=" + (userPrompt==null?0:userPrompt.length()));
        try {
            log.info("AnalyzeStock invoked: ticker={}, userId={}, promptChars={}", tickerSymbol, userId, userPrompt == null ? 0 : userPrompt.length());
            if (bedrockMockEnabled) {
                return createMockResponse(tickerSymbol, userPrompt);
            }
            String inputText = tickerSymbol + (userPrompt != null && !userPrompt.isBlank()?". Additional context: "+userPrompt:"");
            String responseBody = invokeBedrockFlowHttp(flowId, flowAliasId, inputText);
            // appendRawBedrockResponse(tickerSymbol, responseBody);
            return parseUnifiedAgentsResponse(responseBody, tickerSymbol);
        } catch (Exception e) {
            log.error("Error calling Bedrock flow: {}", e.getMessage(), e);
            return createFallbackResponse(tickerSymbol, e.getMessage());
        }
    }

    // private void appendRawBedrockResponse(String tickerSymbol, String responseBody) {
    //     System.out.println("ENTER appendRawBedrockResponse ticker=" + tickerSymbol + " bodySize=" + (responseBody==null?0:responseBody.length()));
    //     if (responseBody == null) { System.out.println("EXIT appendRawBedrockResponse null body"); return; }
    //     try {
    //         String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
    //         String header = "\n==== RAW BEDROCK RESPONSE START " + timestamp + " ticker=" + tickerSymbol + " ====\n";
    //         String footer = "\n==== RAW BEDROCK RESPONSE END " + timestamp + " ticker=" + tickerSymbol + " ====\n";
    //         Path path = Path.of(rawLogFile);
    //         if (!Files.exists(path)) {
    //             Files.writeString(path, header + responseBody + footer, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
    //         } else {
    //             Files.writeString(path, header + responseBody + footer, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    //         }
    //         log.info("Raw Bedrock response appended to {} (ticker={}, size={})", rawLogFile, tickerSymbol, responseBody.length());
    //     } catch (Exception ex) {
    //         log.warn("Failed to write raw Bedrock response to file: {}", ex.getMessage());
    //     }
    //     System.out.println("EXIT appendRawBedrockResponse ticker=" + tickerSymbol);
    // }

    private String invokeBedrockFlowHttp(String flowId, String flowAliasId, String inputData) {
        System.out.println("ENTER invokeBedrockFlowHttp flowId=" + flowId + " alias=" + flowAliasId + " inputChars=" + (inputData==null?0:inputData.length()));
        try {
            String url = String.format("https://bedrock-agent-runtime.%s.amazonaws.com/flows/%s/aliases/%s", awsRegion, flowId, flowAliasId);
            log.debug("invokeBedrockFlowHttp: url={}, inputChars={}", url, inputData == null ? 0 : inputData.length());
            Map<String, Object> requestBody = new HashMap<>();
            List<Map<String, Object>> inputs = new ArrayList<>();
            Map<String, Object> inputNode = new HashMap<>();
            inputNode.put("nodeName", "FlowInputNode");
            inputNode.put("nodeOutputName", "document");
            Map<String, Object> content = new HashMap<>();
            content.put("document", inputData);
            inputNode.put("content", content);
            inputs.add(inputNode);
            requestBody.put("inputs", inputs);
            String requestJson = objectMapper.writeValueAsString(requestBody);
            log.debug("Request JSON size={}, preview='{}'", requestJson.length(), requestJson.substring(0, Math.min(300, requestJson.length())));
            HttpHeaders headers = createAwsSignedHeaders(url, requestJson);
            log.trace("Signed headers prepared: {}", headers.keySet());
            long startMs = System.currentTimeMillis();
            ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(requestJson, headers), byte[].class);
            long elapsed = System.currentTimeMillis() - startMs;
            byte[] bodyBytes = response.getBody();
            String bodyStr = bodyBytes == null ? null : new String(bodyBytes, StandardCharsets.UTF_8);
            if (bodyStr != null && bodyStr.contains("ð") && !bodyStr.contains("\uD83D")) {
                String attempt = new String(bodyStr.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                if (attempt.chars().anyMatch(cp -> Character.getType(cp) == Character.SURROGATE)) {
                    log.debug("invokeBedrockFlowHttp: applied mojibake re-decode heuristic");
                    bodyStr = attempt;
                }
            }
            log.info("Bedrock HTTP call completed: status={}, latency={} ms, bodySize={} chars", response.getStatusCode(), elapsed, bodyStr == null ? 0 : bodyStr.length());
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Bedrock Flow raw response fragment='{}'", bodyStr == null ? "<null>" : bodyStr.substring(0, Math.min(250, bodyStr.length())));
                System.out.println("EXIT invokeBedrockFlowHttp success flowId=" + flowId);
                return bodyStr;
            } else {
                throw new RuntimeException("HTTP request failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error making HTTP request to Bedrock Flow: {}", e.getMessage(), e);
            if (e instanceof org.springframework.web.client.HttpClientErrorException httpError) {
                log.error("Bedrock Flow Error detail: Status={}, Body preview='{}'", httpError.getStatusCode(), httpError.getResponseBodyAsString().substring(0, Math.min(500, httpError.getResponseBodyAsString().length())));
            }
            System.out.println("EXIT invokeBedrockFlowHttp error flowId=" + flowId + " msg=" + e.getMessage());
            throw new RuntimeException("Failed to invoke Bedrock Flow via HTTP", e);
        }
    }

    private HttpHeaders createAwsSignedHeaders(String url, String requestBody) {
        System.out.println("ENTER createAwsSignedHeaders url=" + url + " bodySize=" + (requestBody==null?0:requestBody.length()));
        try {
            String service = "bedrock";
            String method = "POST";
            String canonicalUri = extractPath(url);
            String host = extractHost(url);
            Date now = new Date();
            SimpleDateFormat amzDateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            amzDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String amzDate = amzDateFormat.format(now);
            SimpleDateFormat dateStampFormat = new SimpleDateFormat("yyyyMMdd");
            dateStampFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String dateStamp = dateStampFormat.format(now);
            Map<String, String> headers = new TreeMap<>();
            headers.put("content-type", "application/json");
            headers.put("host", host);
            headers.put("x-amz-date", amzDate);
            StringBuilder canonicalHeaders = new StringBuilder();
            StringBuilder signedHeaders = new StringBuilder();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                canonicalHeaders.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
                if (signedHeaders.length() > 0) signedHeaders.append(";");
                signedHeaders.append(entry.getKey());
            }
            String payloadHash = sha256Hex(requestBody != null ? requestBody : "");
            String canonicalRequest = method + "\n" +
                    canonicalUri + "\n" +
                    "" + "\n" +
                    canonicalHeaders + "\n" +
                    signedHeaders + "\n" +
                    payloadHash;
            String credentialScope = dateStamp + "/" + awsRegion + "/" + service + "/aws4_request";
            String stringToSign = "AWS4-HMAC-SHA256\n" +
                    amzDate + "\n" +
                    credentialScope + "\n" +
                    sha256Hex(canonicalRequest);
            byte[] signingKey = getSignatureKey(awsSecretAccessKey, dateStamp, awsRegion, service);
            String signature = bytesToHex(hmacSha256(stringToSign, signingKey));
            String authorization = "AWS4-HMAC-SHA256 " +
                    "Credential=" + awsAccessKeyId + "/" + credentialScope + ", " +
                    "SignedHeaders=" + signedHeaders + ", " +
                    "Signature=" + signature;
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.set("Host", host);
            httpHeaders.set("X-Amz-Date", amzDate);
            httpHeaders.set("Authorization", authorization);
            System.out.println("EXIT createAwsSignedHeaders url=" + url);
            return httpHeaders;
        } catch (Exception e) {
            log.error("Error creating AWS signed headers: {}", e.getMessage(), e);
            System.out.println("EXIT createAwsSignedHeaders error url=" + url + " msg=" + e.getMessage());
            throw new RuntimeException("Failed to sign AWS request", e);
        }
    }

    private String extractHost(String url) {
        System.out.println("ENTER extractHost url=" + url);
        String host = url.split("://")[1].split("/")[0];
        System.out.println("EXIT extractHost host=" + host);
        return host;
    }

    private String extractPath(String url) {
        System.out.println("ENTER extractPath url=" + url);
        String[] parts = url.split("://")[1].split("/", 2);
        String path = parts.length > 1 ? "/" + parts[1] : "/";
        System.out.println("EXIT extractPath path=" + path);
        return path;
    }

    private String sha256Hex(String data) throws Exception {
        System.out.println("ENTER sha256Hex dataLen=" + (data==null?0:data.length()));
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        String hex = bytesToHex(hash);
        System.out.println("EXIT sha256Hex");
        return hex;
    }

    private byte[] hmacSha256(String data, byte[] key) throws Exception {
        System.out.println("ENTER hmacSha256 dataLen=" + (data==null?0:data.length()) + " keyLen=" + (key==null?0:key.length));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        byte[] out = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        System.out.println("EXIT hmacSha256 outLen=" + (out==null?0:out.length));
        return out;
    }

    private byte[] getSignatureKey(String key, String dateStamp, String regionName, String serviceName) throws Exception {
        System.out.println("ENTER getSignatureKey");
        byte[] kDate = hmacSha256(dateStamp, ("AWS4" + key).getBytes(StandardCharsets.UTF_8));
        byte[] kRegion = hmacSha256(regionName, kDate);
        byte[] kService = hmacSha256(serviceName, kRegion);
        byte[] finalKey = hmacSha256("aws4_request", kService);
        System.out.println("EXIT getSignatureKey");
        return finalKey;
    }

    private String bytesToHex(byte[] bytes) {
        System.out.println("ENTER bytesToHex len=" + (bytes==null?0:bytes.length));
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        String hex = result.toString();
        System.out.println("EXIT bytesToHex len=" + hex.length());
        return hex;
    }

    private AnalysisResponse parseUnifiedAgentsResponse(String body, String ticker) {
        if (body == null) return createFallbackResponse(ticker, "empty body");
        
        log.debug("parseUnifiedAgentsResponse: body size={}, first 200 chars='{}'", 
                  body.length(), body.substring(0, Math.min(200, body.length())).replaceAll("\n", " "));
        
        List<AnalysisResponse.AgentResponse> agents = new ArrayList<>();
        
        String cleanBody = body.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F-\\x9F]", "");
        
        List<String> candidateDocs = new ArrayList<>();
        String marker = ":message-typeevent";
        int idx = 0;
        int eventsFound = 0;
        
        while (idx < cleanBody.length()) {
            int eventStart = cleanBody.indexOf(marker, idx);
            if (eventStart == -1) break;
            eventsFound++;
            
            int jsonStart = cleanBody.indexOf('{', eventStart);
            if (jsonStart == -1) break;
            
            try {
                int jsonEnd = findJsonEnd(cleanBody, jsonStart);
                if (jsonEnd == -1) break;
                
                String eventJson = cleanBody.substring(jsonStart, jsonEnd);
                log.debug("Found event JSON: {}", eventJson.substring(0, Math.min(150, eventJson.length())));
                
                JsonNode eventNode = objectMapper.readTree(eventJson);
                String document = eventNode.path("content").path("document").asText(null);
                
                if (document != null && !document.isBlank()) {
                    log.debug("Extracted document: {}", document.substring(0, Math.min(100, document.length())));
                    candidateDocs.add(document);
                }
            } catch (Exception e) {
                log.debug("Failed to parse event JSON: {}", e.getMessage());
            }
            
            idx = eventStart + marker.length();
        }
        
        log.info("Found {} streaming events, extracted {} documents", eventsFound, candidateDocs.size());
        
        if (candidateDocs.isEmpty()) {
            log.debug("No streaming events found, trying whole body as single document");
            candidateDocs.add(cleanBody);
        }
        
        for (String doc : candidateDocs) {
            try {
                log.debug("Attempting to parse document as JSON: {}", doc.substring(0, Math.min(100, doc.length())));
                JsonNode node = objectMapper.readTree(doc.trim());
                String agentName = node.path("agent").asText("");
                if (!agentName.isEmpty()) {
                    log.debug("Found agent: {}", agentName);
                    AnalysisResponse.AgentResponse ar = mapNodeToAgentResponse(node);
                    agents.add(ar);
                } else {
                    log.debug("No agent field found in document");
                }
            } catch (Exception e) {
                log.debug("Failed to parse agent document: {}", e.getMessage());
            }
        }
        
        log.info("Successfully parsed {} agents", agents.size());
        
        if (agents.isEmpty()) {
            log.warn("No agents parsed from response body (size={}), cleanBodySize={}", body.length(), cleanBody.length());
            return createFallbackResponse(ticker, "no agents parsed");
        }
        
        return AnalysisResponse.builder().tickerSymbol(ticker).agents(agents).build();
    }

    private int findJsonEnd(String text, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            
            if (escape) {
                escape = false;
                continue;
            }
            
            if (c == '\\') {
                escape = true;
                continue;
            }
            
            if (c == '"') {
                inString = !inString;
                continue;
            }
            
            if (!inString) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return i + 1;
                    }
                }
            }
        }
        
        return -1;
    }

    private AnalysisResponse.AgentResponse mapNodeToAgentResponse(JsonNode node) {
        String agent = node.path("agent").asText("");
        Double buyScore = node.has("buy_score") && node.get("buy_score").isNumber()? node.get("buy_score").asDouble(): null;
        List<String> redFlags = jsonArrayToList(node.path("red_flags"));
        List<String> greenFlags = jsonArrayToList(node.path("green_flags"));
        String summary = node.path("summary").asText(node.path("analysis").asText(""));
        Set<String> core = Set.of("agent","buy_score","red_flags","green_flags","summary","analysis");
        Map<String,Object> extra = new LinkedHashMap<>();
        node.fieldNames().forEachRemaining(fn -> {
            if (!core.contains(fn)) extra.put(fn, node.get(fn).isValueNode()? node.get(fn).asText(): node.get(fn).toString());
        });
        return AnalysisResponse.AgentResponse.builder()
                .agent(agent)
                .buyScore(buyScore)
                .redFlags(redFlags)
                .greenFlags(greenFlags)
                .summary(summary)
                .extra(extra.isEmpty()?null:extra)
                .build();
    }

    private AnalysisResponse createFallbackResponse(String tickerSymbol, String errorMessage) {
        return AnalysisResponse.builder()
                .tickerSymbol(tickerSymbol)
                .agents(List.of(AnalysisResponse.AgentResponse.builder()
                        .agent("error")
                        .summary("Service temporarily unavailable: " + errorMessage)
                        .redFlags(List.of("error"))
                        .greenFlags(List.of())
                        .extra(Map.of("detail", errorMessage))
                        .build()))
                .build();
    }

    private AnalysisResponse createMockResponse(String tickerSymbol, String userPrompt) {
        String mockStreamingResponse = ":message-typeevent{\"content\":{\"document\":\"{\\n  \\\"agent\\\": \\\"technicals\\\",\\n  \\\"buy_score\\\": 7.2,\\n  \\\"red_flags\\\": [\\n    \\\"RSI showing early signs of overbought territory at 57.81\\\",\\n    \\\"Price rejected at $230+ resistance twice in past week\\\"\\n  ],\\n  \\\"green_flags\\\": [\\n    \\\"Price trading comfortably above all major moving averages (10, 21, 50, 200)\\\",\\n    \\\"Golden cross pattern with 50-DMA crossing above 200-DMA\\\",\\n    \\\"Strong support established at $220.05 with recent bounces\\\",\\n    \\\"Bullish momentum confirmed with rising volume on up days\\\"\\n  ],\\n  \\\"summary\\\": \\\"" + tickerSymbol + " shows strong bullish momentum with price trading above all key moving averages. After breaking through $220 resistance, price has consolidated in the $224-230 range. The recent pullback from $233+ has found support near $224, suggesting healthy profit-taking rather than trend reversal. Fibonacci retracement at 27.22% indicates room for continued upside. Next key resistance at $235 zone. ⚠️ Watch volume on next breakout attempt for confirmation.\\\"\\n}\"},\"nodeName\":\"Technicals_agent_output\"}" +
               ":message-typeevent{\"content\":{\"document\":\"{\\n  \\\"agent\\\": \\\"news\\\",\\n  \\\"buy_score\\\": 5.5,\\n  \\\"sentiment\\\": \\\"mixed\\\",\\n  \\\"summary\\\": \\\"" + tickerSymbol + " faces mixed news with streaming price hike, potential AI challenges, and ongoing market dynamics 🍏📱\\\",\\n  \\\"analysis\\\": [\\n    \\\"Apple TV+ subscription price increased by 30% to $12.99, potentially improving services revenue\\\",\\n    \\\"Competitive pressure from Google and Samsung in smartphone innovation and AI capabilities\\\"\\n  ],\\n  \\\"impact\\\": [\\n    \\\"Potential revenue boost from streaming price adjustment\\\",\\n    \\\"Increased scrutiny on AI development and smartphone features\\\"\\n  ],\\n  \\\"risks\\\": [\\n    \\\"Competitive challenges in smartphone and AI technology\\\",\\n    \\\"Apple Watch import dispute with Masimo escalating\\\",\\n    \\\"Warren Buffett reportedly reducing Apple stake\\\"\\n  ],\\n  \\\"catalysts\\\": [\\n    \\\"Ongoing development of AI capabilities\\\",\\n    \\\"Potential new product launches to counter competitive pressures\\\"\\n  ],\\n  \\\"coverage_note\\\": \\\"Limited news coverage (2 days), analysis based on available headlines\\\"\\n}\"},\"nodeName\":\"News_agent_output\"}" +
               ":message-typeevent{\"content\":{\"document\":\"{\\n  \\\"agent\\\": \\\"fundamentals\\\",\\n  \\\"buy_score\\\": 8.5,\\n  \\\"red_flags\\\": [\\n    \\\"⚠️ Increasing liabilities to equity ratio (5.41x in 2024), indicating higher financial leverage\\\",\\n    \\\"Slight decline in net profit margin from 25.31% to 23.97% in 2024\\\"\\n  ],\\n  \\\"green_flags\\\": [\\n    \\\"🌟 Exceptional Free Cash Flow growth: $158.6B in 2024, up from $125B in 2023\\\",\\n    \\\"🚀 Strong and expanding gross profit margin: 46.21% in 2024, up from 44.13% in 2023\\\",\\n    \\\"💪 Consistently high Return on Invested Capital (ROIC): Stable around 0.52 in recent years\\\",\\n    \\\"🔬 Significant R&D investment: R&D expenses increased to $31.37B in 2024\\\"\\n  ],\\n  \\\"summary\\\": \\\"" + tickerSymbol + " demonstrates robust financial fundamentals with strong cash generation, expanding margins, and consistent value creation. Despite slight margin compression, the company maintains exceptional operational efficiency and continues to invest heavily in innovation. 🍏📈\\\"\\n}\"},\"nodeName\":\"Fundamentals_agent_output\"}" +
               ":message-typeevent{\"content\":{\"document\":\"{\\n  \\\"agent\\\": \\\"trades\\\",\\n  \\\"buy_score\\\": 6.8,\\n  \\\"recommendation\\\": \\\"Buy a small amount\\\",\\n  \\\"reasoning\\\": [\\n    \\\"Fundamentals: " + tickerSymbol + " demonstrates robust financial fundamentals with strong cash generation, expanding margins, and consistent value creation. Despite slight margin compression, the company maintains exceptional operational efficiency and continues to invest heavily in innovation.\\\",\\n    \\\"Technicals: " + tickerSymbol + " shows strong bullish momentum with price trading above all key moving averages. After breaking through $220 resistance, price has consolidated in the $224-230 range. The recent pullback from $233+ has found support near $224, suggesting healthy profit-taking rather than trend reversal.\\\",\\n    \\\"News: " + tickerSymbol + " faces mixed news with streaming price hike, potential AI challenges, and ongoing market dynamics. Potential revenue boost from streaming price adjustment, increased scrutiny on AI development and smartphone features.\\\"\\n  ],\\n  \\\"trade_plan\\\": \\\"Enter if price stays above $220.05 on a daily close, or breaks above $230; stop-loss at $215; profit targets $240 and $250; short-term horizon 2–5 days.\\\",\\n  \\\"notes\\\": [\\n    \\\"Position size: small (~0.5% of account).\\\",\\n    \\\"Monitor competitive pressures in AI and smartphone technology; avoid holding long-term until fundamentals and technicals strengthen further.\\\"\\n  ]\\n}\\n\"},\"nodeName\":\"Trades_agent_output\"}" +
               ":message-typeevent{\"completionReason\":\"SUCCESS\"}";
        
        return parseUnifiedAgentsResponse(mockStreamingResponse, tickerSymbol);
    }

    private List<String> jsonArrayToList(JsonNode node) {
        if (node == null || !node.isArray()) return Collections.emptyList();
        List<String> list = new ArrayList<>();
        for (JsonNode n : node) list.add(n.asText());
        return list;
    }
}