-- =========================================================================
-- V3__init_files_and_imports.sql
-- Step 3: Files, Student Imports (Excel & CSV), and Photo Matching
-- =========================================================================

-- 1. Files table (stores metadata for all uploaded documents, templates, photos)
CREATE TABLE files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    school_id UUID REFERENCES schools(id) ON DELETE CASCADE,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Student Import Batches table
CREATE TABLE student_import_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    uploaded_by UUID REFERENCES users(id) ON DELETE SET NULL,
    original_file_id UUID REFERENCES files(id) ON DELETE SET NULL,
    file_type VARCHAR(20) NOT NULL,
    total_rows INT NOT NULL DEFAULT 0,
    valid_rows INT NOT NULL DEFAULT 0,
    error_rows INT NOT NULL DEFAULT 0,
    imported_rows INT NOT NULL DEFAULT 0,
    import_status VARCHAR(50) NOT NULL DEFAULT 'PROCESSING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ
);

-- 3. Student Import Rows table (stores staging data for each row in the batch)
CREATE TABLE student_import_rows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES student_import_batches(id) ON DELETE CASCADE,
    row_number INT NOT NULL,
    admission_number VARCHAR(100),
    student_name VARCHAR(255),
    father_name VARCHAR(255),
    mother_name VARCHAR(255),
    date_of_birth VARCHAR(50),
    gender VARCHAR(20),
    blood_group VARCHAR(10),
    class_name VARCHAR(100),
    section VARCHAR(50),
    roll_number VARCHAR(50),
    address TEXT,
    mobile VARCHAR(20),
    emergency_contact VARCHAR(20),
    raw_data_json TEXT,
    validation_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT
);

-- 4. Student Import Errors table (field-level validation error details)
CREATE TABLE student_import_errors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES student_import_batches(id) ON DELETE CASCADE,
    row_number INT NOT NULL,
    field_name VARCHAR(100),
    error_message TEXT NOT NULL
);

-- 5. Photo Import Batches table
CREATE TABLE photo_import_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    uploaded_by UUID REFERENCES users(id) ON DELETE SET NULL,
    original_file_id UUID REFERENCES files(id) ON DELETE SET NULL,
    selected_class VARCHAR(100),
    selected_section VARCHAR(50),
    total_files INT NOT NULL DEFAULT 0,
    matched_files INT NOT NULL DEFAULT 0,
    unmatched_files INT NOT NULL DEFAULT 0,
    duplicate_files INT NOT NULL DEFAULT 0,
    import_status VARCHAR(50) NOT NULL DEFAULT 'PROCESSING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Photo Import Results table (individual image matching outcomes)
CREATE TABLE photo_import_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES photo_import_batches(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    parsed_class VARCHAR(100),
    parsed_section VARCHAR(50),
    parsed_roll_number VARCHAR(50),
    matched_student_id UUID REFERENCES students(id) ON DELETE SET NULL,
    photo_file_id UUID REFERENCES files(id) ON DELETE SET NULL,
    match_status VARCHAR(50) NOT NULL,
    message TEXT
);

-- 7. Add foreign key constraint from students(photo_file_id) to files(id)
ALTER TABLE students 
    ALTER COLUMN photo_file_id TYPE UUID USING NULLIF(photo_file_id, '')::uuid;

ALTER TABLE students 
    ADD CONSTRAINT fk_students_photo_file 
    FOREIGN KEY (photo_file_id) REFERENCES files(id) 
    ON DELETE SET NULL;

-- 8. Performance & Foreign Key Indexes (AGENTS.md Rule 6)
-- files indexes
CREATE INDEX idx_files_school_id ON files(school_id);
CREATE INDEX idx_files_created_by ON files(created_by);
CREATE INDEX idx_files_created_at ON files(created_at);

-- student_import_batches indexes
CREATE INDEX idx_student_import_batches_school_id ON student_import_batches(school_id);
CREATE INDEX idx_student_import_batches_uploaded_by ON student_import_batches(uploaded_by);
CREATE INDEX idx_student_import_batches_original_file_id ON student_import_batches(original_file_id);
CREATE INDEX idx_student_import_batches_import_status ON student_import_batches(import_status);
CREATE INDEX idx_student_import_batches_created_at ON student_import_batches(created_at);

-- student_import_rows indexes
CREATE INDEX idx_student_import_rows_batch_id ON student_import_rows(batch_id);
CREATE INDEX idx_student_import_rows_validation_status ON student_import_rows(validation_status);
CREATE INDEX idx_student_import_rows_batch_row ON student_import_rows(batch_id, row_number);
CREATE INDEX idx_student_import_rows_batch_status ON student_import_rows(batch_id, validation_status);

-- student_import_errors indexes
CREATE INDEX idx_student_import_errors_batch_id ON student_import_errors(batch_id);
CREATE INDEX idx_student_import_errors_batch_row ON student_import_errors(batch_id, row_number);

-- photo_import_batches indexes
CREATE INDEX idx_photo_import_batches_school_id ON photo_import_batches(school_id);
CREATE INDEX idx_photo_import_batches_uploaded_by ON photo_import_batches(uploaded_by);
CREATE INDEX idx_photo_import_batches_original_file_id ON photo_import_batches(original_file_id);
CREATE INDEX idx_photo_import_batches_import_status ON photo_import_batches(import_status);
CREATE INDEX idx_photo_import_batches_created_at ON photo_import_batches(created_at);

-- photo_import_results indexes
CREATE INDEX idx_photo_import_results_batch_id ON photo_import_results(batch_id);
CREATE INDEX idx_photo_import_results_matched_student_id ON photo_import_results(matched_student_id);
CREATE INDEX idx_photo_import_results_photo_file_id ON photo_import_results(photo_file_id);
CREATE INDEX idx_photo_import_results_match_status ON photo_import_results(match_status);
CREATE INDEX idx_photo_import_results_batch_status ON photo_import_results(batch_id, match_status);

-- students indexes
CREATE INDEX idx_students_photo_file_id ON students(photo_file_id);
