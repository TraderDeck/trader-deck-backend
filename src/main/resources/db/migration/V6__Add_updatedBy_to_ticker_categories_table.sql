ALTER TABLE ticker_categories
ADD COLUMN updated_by UUID NOT NULL;

ALTER TABLE ticker_categories
ADD CONSTRAINT fk_ticker_categories_updated_by
FOREIGN KEY (updated_by) REFERENCES users(id)
ON DELETE SET NULL;


ALTER TABLE ticker_categories
DROP CONSTRAINT ticker_categories_pkey;

ALTER TABLE ticker_categories
ADD PRIMARY KEY (ticker_symbol, updated_by);