package com.traderdeck.backend.controllers;

import com.traderdeck.backend.models.TechnicalAnalysis;
import com.traderdeck.backend.services.TechnicalAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/technical-analysis")
@RequiredArgsConstructor
public class TechnicalAnalysisController {

    private final TechnicalAnalysisService service;

    @GetMapping("/{ticker}")
    public ResponseEntity<?> getByTickerAndDate(
            @PathVariable String ticker,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.getByTickerAndDate(ticker, date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TechnicalAnalysis analysis) {
        return ResponseEntity.ok(service.save(analysis));
    }
}
