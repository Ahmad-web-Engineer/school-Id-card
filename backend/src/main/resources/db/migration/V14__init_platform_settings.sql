-- =========================================================================
-- V14__init_platform_settings.sql
-- Platform settings for ID card printing charges, tax, agency costs, and operations
-- =========================================================================

CREATE TABLE IF NOT EXISTS platform_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_platform_settings_category ON platform_settings (category);

-- Seed default ID card printing charges and parameters
INSERT INTO platform_settings (setting_key, setting_value, category, description)
VALUES 
    ('PRICING_PVC_CARD', '45.00', 'PRICING', 'Base selling price per PVC ID card in INR'),
    ('PRICING_RFID_CARD', '95.00', 'PRICING', 'Base selling price per RFID smart card in INR'),
    ('PRICING_LANYARD', '18.00', 'PRICING', 'Selling price per custom lanyard ribbon in INR'),
    ('PRICING_CARD_HOLDER', '12.00', 'PRICING', 'Selling price per protective card pouch / holder in INR'),
    ('PRICING_RUSH_FEE', '20.00', 'PRICING', 'Priority / rush production surcharge per card in INR'),
    ('AGENCY_COST_PER_CARD', '25.00', 'AGENCY', 'Standard manufacturing cost paid to printing agency partner per card in INR'),
    ('TAX_RATE_PERCENT', '18.00', 'COMMERCIAL', 'Default GST tax rate percentage applied to school orders'),
    ('FLAT_SHIPPING_FEE', '150.00', 'COMMERCIAL', 'Flat courier & delivery fee per shipment in INR'),
    ('MIN_ORDER_QUANTITY', '25', 'COMMERCIAL', 'Minimum order quantity threshold for custom ID card production'),
    ('PAYMENT_DUE_DAYS', '30', 'COMMERCIAL', 'Default invoice payment terms in days from invoice generation'),
    ('QUOTATION_VALIDITY_DAYS', '15', 'COMMERCIAL', 'Period for which an issued quotation remains valid in days'),
    ('DEFAULT_DELIVERY_MODEL', 'DIRECT_TO_SCHOOL', 'OPERATIONAL', 'Default shipping route (DIRECT_TO_SCHOOL or PLATFORM_QC_THEN_SCHOOL)'),
    ('PLATFORM_CURRENCY', 'INR', 'OPERATIONAL', 'Base platform currency code')
ON CONFLICT (setting_key) DO NOTHING;
