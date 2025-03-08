package com.traderdeck.backend.services;

import com.traderdeck.backend.models.Ticker;
import com.traderdeck.backend.repositories.TickerRepository;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.List;

@Service
public class TickersSeeder {
    private final TickerRepository tickerRepository;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.tickers-file}")
    private String tickersFileKey;

    public TickersSeeder(TickerRepository tickerRepository) {
        this.tickerRepository = tickerRepository;
        this.s3Client = S3Client.builder()
                .credentialsProvider(DefaultCredentialsProvider.create()) // Uses AWS credentials from environment
                .build();
    }

    @PostConstruct
    public void seedDatabase() {
        if (tickerRepository.count() > 0) {
            System.out.println("⚠️ Ticker Database already seeded. Skipping.");
            return;
        }

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(tickersFileKey)
                    .build();

            ResponseInputStream<?> s3Object = s3Client.getObject(getObjectRequest);
            InputStreamReader reader = new InputStreamReader(s3Object);

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

            tickerRepository.saveAll(validTickers);
            System.out.println("Ticker data successfully imported from S3.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to load tickers from S3.");
        }
    }
}
