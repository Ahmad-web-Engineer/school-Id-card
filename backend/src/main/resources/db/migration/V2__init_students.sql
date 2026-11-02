-- =========================================================================
-- V2__init_students.sql
-- Step 2: Students Master Records
-- =========================================================================

CREATE TABLE students (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    admission_number VARCHAR(50) NOT NULL,
    student_name VARCHAR(150) NOT NULL,
    father_name VARCHAR(150),
    mother_name VARCHAR(150),
    date_of_birth DATE,
    gender VARCHAR(20),
    blood_group VARCHAR(10),
    class_name VARCHAR(50) NOT NULL,
    section VARCHAR(20),
    roll_number VARCHAR(30),
    address VARCHAR(300),
    mobile VARCHAR(20),
    emergency_contact VARCHAR(20),
    photo_file_id VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_school_admission UNIQUE (school_id, admission_number)
);

-- Performance & Foreign Key Indexes (Rule 6)
CREATE INDEX idx_students_school_id ON students(school_id);
CREATE INDEX idx_students_class_section ON students(school_id, class_name, section);
CREATE INDEX idx_students_admission ON students(school_id, admission_number);
CREATE INDEX idx_students_status ON students(status);
CREATE INDEX idx_students_name ON students(school_id, student_name);
