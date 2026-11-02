-- Flyway Migration V8: Dispatch and Delivery (Model A & Model B)

CREATE TABLE IF NOT EXISTS dispatch_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID REFERENCES agency_assignments(id) ON DELETE RESTRICT,
    order_id UUID NOT NULL REFERENCES printing_orders(id) ON DELETE RESTRICT,
    dispatch_number VARCHAR(50) NOT NULL UNIQUE,
    courier_name VARCHAR(100) NOT NULL,
    tracking_number VARCHAR(100) NOT NULL,
    dispatch_quantity INTEGER NOT NULL CHECK (dispatch_quantity > 0),
    dispatch_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dispatch_status VARCHAR(50) NOT NULL DEFAULT 'IN_TRANSIT',
    dispatch_type VARCHAR(50) NOT NULL DEFAULT 'AGENCY_TO_SCHOOL',
    dispatched_by UUID REFERENCES users(id),
    dispatch_notes TEXT,
    proof_file_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS delivery_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES printing_orders(id) ON DELETE RESTRICT,
    dispatch_id UUID REFERENCES dispatch_records(id) ON DELETE SET NULL,
    delivery_number VARCHAR(50) NOT NULL UNIQUE,
    delivery_model VARCHAR(50) NOT NULL DEFAULT 'DIRECT_TO_SCHOOL',
    delivery_quantity INTEGER NOT NULL CHECK (delivery_quantity > 0),
    received_by VARCHAR(100) NOT NULL,
    received_mobile VARCHAR(20),
    delivery_status VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
    delivered_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivery_notes TEXT,
    proof_of_delivery_file_id UUID,
    closed_by UUID REFERENCES users(id),
    closed_at TIMESTAMPTZ,
    closure_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_dispatch_records_assignment_id ON dispatch_records(assignment_id);
CREATE INDEX IF NOT EXISTS idx_dispatch_records_order_id ON dispatch_records(order_id);
CREATE INDEX IF NOT EXISTS idx_dispatch_records_status ON dispatch_records(dispatch_status);
CREATE INDEX IF NOT EXISTS idx_dispatch_records_tracking_number ON dispatch_records(tracking_number);
CREATE INDEX IF NOT EXISTS idx_dispatch_records_created_at ON dispatch_records(created_at);

CREATE INDEX IF NOT EXISTS idx_delivery_records_order_id ON delivery_records(order_id);
CREATE INDEX IF NOT EXISTS idx_delivery_records_dispatch_id ON delivery_records(dispatch_id);
CREATE INDEX IF NOT EXISTS idx_delivery_records_status ON delivery_records(delivery_status);
CREATE INDEX IF NOT EXISTS idx_delivery_records_delivered_at ON delivery_records(delivered_at);
