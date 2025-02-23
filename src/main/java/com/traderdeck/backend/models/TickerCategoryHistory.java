package com.traderdeck.backend.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticker_category_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TickerCategoryHistory {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String tickerSymbol;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    private UUID updatedBy;
}
