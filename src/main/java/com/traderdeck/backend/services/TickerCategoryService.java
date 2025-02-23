package com.traderdeck.backend.services;

import com.traderdeck.backend.models.TickerCategory;
import com.traderdeck.backend.models.TickerCategoryHistory;
import com.traderdeck.backend.repositories.TickerCategoryHistoryRepository;
import com.traderdeck.backend.repositories.TickerCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TickerCategoryService {

    private final TickerCategoryHistoryRepository historyRepository;
    private final TickerCategoryRepository categoryRepository;

    public TickerCategoryService(TickerCategoryHistoryRepository historyRepository, TickerCategoryRepository categoryRepository) {
        this.historyRepository = historyRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public void updateCategory(String tickerSymbol, String newCategory, UUID updatedBy) {
        // Log history
        TickerCategoryHistory history = TickerCategoryHistory.builder()
                .tickerSymbol(tickerSymbol)
                .category(newCategory)
                .changedAt(LocalDateTime.now())
                .updatedBy(updatedBy)
                .build();
        historyRepository.save(history);

        // Check if the category entry exists for this ticker and user
        Optional<TickerCategory> existingCategory = categoryRepository.findByTickerSymbolAndUpdatedBy(tickerSymbol, updatedBy);

        if (existingCategory.isPresent()) {
            TickerCategory category = existingCategory.get();
            category.setCategory(newCategory);
            category.setUpdatedAt(LocalDateTime.now());
            categoryRepository.save(category);
        } else {
            TickerCategory newCategoryEntry = new TickerCategory(tickerSymbol, updatedBy, newCategory, LocalDateTime.now());
            categoryRepository.save(newCategoryEntry);
        }
    }


    public Optional<TickerCategory> getCurrentCategory(String tickerSymbol, UUID updatedBy) {
        return categoryRepository.findByTickerSymbolAndUpdatedBy(tickerSymbol,updatedBy);
    }

    public Map<String, TickerCategory> getCurrentCategories(List<String> tickerSymbols, UUID updatedBy) {
        System.out.println("------This api is called....");
        List<TickerCategory> list = categoryRepository.findByTickerSymbolInAndUpdatedBy(tickerSymbols,updatedBy);
        return list.stream().collect(Collectors.toMap(TickerCategory::getTickerSymbol, Function.identity()));
    }


    public List<TickerCategoryHistory> getCategoryHistory(String tickerSymbol, UUID updatedBy) {
        return historyRepository.findByTickerSymbolAndUpdatedByOrderByChangedAtDesc(tickerSymbol, updatedBy);
    }
}
