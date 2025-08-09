ALTER TABLE technical_analysis
  ADD COLUMN open_price NUMERIC(12, 4),
  ADD COLUMN volume BIGINT;

-- Optional: index if you'll often filter or sort by volume
CREATE INDEX IF NOT EXISTS ta_volume_idx
  ON technical_analysis (volume);
