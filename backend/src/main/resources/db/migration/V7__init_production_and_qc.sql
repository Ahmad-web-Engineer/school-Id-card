-- V7__init_production_and_qc.sql
-- Production batches, progress tracking, quality control inspections, and reprint requests

CREATE TABLE production_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID NOT NULL REFERENCES agency_assignments(id) ON DELETE RESTRICT,
    batch_number VARCHAR(50) NOT NULL UNIQUE,
    planned_quantity INTEGER NOT NULL CHECK (planned_quantity > 0),
    actual_quantity INTEGER NOT NULL DEFAULT 0,
    production_status VARCHAR(50) NOT NULL DEFAULT 'PLANNED',
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE production_updates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES production_batches(id) ON DELETE CASCADE,
    updated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    printed_quantity INTEGER NOT NULL DEFAULT 0,
    rejected_quantity INTEGER NOT NULL DEFAULT 0,
    pending_quantity INTEGER NOT NULL DEFAULT 0,
    remarks TEXT,
    evidence_file_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE quality_checks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID NOT NULL REFERENCES agency_assignments(id) ON DELETE RESTRICT,
    check_number VARCHAR(50) NOT NULL UNIQUE,
    inspected_quantity INTEGER NOT NULL CHECK (inspected_quantity > 0),
    accepted_quantity INTEGER NOT NULL CHECK (accepted_quantity >= 0),
    rejected_quantity INTEGER NOT NULL CHECK (rejected_quantity >= 0),
    defect_description TEXT,
    defect_category VARCHAR(50),
    inspected_by UUID REFERENCES users(id) ON DELETE SET NULL,
    inspection_status VARCHAR(50) NOT NULL DEFAULT 'PASSED',
    notes TEXT,
    inspected_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_qc_sum CHECK (accepted_quantity + rejected_quantity = inspected_quantity)
);

CREATE TABLE reprint_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID NOT NULL REFERENCES agency_assignments(id) ON DELETE RESTRICT,
    quality_check_id UUID REFERENCES quality_checks(id) ON DELETE SET NULL,
    request_number VARCHAR(50) NOT NULL UNIQUE,
    replacement_quantity INTEGER NOT NULL CHECK (replacement_quantity > 0),
    reason TEXT NOT NULL,
    cost_bearer VARCHAR(50) NOT NULL DEFAULT 'AGENCY',
    approval_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    approved_by UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_at TIMESTAMPTZ,
    rejection_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance and non-negotiable rule 6
CREATE INDEX idx_production_batches_assignment_id ON production_batches(assignment_id);
CREATE INDEX idx_production_batches_status ON production_batches(production_status);
CREATE INDEX idx_production_updates_batch_id ON production_updates(batch_id);
CREATE INDEX idx_quality_checks_assignment_id ON quality_checks(assignment_id);
CREATE INDEX idx_quality_checks_status ON quality_checks(inspection_status);
CREATE INDEX idx_reprint_requests_assignment_id ON reprint_requests(assignment_id);
CREATE INDEX idx_reprint_requests_qc_id ON reprint_requests(quality_check_id);
CREATE INDEX idx_reprint_requests_status ON reprint_requests(approval_status);
