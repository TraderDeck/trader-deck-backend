-- Drop the existing numeric support_level column
ALTER TABLE technical_analysis
DROP COLUMN support_level;

-- Add a new support_level column as TEXT to store the full support structure as a string
ALTER TABLE technical_analysis
ADD COLUMN support_level TEXT;

-- Drop the existing numeric rsi_support column
ALTER TABLE technical_analysis
DROP COLUMN rsi_support;

-- Add a new rsi_support column as TEXT to store the full support structure as a string
ALTER TABLE technical_analysis
ADD COLUMN rsi_support TEXT;