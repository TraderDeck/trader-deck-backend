package com.traderdeck.backend.models;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TechnicalAnalysisId implements Serializable {
    private String tickerSymbol;
    private LocalDate analysisDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TechnicalAnalysisId)) return false;
        TechnicalAnalysisId that = (TechnicalAnalysisId) o;
        return tickerSymbol.equals(that.tickerSymbol) &&
                analysisDate.equals(that.analysisDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tickerSymbol, analysisDate);
    }
}
