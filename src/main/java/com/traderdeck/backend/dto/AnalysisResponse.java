package com.traderdeck.backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AnalysisResponse {
    private String tickerSymbol;
    private List<AgentResponse> agents; 

    @Data
    @Builder
    public static class AgentResponse {
        private String agent;          
        private Double buyScore;      
        private List<String> redFlags; 
        private List<String> greenFlags;
        private String summary;    
        private Map<String, Object> extra; 
    }
}