-- V13__add_school_settings_fields.sql
-- Add school settings, logo/signature assets, affiliation, and principal details for auto template population

ALTER TABLE schools
    ADD COLUMN IF NOT EXISTS signature_file_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS website VARCHAR(200),
    ADD COLUMN IF NOT EXISTS affiliation_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS tagline VARCHAR(300),
    ADD COLUMN IF NOT EXISTS principal_name VARCHAR(150);
