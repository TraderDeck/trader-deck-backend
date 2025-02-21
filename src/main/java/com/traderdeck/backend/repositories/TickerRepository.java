package com.traderdeck.backend.repositories;

import com.traderdeck.backend.models.Ticker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TickerRepository extends JpaRepository<Ticker, String> {

    // Find ticker by symbol
    List<Ticker> findBySymbol(String symbol);

    // Find ticker by name
    List<Ticker> findByNameContainingIgnoreCase(String name);

    // Find tickers by industry
    List<Ticker> findByIndustry(String industry);

    List<Ticker> findBySector(String sector);

    // Find tickers with a certain market cap or higher
    List<Ticker> findByMarketCapGreaterThanEqual(Long marketCap);

    // Custom Query: Find tickers based on multiple filters
    @Query("SELECT t FROM Ticker t WHERE " +
            "(:symbol IS NULL OR t.symbol = :symbol) AND " +
            "(:name IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%',  CAST(:name AS text), '%'))) AND " +
            "(:industry IS NULL OR t.industry = :industry) AND " +
            "(:sector IS NULL OR t.sector = :sector) AND " +
            "(:marketCapMin IS NULL OR t.marketCap >= :marketCapMin)")
    List<Ticker> findFilteredTickers(@Param("symbol") String symbol,
                                     @Param("name") String name,
                                     @Param("industry") String industry,
                                     @Param("sector") String sector,
                                     @Param("marketCapMin") Long marketCap);
}