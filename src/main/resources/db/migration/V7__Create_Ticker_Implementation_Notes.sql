CREATE TABLE ticker_implementation_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticker_symbol VARCHAR(10) NOT NULL,
    user_id UUID NOT NULL,
    content TEXT NOT NULL,
    last_updated TIMESTAMP DEFAULT NOW(),

    CONSTRAINT fk_ticker FOREIGN KEY (ticker_symbol) REFERENCES ticker(symbol) ON DELETE CASCADE,

    UNIQUE (ticker_symbol, user_id)
);

CREATE INDEX idx_imp_ticker_symbol ON ticker_implementation_notes (ticker_symbol);
CREATE INDEX idx_imp_user_id ON ticker_implementation_notes (user_id);
