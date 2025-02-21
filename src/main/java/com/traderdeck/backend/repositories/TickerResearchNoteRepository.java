package com.traderdeck.backend.repositories;

import com.traderdeck.backend.models.TickerResearchNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TickerResearchNoteRepository extends JpaRepository<TickerResearchNote, UUID> {

    Optional<TickerResearchNote> findByTickerSymbolAndUserId(String tickerSymbol, UUID userId);

    List<TickerResearchNote> findByTickerSymbolInAndUserId(List<String> tickerSymbols, UUID userId);
}
