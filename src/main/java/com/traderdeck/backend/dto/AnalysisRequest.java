package com.traderdeck.backend.dto;

import lombok.Data;

@Data
public class AnalysisRequest {
    private String tickerSymbol;
    private String userPrompt;
}