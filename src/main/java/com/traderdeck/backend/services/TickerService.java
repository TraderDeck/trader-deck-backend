package com.traderdeck.backend.services;

import com.traderdeck.backend.models.Ticker;
import com.traderdeck.backend.repositories.TickerRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class TickerService {
    private final TickerRepository tickerRepository;

    public TickerService(TickerRepository tickerRepository) {
        this.tickerRepository = tickerRepository;
    }

    public List<Ticker> getFilteredTickers(String symbol, String name, String industry, String sector, Long marketCapMin) {
        System.out.println("args "+ symbol+name+industry+sector+marketCapMin);
        List<Ticker>  tickers = tickerRepository.findFilteredTickers(symbol, name, industry, sector, marketCapMin);
        System.out.println("tickers are "+ tickers);
        return tickers;
    }
}