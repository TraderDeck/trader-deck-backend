package com.traderdeck.backend.repositories;

import com.traderdeck.backend.models.TickerImplementationNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TickerImplementationNoteRepository extends JpaRepository<TickerImplementationNote, UUID> {
    List<TickerImplementationNote> findByUserId(UUID userId);
    List<TickerImplementationNote> findByUserIdAndTickerSymbol(UUID userId, String tickerSymbol);
    List<TickerImplementationNote> findByUserIdAndTickerSymbolIn(UUID userId, List<String> tickerSymbols);
}
