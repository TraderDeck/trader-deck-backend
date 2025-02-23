package com.traderdeck.backend.controllers;

import com.traderdeck.backend.models.TickerCategory;
import com.traderdeck.backend.models.TickerCategoryHistory;
import com.traderdeck.backend.services.TickerCategoryService;
import com.traderdeck.backend.services.JwtService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/ticker-category")
@RequiredArgsConstructor
public class TickerCategoryController {

    private final TickerCategoryService categoryService;
    private final JwtService jwtService;


    private UUID extractUserIdFromToken(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7); // Remove "Bearer " prefix
        }
        return UUID.fromString(jwtService.extractUserId(token));
    }


    @PostMapping("/update")
    public ResponseEntity<String> updateCategory(
            @RequestHeader("Authorization") String token,
            @RequestParam String tickerSymbol,
            @RequestParam String category) {

        UUID updatedBy = extractUserIdFromToken(token);
        categoryService.updateCategory(tickerSymbol, category, updatedBy);
        return ResponseEntity.ok("Category updated successfully.");
    }

    @GetMapping("/current/{tickerSymbol}")
    public ResponseEntity<TickerCategory> getCurrentCategory(
            @RequestHeader("Authorization") String token,
            @PathVariable String tickerSymbol) {
        UUID updatedBy = extractUserIdFromToken(token);
        Optional<TickerCategory> category = categoryService.getCurrentCategory(tickerSymbol,updatedBy);
        return category.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/current-all")
    public ResponseEntity<Map<String, TickerCategory>> getCurrentCategories(
            @RequestHeader("Authorization") String token,
            @RequestParam String tickerSymbols) {
        System.out.println("------This api is called...." + tickerSymbols);

        UUID updatedBy = extractUserIdFromToken(token);
        List<String> symbolsList = Arrays.asList(tickerSymbols.split(","));
        Map<String, TickerCategory> categories = categoryService.getCurrentCategories(symbolsList, updatedBy);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/history/{tickerSymbol}")
    public ResponseEntity<List<TickerCategoryHistory>> getCategoryHistory(
            @RequestHeader("Authorization") String token,
            @PathVariable String tickerSymbol) {
        UUID updatedBy = extractUserIdFromToken(token);
        return ResponseEntity.ok(categoryService.getCategoryHistory(tickerSymbol,updatedBy));
    }
}
