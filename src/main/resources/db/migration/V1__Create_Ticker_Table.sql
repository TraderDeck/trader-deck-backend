CREATE TABLE ticker (
    symbol VARCHAR(10) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    country VARCHAR(100) NOT NULL,
    industry VARCHAR(255) NOT NULL,
    sector VARCHAR(255) NOT NULL,
    market_cap BIGINT NOT NULL
);