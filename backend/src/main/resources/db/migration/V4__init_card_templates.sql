-- =========================================================================
-- V4__init_card_templates.sql
-- Step 4: ID Card Template Designer (Templates, Versions & Assignments)
-- =========================================================================

-- 1. Card Templates table (stores template header / metadata)
-- school_id is NULL for global templates available to all schools
CREATE TABLE card_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_name VARCHAR(150) NOT NULL,
    template_category VARCHAR(50) NOT NULL DEFAULT 'STUDENT',
    school_id UUID REFERENCES schools(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_template_name UNIQUE (template_name)
);

-- 2. Template Versions table (stores versioned canvas dimensions & JSON designs)
-- Approval workflow: DRAFT -> PENDING_APPROVAL -> APPROVED -> ARCHIVED
CREATE TABLE template_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES card_templates(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    card_width_mm NUMERIC(8,2) NOT NULL DEFAULT 85.60,
    card_height_mm NUMERIC(8,2) NOT NULL DEFAULT 54.00,
    bleed_margin_mm NUMERIC(8,2) NOT NULL DEFAULT 3.00,
    front_design_json TEXT NOT NULL,
    back_design_json TEXT NOT NULL,
    approval_status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_by UUID REFERENCES users(id) ON DELETE SET NULL,
    approved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_template_version UNIQUE (template_id, version_number)
);

-- 3. Template Assignments table (assigns approved templates to schools per session)
CREATE TABLE template_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES card_templates(id) ON DELETE CASCADE,
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    academic_session VARCHAR(50) NOT NULL DEFAULT '2026-2027',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_school_template_session UNIQUE (template_id, school_id, academic_session)
);

-- 4. Performance & Foreign Key Indexes (AGENTS.md Rule 6)

-- card_templates indexes
CREATE INDEX idx_card_templates_school_id ON card_templates(school_id);
CREATE INDEX idx_card_templates_status ON card_templates(status);
CREATE INDEX idx_card_templates_category ON card_templates(template_category);
CREATE INDEX idx_card_templates_school_status ON card_templates(school_id, status);
CREATE INDEX idx_card_templates_created_at ON card_templates(created_at);

-- template_versions indexes
CREATE INDEX idx_template_versions_template_id ON template_versions(template_id);
CREATE INDEX idx_template_versions_created_by ON template_versions(created_by);
CREATE INDEX idx_template_versions_approved_by ON template_versions(approved_by);
CREATE INDEX idx_template_versions_approval_status ON template_versions(approval_status);
CREATE INDEX idx_template_versions_tmpl_status ON template_versions(template_id, approval_status);
CREATE INDEX idx_template_versions_created_at ON template_versions(created_at);

-- template_assignments indexes
CREATE INDEX idx_template_assignments_template_id ON template_assignments(template_id);
CREATE INDEX idx_template_assignments_school_id ON template_assignments(school_id);
CREATE INDEX idx_template_assignments_academic_session ON template_assignments(academic_session);
CREATE INDEX idx_template_assignments_active ON template_assignments(active);
CREATE INDEX idx_template_assignments_school_session ON template_assignments(school_id, academic_session, active);
CREATE INDEX idx_template_assignments_assigned_at ON template_assignments(assigned_at);
