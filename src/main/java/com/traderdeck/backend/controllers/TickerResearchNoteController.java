package com.traderdeck.backend.controllers;

import com.traderdeck.backend.models.TickerResearchNote;
import com.traderdeck.backend.services.TickerResearchNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/research-notes")
@RequiredArgsConstructor
public class TickerResearchNoteController {

    private final TickerResearchNoteService service;

    @GetMapping("/{userId}")
    public ResponseEntity<List<TickerResearchNote>> getNotesForTickers(
            @PathVariable UUID userId,
            @RequestParam List<String> tickerSymbols) {
        return ResponseEntity.ok(service.getNotesForTickers(tickerSymbols, userId));
    }

    @GetMapping("/{userId}/{tickerSymbol}")
    public ResponseEntity<TickerResearchNote> getNoteByTicker(
            @PathVariable UUID userId,
            @PathVariable String tickerSymbol) {
        Optional<TickerResearchNote> note = service.getNoteByTicker(tickerSymbol, userId);
        return note.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TickerResearchNote> createOrUpdateNote(@RequestBody TickerResearchNote request) {
        TickerResearchNote savedNote = service.createOrUpdateNote(
                request.getTickerSymbol(),
                request.getUserId(),
                request.getContent()
        );
        return ResponseEntity.ok(savedNote);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNoteById(@PathVariable UUID id) {
        service.deleteNoteById(id);
        return ResponseEntity.noContent().build();
    }
}
