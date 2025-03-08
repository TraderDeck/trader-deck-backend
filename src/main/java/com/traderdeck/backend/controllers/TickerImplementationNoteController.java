package com.traderdeck.backend.controllers;

import com.traderdeck.backend.models.TickerImplementationNote;
import com.traderdeck.backend.services.TickerImplementationNoteService;
import com.traderdeck.backend.services.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/implementation-notes")
@RequiredArgsConstructor
public class TickerImplementationNoteController {

    private final TickerImplementationNoteService service;
    private final JwtService jwtService;

    private UUID extractUserIdFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7); // Remove "Bearer " prefix
        }
        return UUID.fromString(jwtService.extractUserId(token));
    }

    @GetMapping
    public ResponseEntity<List<TickerImplementationNote>> getNotesForTickers(
            @RequestHeader("Authorization") String token,
            @RequestParam List<String> tickerSymbols) {
        UUID userId = extractUserIdFromToken(token);
        return ResponseEntity.ok(service.getNotesForTickers(tickerSymbols, userId));
    }

    @GetMapping("/{tickerSymbol}")
    public ResponseEntity<TickerImplementationNote> getNoteByTicker(
            @RequestHeader("Authorization") String token,
            @PathVariable String tickerSymbol) {
        UUID userId = extractUserIdFromToken(token);
        Optional<TickerImplementationNote> note = service.getNoteByTicker(tickerSymbol, userId);
        return note.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TickerImplementationNote> createOrUpdateNote(
            @RequestHeader("Authorization") String token,
            @RequestBody TickerImplementationNote request) {
        UUID userId = extractUserIdFromToken(token);

        if (request.getTickerSymbol() == null || request.getContent() == null) {
            return ResponseEntity.badRequest().body(null);
        }

        TickerImplementationNote savedNote = service.createOrUpdateImplementationNote(
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
