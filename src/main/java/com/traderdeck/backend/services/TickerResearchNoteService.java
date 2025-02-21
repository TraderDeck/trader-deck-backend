package com.traderdeck.backend.services;

import com.traderdeck.backend.models.TickerResearchNote;
import com.traderdeck.backend.repositories.TickerResearchNoteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TickerResearchNoteService {

    private final TickerResearchNoteRepository repository;


    public List<TickerResearchNote> getNotesForTickers(List<String> tickerSymbols, UUID userId) {
        return repository.findByTickerSymbolInAndUserId(tickerSymbols, userId);
    }

    public Optional<TickerResearchNote> getNoteByTicker(String tickerSymbol, UUID userId) {
        return repository.findByTickerSymbolAndUserId(tickerSymbol, userId);
    }

    @Transactional
    public TickerResearchNote createOrUpdateNote(String tickerSymbol, UUID userId, String content) {
        return repository.findByTickerSymbolAndUserId(tickerSymbol, userId)
                .map(existingNote -> {
                    existingNote.setContent(content);
                    existingNote.setLastUpdated(LocalDateTime.now());
                    return repository.save(existingNote);  // ✅ Updates existing note
                })
                .orElseGet(() -> {
                    TickerResearchNote newNote = TickerResearchNote.builder()
                            .tickerSymbol(tickerSymbol)
                            .userId(userId)
                            .content(content)
                            .lastUpdated(LocalDateTime.now())
                            .build();
                    return repository.save(newNote);  // ✅ Creates new row if it doesn't exist
                });
    }
    public void deleteNoteById(UUID id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Research note not found.");
        }
        repository.deleteById(id);
    }
}
