package com.traderdeck.backend.controllers;

import com.traderdeck.backend.models.TickerResearchNote;
import com.traderdeck.backend.services.TickerResearchNoteService;
import com.traderdeck.backend.services.JwtService;
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
    private final JwtService jwtService;

    private UUID extractUserIdFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7); // Remove "Bearer " prefix
        }
        return UUID.fromString(jwtService.extractUserId(token));
    }

    @GetMapping
    public ResponseEntity<List<TickerResearchNote>> getNotesForTickers(
            @RequestHeader("Authorization") String token,
            @RequestParam List<String> tickerSymbols) {
        UUID userId = extractUserIdFromToken(token);
        return ResponseEntity.ok(service.getNotesForTickers(tickerSymbols, userId));
    }

    @GetMapping("/{tickerSymbol}")
    public ResponseEntity<TickerResearchNote> getNoteByTicker(
            @RequestHeader("Authorization") String token,
            @PathVariable String tickerSymbol) {
        UUID userId = extractUserIdFromToken(token);
        Optional<TickerResearchNote> note = service.getNoteByTicker(tickerSymbol, userId);
        return note.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TickerResearchNote> createOrUpdateNote(
            @RequestHeader("Authorization") String token,
            @RequestBody TickerResearchNote request) {
        UUID userId = extractUserIdFromToken(token);

        if (request.getTickerSymbol() == null || request.getContent() == null) {
            return ResponseEntity.badRequest().body(null);
        }

        TickerResearchNote savedNote = service.createOrUpdateResearchNote(
                request.getTickerSymbol(),
                request.getContent(),
                userId
        );

        return ResponseEntity.ok(savedNote);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNoteById(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID id) {
        UUID userId = extractUserIdFromToken(token);
        service.deleteNoteById(id, userId);
        return ResponseEntity.noContent().build();
    }
}
