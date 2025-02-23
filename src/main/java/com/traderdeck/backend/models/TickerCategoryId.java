package com.traderdeck.backend.models;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TickerCategoryId implements Serializable {
    private String tickerSymbol;
    private UUID updatedBy;
}