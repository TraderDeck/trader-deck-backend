CREATE TABLE technical_analysis (
    ticker_symbol VARCHAR(10) NOT NULL,
    analysis_date DATE NOT NULL,

    -- Price Data
    close_price NUMERIC(12, 4),
    high_price NUMERIC(12, 4),
    low_price NUMERIC(12, 4),

    -- Moving Averages
    ma_10 NUMERIC(12, 4),
    ema_21 NUMERIC(12, 4),
    ma_50 NUMERIC(12, 4),
    ma_200 NUMERIC(12, 4),

    -- RSI
    rsi_14 NUMERIC(6, 2),
    rsi_support NUMERIC(6, 2),

    -- Support Level
    support_level NUMERIC(12, 4),

    -- Fibonacci Retracement
    fibonacci_retracement NUMERIC(12, 4),

    -- Timestamp for insertion
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Keys
    CONSTRAINT pk_technical_analysis PRIMARY KEY (ticker_symbol, analysis_date),
    CONSTRAINT fk_technical_analysis_ticker
        FOREIGN KEY (ticker_symbol) REFERENCES ticker(symbol) ON DELETE CASCADE
);

CREATE INDEX idx_tech_analysis_ticker_date ON technical_analysis (ticker_symbol, analysis_date);