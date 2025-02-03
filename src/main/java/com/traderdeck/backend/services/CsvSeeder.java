package com.traderdeck.backend.services;

import com.traderdeck.backend.models.Ticker;
import com.traderdeck.backend.repositories.TickerRepository;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import java.io.FileReader;
import java.io.Reader;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CsvSeeder {
    private final TickerRepository tickerRepository;

    public CsvSeeder(TickerRepository tickerRepository) {
        this.tickerRepository = tickerRepository;
    }

    @PostConstruct
    public void seedDatabase() {
        if (tickerRepository.count() > 0) {
            System.out.println("Database already seeded. Skipping.");
            return;
        }

        try (Reader reader = new FileReader("src/main/resources/tickers.csv")) {
            CsvToBean<Ticker> csvToBean = new CsvToBeanBuilder<Ticker>(reader)
                    .withType(Ticker.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build();

            List<Ticker> tickers = csvToBean.parse();

            List<Ticker> validTickers = tickers.stream()
                    .filter(t -> t.getSymbol() != null && !t.getSymbol().isEmpty() &&
                            t.getName() != null && !t.getName().isEmpty() &&
                            t.getCountry() != null && !t.getCountry().isEmpty() &&
                            t.getIndustry() != null && !t.getIndustry().isEmpty() &&
                            t.getSector() != null && !t.getSector().isEmpty() &&
                            t.getMarketCap() != null && !t.getMarketCap().equals(BigInteger.ZERO))
                    .toList();

////            System.out.println("---------- Printing tickers parsed: "+tickers);
//            System.out.println("-------- Parsing Done .....: ");
            tickerRepository.saveAll(validTickers);
            System.out.println("Ticker data successfully imported.");
        } catch (Exception e) {
//            System.out.println("-------- Parsing Error .....: "+e);
            e.printStackTrace();
        }
    }
}