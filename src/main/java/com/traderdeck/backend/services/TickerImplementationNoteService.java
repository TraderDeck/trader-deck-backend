package com.traderdeck.backend.services;

import com.traderdeck.backend.models.TickerImplementationNote;
import com.traderdeck.backend.models.TickerImplementationNote;
import com.traderdeck.backend.models.User;
import com.traderdeck.backend.repositories.TickerImplementationNoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TickerImplementationNoteService {

    private final TickerImplementationNoteRepository ImplementationNoteRepository;

    public TickerImplementationNoteService(TickerImplementationNoteRepository ImplementationNoteRepository) {
        this.ImplementationNoteRepository = ImplementationNoteRepository;
    }

    @Transactional
    public TickerImplementationNote createOrUpdateImplementationNote(String tickerSymbol, String content, UUID userId) {
        Optional<TickerImplementationNote> existingNote = ImplementationNoteRepository.findByUserIdAndTickerSymbol(userId, tickerSymbol)
                .stream().findFirst();

        if (existingNote.isPresent()) {
            TickerImplementationNote note = existingNote.get();
            note.setContent(content);
            note.setLastUpdated(LocalDateTime.now());
            return ImplementationNoteRepository.save(note);
        } else {
            TickerImplementationNote newNote = TickerImplementationNote.builder()
                    .tickerSymbol(tickerSymbol)
                    .content(content)
                    .userId(userId)
                    .lastUpdated(LocalDateTime.now())
                    .build();
            return ImplementationNoteRepository.save(newNote);
        }
    }

    public List<TickerImplementationNote> getNotesForTickers(List<String> tickerSymbols, UUID userId) {
        return ImplementationNoteRepository.findByUserIdAndTickerSymbolIn(userId, tickerSymbols);
    }

    public Optional<TickerImplementationNote> getNoteByTicker(String tickerSymbol, UUID userId) {
        return ImplementationNoteRepository.findByUserIdAndTickerSymbol(userId, tickerSymbol)
                .stream().findFirst();
    }

    @Transactional
    public void deleteNoteById(UUID noteId, UUID userId) {
        Optional<TickerImplementationNote> note = ImplementationNoteRepository.findById(noteId);
        if (note.isPresent() && note.get().getUserId().equals(userId)) {
            ImplementationNoteRepository.deleteById(noteId);
        } else {
            throw new IllegalArgumentException("Unauthorized to delete this note");
        }
    }
}
