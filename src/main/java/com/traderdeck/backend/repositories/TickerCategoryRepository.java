package com.traderdeck.backend.repositories;

import com.traderdeck.backend.models.TickerCategory;
import com.traderdeck.backend.models.TickerCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TickerCategoryRepository extends JpaRepository<TickerCategory, TickerCategoryId> {
    Optional<TickerCategory> findByTickerSymbolAndUpdatedBy(String tickerSymbol, UUID updatedBy);
    List<TickerCategory> findByTickerSymbolInAndUpdatedBy(List<String> tickerSymbols, UUID updatedBy);
}
