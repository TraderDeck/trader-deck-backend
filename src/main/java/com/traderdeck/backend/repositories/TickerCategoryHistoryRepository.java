package com.traderdeck.backend.repositories;

import com.traderdeck.backend.models.TickerCategoryHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TickerCategoryHistoryRepository extends JpaRepository<TickerCategoryHistory, UUID> {
    List<TickerCategoryHistory> findByTickerSymbolAndUpdatedByOrderByChangedAtDesc (String tickerSymbol, UUID updatedBy);
}
