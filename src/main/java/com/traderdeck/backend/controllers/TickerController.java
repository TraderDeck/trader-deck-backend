package com.traderdeck.backend.controllers;

import com.traderdeck.backend.models.Ticker;
import com.traderdeck.backend.services.TickerService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tickers")
public class TickerController {
    private final TickerService tickerService;

    public TickerController(TickerService tickerService) {
        this.tickerService = tickerService;
    }

    // ✅ GET Request: Retrieve tickers based on filters
    @GetMapping("/search")
    public List<Ticker> searchTickers(
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String industry,
            @RequestParam(required = false) String sector,
            @RequestParam(required = false) Long marketCapMin) {

        System.out.println("----------------------- marketCap is : " + marketCapMin);
        
        return tickerService.getFilteredTickers(symbol, name, industry, sector, marketCapMin);
    }
}