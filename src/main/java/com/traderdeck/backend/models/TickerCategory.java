package com.traderdeck.backend.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticker_categories")
@IdClass(TickerCategoryId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TickerCategory {

    @Id
    @Column(name = "ticker_symbol", length = 10, nullable = false)
    private String tickerSymbol;

    @Id
    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Column(name = "category", length = 50, nullable = false)
    private String category;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
