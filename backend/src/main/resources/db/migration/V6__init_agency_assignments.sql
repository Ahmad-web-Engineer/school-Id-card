-- =========================================================================
-- V6__init_agency_assignments.sql
-- Step 6: Agency Management and Job Assignment (Child assignments, splitting, and tracking)
-- =========================================================================

-- 1. Agency Assignments table (Internal jobs assigned to printing agencies)
CREATE TABLE agency_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_number VARCHAR(50) NOT NULL,
    order_id UUID NOT NULL REFERENCES printing_orders(id) ON DELETE RESTRICT,
    agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE RESTRICT,
    assigned_by UUID REFERENCES users(id) ON DELETE SET NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expected_delivery_date DATE,
    agreed_rate NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    total_agreed_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    assignment_status VARCHAR(50) NOT NULL DEFAULT 'ASSIGNED',
    reassigned_from_id UUID REFERENCES agency_assignments(id) ON DELETE SET NULL,
    reassignment_reason TEXT,
    rejection_reason TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_assignment_number UNIQUE (assignment_number)
);

-- 2. Agency Job Items table (Allocated quantities per order item for this assignment)
CREATE TABLE agency_job_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID NOT NULL REFERENCES agency_assignments(id) ON DELETE CASCADE,
    order_item_id UUID NOT NULL REFERENCES printing_order_items(id) ON DELETE RESTRICT,
    assigned_quantity INT NOT NULL DEFAULT 0,
    printed_quantity INT NOT NULL DEFAULT 0,
    rejected_quantity INT NOT NULL DEFAULT 0,
    accepted_quantity INT NOT NULL DEFAULT 0,
    pending_quantity INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Performance & Foreign Key Indexes (AGENTS.md Rule 6)
CREATE INDEX idx_agency_assignments_order_id ON agency_assignments(order_id);
CREATE INDEX idx_agency_assignments_agency_id ON agency_assignments(agency_id);
CREATE INDEX idx_agency_assignments_status ON agency_assignments(assignment_status);
CREATE INDEX idx_agency_assignments_number ON agency_assignments(assignment_number);
CREATE INDEX idx_agency_assignments_reassigned_from ON agency_assignments(reassigned_from_id);
CREATE INDEX idx_agency_assignments_expected_delivery ON agency_assignments(expected_delivery_date);
CREATE INDEX idx_agency_assignments_agency_status ON agency_assignments(agency_id, assignment_status);
CREATE INDEX idx_agency_assignments_created_at ON agency_assignments(created_at);

CREATE INDEX idx_agency_job_items_assignment_id ON agency_job_items(assignment_id);
CREATE INDEX idx_agency_job_items_order_item_id ON agency_job_items(order_item_id);
