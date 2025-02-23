package com.traderdeck.backend.repositories;

import com.traderdeck.backend.models.TickerResearchNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TickerResearchNoteRepository extends JpaRepository<TickerResearchNote, UUID> {
    List<TickerResearchNote> findByUserId(UUID userId);
    List<TickerResearchNote> findByUserIdAndTickerSymbol(UUID userId, String tickerSymbol);
    List<TickerResearchNote> findByUserIdAndTickerSymbolIn(UUID userId, List<String> tickerSymbols);
}
