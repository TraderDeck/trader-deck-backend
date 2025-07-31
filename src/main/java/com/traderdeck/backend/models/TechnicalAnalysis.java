package com.traderdeck.backend.models;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@IdClass(TechnicalAnalysisId.class)
@Table(name = "technical_analysis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalAnalysis {

    @Id
    @Column(name = "ticker_symbol")
    private String tickerSymbol;

    @Id
    @Column(name = "analysis_date")
    private LocalDate analysisDate;

    @Column(name = "close_price")
    private BigDecimal closePrice;

    @Column(name = "high_price")
    private BigDecimal highPrice;

    @Column(name = "low_price")
    private BigDecimal lowPrice;

    @Column(name = "ma_10")
    private BigDecimal ma10;

    @Column(name = "ema_21")
    private BigDecimal ema21;

    @Column(name = "ma_50")
    private BigDecimal ma50;

    @Column(name = "ma_200")
    private BigDecimal ma200;

    @Column(name = "rsi_14")
    private BigDecimal rsi14;

    @Column(name = "rsi_support")
    private BigDecimal rsiSupport;

    @Column(name = "support_level")
    private BigDecimal supportLevel;

    @Column(name = "fibonacci_retracement")
    private BigDecimal fibonacciRetracement;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
