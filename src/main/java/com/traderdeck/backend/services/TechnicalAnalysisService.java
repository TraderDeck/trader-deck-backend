package com.traderdeck.backend.services;

import com.traderdeck.backend.models.TechnicalAnalysis;
import com.traderdeck.backend.repositories.TechnicalAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TechnicalAnalysisService {

    private final TechnicalAnalysisRepository repository;

    public Optional<TechnicalAnalysis> getByTickerAndDate(String ticker, LocalDate date) {
        return repository.findByTickerSymbolAndAnalysisDate(ticker, date);
    }

    public TechnicalAnalysis save(TechnicalAnalysis analysis) {
        return repository.save(analysis);
    }
}
