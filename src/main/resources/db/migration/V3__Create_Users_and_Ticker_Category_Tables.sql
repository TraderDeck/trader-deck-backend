-- Create users table
CREATE TABLE users (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL
);

-- Create category history table
CREATE TABLE ticker_category_history (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    ticker_symbol VARCHAR(10) NOT NULL,
    category VARCHAR(50) NOT NULL,
    changed_at TIMESTAMP DEFAULT NOW(),
    updated_by UUID NOT NULL,
    FOREIGN KEY (updated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Create current category table
CREATE TABLE ticker_categories (
    ticker_symbol VARCHAR(10) PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP DEFAULT NOW()
);
