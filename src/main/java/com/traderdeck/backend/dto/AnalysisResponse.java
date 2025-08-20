package com.traderdeck.backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalysisResponse {
    private String tickerSymbol;
    private TradeAgentResponse tradeAgent;
    private TechnicalAgentResponse technicalAgent;
    private FundamentalsAgentResponse fundamentalsAgent;
    private NewsAgentResponse newsAgent;

    @Data
    @Builder
    public static class TradeAgentResponse {
        private String recommendation;
        private String reasoning;
        private String confidence;
    }

    @Data
    @Builder
    public static class TechnicalAgentResponse {
        private String analysis;
        private String signals;
        private String indicators;
    }

    @Data
    @Builder
    public static class FundamentalsAgentResponse {
        private String analysis;
        private String valuation;
        private String metrics;
    }

    @Data
    @Builder
    public static class NewsAgentResponse {
        private String sentiment;
        private String summary;
        private String impact;
    }
}