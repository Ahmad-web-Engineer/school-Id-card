-- Flyway Migration V9: Finance, Invoices, Payments, and Agency Settlements

CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES printing_orders(id) ON DELETE RESTRICT,
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE RESTRICT,
    payment_reference VARCHAR(50) NOT NULL UNIQUE,
    amount NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    payment_method VARCHAR(50) NOT NULL DEFAULT 'BANK_TRANSFER',
    payment_status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED',
    transaction_reference VARCHAR(100),
    payment_notes TEXT,
    recorded_by UUID REFERENCES users(id),
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES printing_orders(id) ON DELETE RESTRICT,
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE RESTRICT,
    quotation_id UUID REFERENCES quotations(id) ON DELETE RESTRICT,
    invoice_number VARCHAR(50) NOT NULL UNIQUE,
    subtotal NUMERIC(14,2) NOT NULL,
    discount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    tax_amount NUMERIC(14,2) NOT NULL,
    total_amount NUMERIC(14,2) NOT NULL,
    paid_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    due_amount NUMERIC(14,2) NOT NULL,
    invoice_status VARCHAR(50) NOT NULL DEFAULT 'ISSUED',
    issued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date DATE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agency_settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID NOT NULL REFERENCES agency_assignments(id) ON DELETE RESTRICT,
    agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE RESTRICT,
    settlement_number VARCHAR(50) NOT NULL UNIQUE,
    agreed_amount NUMERIC(14,2) NOT NULL,
    deduction_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    penalty_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    payable_amount NUMERIC(14,2) NOT NULL,
    paid_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    outstanding_amount NUMERIC(14,2) NOT NULL,
    settlement_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    payment_reference VARCHAR(100),
    settled_at TIMESTAMPTZ,
    settled_by UUID REFERENCES users(id),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cost_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES printing_orders(id) ON DELETE CASCADE,
    assignment_id UUID REFERENCES agency_assignments(id) ON DELETE SET NULL,
    cost_category VARCHAR(50) NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_school_id ON payments(school_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(payment_status);
CREATE INDEX IF NOT EXISTS idx_payments_reference ON payments(payment_reference);

CREATE INDEX IF NOT EXISTS idx_invoices_order_id ON invoices(order_id);
CREATE INDEX IF NOT EXISTS idx_invoices_school_id ON invoices(school_id);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices(invoice_status);
CREATE INDEX IF NOT EXISTS idx_invoices_number ON invoices(invoice_number);

CREATE INDEX IF NOT EXISTS idx_agency_settlements_assignment_id ON agency_settlements(assignment_id);
CREATE INDEX IF NOT EXISTS idx_agency_settlements_agency_id ON agency_settlements(agency_id);
CREATE INDEX IF NOT EXISTS idx_agency_settlements_status ON agency_settlements(settlement_status);

CREATE INDEX IF NOT EXISTS idx_cost_entries_order_id ON cost_entries(order_id);
CREATE INDEX IF NOT EXISTS idx_cost_entries_assignment_id ON cost_entries(assignment_id);
