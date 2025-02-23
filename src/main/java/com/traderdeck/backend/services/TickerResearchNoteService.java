package com.traderdeck.backend.services;

import com.traderdeck.backend.models.TickerResearchNote;
import com.traderdeck.backend.models.User;
import com.traderdeck.backend.repositories.TickerResearchNoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TickerResearchNoteService {

    private final TickerResearchNoteRepository researchNoteRepository;

    public TickerResearchNoteService(TickerResearchNoteRepository researchNoteRepository) {
        this.researchNoteRepository = researchNoteRepository;
    }

    @Transactional
    public TickerResearchNote createOrUpdateResearchNote(String tickerSymbol, String content, UUID userId) {
        Optional<TickerResearchNote> existingNote = researchNoteRepository.findByUserIdAndTickerSymbol(userId, tickerSymbol)
                .stream().findFirst();

        if (existingNote.isPresent()) {
            TickerResearchNote note = existingNote.get();
            note.setContent(content);
            note.setLastUpdated(LocalDateTime.now());
            return researchNoteRepository.save(note);
        } else {
            TickerResearchNote newNote = TickerResearchNote.builder()
                    .tickerSymbol(tickerSymbol)
                    .content(content)
                    .userId(userId)
                    .lastUpdated(LocalDateTime.now())
                    .build();
            return researchNoteRepository.save(newNote);
        }
    }

    public List<TickerResearchNote> getNotesForTickers(List<String> tickerSymbols, UUID userId) {
        return researchNoteRepository.findByUserIdAndTickerSymbolIn(userId, tickerSymbols);
    }

    public Optional<TickerResearchNote> getNoteByTicker(String tickerSymbol, UUID userId) {
        return researchNoteRepository.findByUserIdAndTickerSymbol(userId, tickerSymbol)
                .stream().findFirst();
    }

    @Transactional
    public void deleteNoteById(UUID noteId, UUID userId) {
        Optional<TickerResearchNote> note = researchNoteRepository.findById(noteId);
        if (note.isPresent() && note.get().getUserId().equals(userId)) {
            researchNoteRepository.deleteById(noteId);
        } else {
            throw new IllegalArgumentException("Unauthorized to delete this note");
        }
    }
}
