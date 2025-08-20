package com.traderdeck.backend.controllers;

import com.traderdeck.backend.dto.AnalysisRequest;
import com.traderdeck.backend.dto.AnalysisResponse;
import com.traderdeck.backend.services.BedrockAnalysisService;
import com.traderdeck.backend.services.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@Slf4j
public class AnalyzeController {

    private final BedrockAnalysisService bedrockAnalysisService;
    private final JwtService jwtService;

    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResponse> analyzeStock(@RequestBody AnalysisRequest request) {
        try {
            log.info("Received analysis request for ticker: {}", request.getTickerSymbol());
            
            // For now, we'll use a dummy user ID since authentication might not be required
            // You can add authentication later if needed
            UUID userId = UUID.randomUUID();
            
            AnalysisResponse response = bedrockAnalysisService.analyzeStock(
                request.getTickerSymbol(), 
                request.getUserPrompt(), 
                userId
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error analyzing stock {}: {}", request.getTickerSymbol(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(createErrorResponse(request.getTickerSymbol(), e.getMessage()));
        }
    }

    private AnalysisResponse createErrorResponse(String tickerSymbol, String errorMessage) {
        return AnalysisResponse.builder()
                .tickerSymbol(tickerSymbol)
                .tradeAgent(AnalysisResponse.TradeAgentResponse.builder()
                        .recommendation("ERROR")
                        .reasoning("Analysis failed: " + errorMessage)
                        .confidence("Low")
                        .build())
                .technicalAgent(AnalysisResponse.TechnicalAgentResponse.builder()
                        .analysis("Analysis failed")
                        .signals("N/A")
                        .indicators("N/A")
                        .build())
                .fundamentalsAgent(AnalysisResponse.FundamentalsAgentResponse.builder()
                        .analysis("Analysis failed")
                        .valuation("N/A")
                        .metrics("N/A")
                        .build())
                .newsAgent(AnalysisResponse.NewsAgentResponse.builder()
                        .sentiment("Neutral")
                        .summary("Analysis failed")
                        .impact("Unknown")
                        .build())
                .build();
    }
}