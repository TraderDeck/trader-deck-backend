package com.traderdeck.backend.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticker_research_notes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"ticker_symbol", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TickerResearchNote {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String tickerSymbol;

    @Column(name = "user_id", nullable = false)
    private UUID userId; // ✅ Ensure this field exists

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();
}
