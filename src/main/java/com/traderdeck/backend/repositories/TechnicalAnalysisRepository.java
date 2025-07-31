package com.traderdeck.backend.repositories;

import com.traderdeck.backend.models.TechnicalAnalysis;
import com.traderdeck.backend.models.TechnicalAnalysisId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TechnicalAnalysisRepository extends JpaRepository<TechnicalAnalysis, TechnicalAnalysisId> {
    Optional<TechnicalAnalysis> findByTickerSymbolAndAnalysisDate(String tickerSymbol, LocalDate analysisDate);
}
