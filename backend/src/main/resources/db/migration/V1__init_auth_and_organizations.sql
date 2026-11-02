-- =========================================================================
-- V1__init_auth_and_organizations.sql
-- Step 1: Authentication, Roles, Organizations (Schools & Agencies)
-- =========================================================================

-- Enable pgcrypto extension for gen_random_uuid() if available
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. Roles table
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- 2. Users table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    mobile VARCHAR(20),
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. User roles mapping table
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- 4. Schools table
CREATE TABLE schools (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_name VARCHAR(200) NOT NULL,
    school_code VARCHAR(50) NOT NULL UNIQUE,
    address VARCHAR(300),
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(20),
    email VARCHAR(150) NOT NULL UNIQUE,
    mobile VARCHAR(20),
    logo_file_id VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Printing Agencies table
CREATE TABLE agencies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_name VARCHAR(200) NOT NULL,
    contact_person VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    mobile VARCHAR(20) NOT NULL,
    address VARCHAR(300),
    production_capacity INTEGER NOT NULL DEFAULT 1000,
    capabilities VARCHAR(500),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. School users mapping table
CREATE TABLE school_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    designation VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_school_user UNIQUE (user_id, school_id)
);

-- 7. Agency users mapping table
CREATE TABLE agency_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    agency_id UUID NOT NULL REFERENCES agencies(id) ON DELETE CASCADE,
    designation VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_agency_user UNIQUE (user_id, agency_id)
);

-- 8. Refresh tokens table
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance & non-negotiable rule 6
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX idx_schools_code ON schools(school_code);
CREATE INDEX idx_schools_status ON schools(status);
CREATE INDEX idx_agencies_status ON agencies(status);
CREATE INDEX idx_school_users_user_id ON school_users(user_id);
CREATE INDEX idx_school_users_school_id ON school_users(school_id);
CREATE INDEX idx_agency_users_user_id ON agency_users(user_id);
CREATE INDEX idx_agency_users_agency_id ON agency_users(agency_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- Seed Initial Roles
INSERT INTO roles (id, role_name, description) VALUES
    ('11111111-1111-1111-1111-111111111111', 'SUPER_ADMIN', 'Platform Administrator with complete operational control'),
    ('22222222-2222-2222-2222-222222222222', 'SCHOOL', 'School Administrator managing student data and card orders'),
    ('33333333-3333-3333-3333-333333333333', 'PRINTING_AGENCY', 'Printing Agency fulfillment partner for card production');

-- Seed Default Super Admin User (password: Password123!)
-- Hash generated with BCrypt 10 rounds: $2a$10$wT55a30Bw8Y0pY3Nsqv6u.3i2c7pZ5XoG7408ZkWXv/X7Y0N2mFf6
INSERT INTO users (id, full_name, email, mobile, password_hash, status) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Super Admin', 'admin@platform.com', '9876543210', '$2a$10$wT55a30Bw8Y0pY3Nsqv6u.3i2c7pZ5XoG7408ZkWXv/X7Y0N2mFf6', 'ACTIVE');

-- Assign SUPER_ADMIN role to default admin user
INSERT INTO user_roles (user_id, role_id) VALUES
    ('00000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111');
