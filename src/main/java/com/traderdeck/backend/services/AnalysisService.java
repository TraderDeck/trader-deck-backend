package com.traderdeck.backend.services;

import com.traderdeck.backend.dto.AnalysisRequest;
import com.traderdeck.backend.dto.AnalysisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    public AnalysisResponse analyzeStock(AnalysisRequest request, UUID userId) {
        return AnalysisResponse.builder()
                .tickerSymbol(request.getTickerSymbol())
                .agents(List.of(
                        AnalysisResponse.AgentResponse.builder()
                                .agent("technicals")
                                .buyScore(7.2)
                                .greenFlags(List.of("RSI 65 bullish zone","Price above 50/200 MAs"))
                                .redFlags(List.of("RSI not yet above 70"))
                                .summary("Technical analysis shows strong momentum with RSI at 65 and moving averages trending upward.")
                                .extra(Map.of(
                                        "indicators","RSI:65, MACD: Bullish, MAs: Uptrend",
                                        "signals","MACD crossover + volume spike"
                                ))
                                .build(),
                        AnalysisResponse.AgentResponse.builder()
                                .agent("fundamentals")
                                .buyScore(7.8)
                                .greenFlags(List.of("Revenue growth 12%","EPS growth 15%"))
                                .redFlags(List.of("Debt/Equity rising slightly"))
                                .summary("Company shows strong fundamentals with growing revenue and improving profit margins.")
                                .extra(Map.of(
                                        "valuation","Fair relative to peers (P/E 22)",
                                        "metrics","P/E:22 | EPSg:15% | RevG:12% | D/E:0.3"
                                ))
                                .build(),
                        AnalysisResponse.AgentResponse.builder()
                                .agent("news")
                                .buyScore(6.5)
                                .greenFlags(List.of("Earnings beat","Guidance raised"))
                                .redFlags(List.of("Sector volatility"))
                                .summary("Recent earnings beat expectations with positive guidance for next quarter.")
                                .extra(Map.of(
                                        "sentiment","Bullish",
                                        "impact","Positive news flow likely to support price near term"
                                ))
                                .build(),
                        AnalysisResponse.AgentResponse.builder()
                                .agent("trades")
                                .buyScore(7.0)
                                .greenFlags(List.of("Momentum alignment","Fundamental strength"))
                                .redFlags(List.of("Resistance overhead"))
                                .summary("Recommendation: Buy. Entry favorable with clear support below.")
                                .extra(Map.of(
                                        "recommendation","Buy",
                                        "reasoning","Momentum + fundamentals + news sentiment align",
                                        "confidence","0.85",
                                        "trade_plan","Entry near 50DMA, SL below recent swing, targets R1/R2"
                                ))
                                .build()
                ))
                .build();
    }

    public AnalysisResponse errorResponse(String ticker, String message) {
        return AnalysisResponse.builder()
                .tickerSymbol(ticker)
                .agents(List.of(
                        AnalysisResponse.AgentResponse.builder()
                                .agent("error")
                                .buyScore(null)
                                .redFlags(List.of("error"))
                                .greenFlags(List.of())
                                .summary(message)
                                .extra(Map.of("detail", message))
                                .build()
                ))
                .build();
    }
}