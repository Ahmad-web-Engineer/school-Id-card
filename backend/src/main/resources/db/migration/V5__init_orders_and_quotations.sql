-- =========================================================================
-- V5__init_orders_and_quotations.sql
-- Step 5: Orders and Quotations (Parent Orders, Order Items, Student Snapshots, Quotations)
-- =========================================================================

-- 1. Printing Orders table (Parent Orders placed by School)
CREATE TABLE printing_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(50) NOT NULL,
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE RESTRICT,
    academic_session VARCHAR(50) NOT NULL DEFAULT '2026-2027',
    template_version_id UUID NOT NULL REFERENCES template_versions(id) ON DELETE RESTRICT,
    total_students INT NOT NULL DEFAULT 0,
    total_quantity INT NOT NULL DEFAULT 0,
    order_status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    delivery_model VARCHAR(50) NOT NULL DEFAULT 'DIRECT_TO_SCHOOL',
    delivery_address TEXT,
    contact_person VARCHAR(100),
    contact_mobile VARCHAR(20),
    remarks TEXT,
    rejection_remarks TEXT,
    submitted_at TIMESTAMPTZ,
    verified_at TIMESTAMPTZ,
    verified_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_order_number UNIQUE (order_number)
);

-- 2. Printing Order Items table (line items: PVC cards, lanyards, holders, etc.)
CREATE TABLE printing_order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES printing_orders(id) ON DELETE CASCADE,
    product_type VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    unit_selling_price NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    item_total NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Order Student Items table (immutable snapshot of student info at order time)
CREATE TABLE order_student_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES printing_orders(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
    admission_number VARCHAR(100) NOT NULL,
    student_name VARCHAR(200) NOT NULL,
    father_name VARCHAR(200),
    mother_name VARCHAR(200),
    date_of_birth DATE,
    gender VARCHAR(20),
    blood_group VARCHAR(10),
    class_name VARCHAR(50) NOT NULL,
    section VARCHAR(20),
    roll_number VARCHAR(50),
    address TEXT,
    emergency_contact VARCHAR(20),
    photo_file_id UUID REFERENCES files(id) ON DELETE SET NULL,
    template_version_id UUID NOT NULL REFERENCES template_versions(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_order_student UNIQUE (order_id, student_id)
);

-- 4. Quotations table (Admin-generated commercial quote for an order)
CREATE TABLE quotations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quotation_number VARCHAR(50) NOT NULL,
    order_id UUID NOT NULL REFERENCES printing_orders(id) ON DELETE CASCADE,
    subtotal NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    discount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    tax_rate NUMERIC(6,2) NOT NULL DEFAULT 18.00,
    tax_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    total_amount NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    quotation_status VARCHAR(50) NOT NULL DEFAULT 'GENERATED',
    valid_until DATE,
    generated_by UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_by UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_at TIMESTAMPTZ,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_quotation_number UNIQUE (quotation_number),
    CONSTRAINT uq_quotation_order UNIQUE (order_id)
);

-- 5. Quotation Items table (Itemized quotation lines)
CREATE TABLE quotation_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quotation_id UUID NOT NULL REFERENCES quotations(id) ON DELETE CASCADE,
    product_description VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price NUMERIC(14,2) NOT NULL DEFAULT 0.00,
    amount NUMERIC(14,2) NOT NULL DEFAULT 0.00
);

-- 6. Performance & Foreign Key Indexes (AGENTS.md Rule 6)
CREATE INDEX idx_printing_orders_school_id ON printing_orders(school_id);
CREATE INDEX idx_printing_orders_status ON printing_orders(order_status);
CREATE INDEX idx_printing_orders_order_number ON printing_orders(order_number);
CREATE INDEX idx_printing_orders_template_version ON printing_orders(template_version_id);
CREATE INDEX idx_printing_orders_created_at ON printing_orders(created_at);
CREATE INDEX idx_printing_orders_school_status ON printing_orders(school_id, order_status);

CREATE INDEX idx_printing_order_items_order_id ON printing_order_items(order_id);

CREATE INDEX idx_order_student_items_order_id ON order_student_items(order_id);
CREATE INDEX idx_order_student_items_student_id ON order_student_items(student_id);
CREATE INDEX idx_order_student_items_photo ON order_student_items(photo_file_id);

CREATE INDEX idx_quotations_order_id ON quotations(order_id);
CREATE INDEX idx_quotations_quotation_number ON quotations(quotation_number);
CREATE INDEX idx_quotations_status ON quotations(quotation_status);

CREATE INDEX idx_quotation_items_quotation_id ON quotation_items(quotation_id);
