-- V15: Add Print Settings and Agency Customization Permissions

ALTER TABLE agency_assignments
    ADD COLUMN IF NOT EXISTS print_settings_json TEXT,
    ADD COLUMN IF NOT EXISTS allow_agency_override BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE printing_orders
    ADD COLUMN IF NOT EXISTS print_settings_json TEXT;

COMMENT ON COLUMN agency_assignments.print_settings_json IS 'JSON-encoded prepress print specifications including sheet size, grid layout, gaps, margins, and sides';
COMMENT ON COLUMN agency_assignments.allow_agency_override IS 'Flag set by Admin determining if Printing Agency is authorized to adjust print layout settings';
COMMENT ON COLUMN printing_orders.print_settings_json IS 'JSON-encoded default prepress print specifications configured for the order';
