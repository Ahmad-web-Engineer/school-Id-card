-- =========================================================================
-- V12__seed_demo_organizations_and_dashboards.sql
-- Seed default demo organizations (School, Agency), active user accounts,
-- template, sample students, and sample order for instant dashboard & testing
-- =========================================================================

-- 1. Demo School: Greenwood International School
INSERT INTO schools (id, school_name, school_code, address, city, state, pincode, email, mobile, status)
SELECT 
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'Greenwood International School',
    'GWIS-001',
    '14 Heritage Boulevard, Indiranagar',
    'Bengaluru',
    'Karnataka',
    '560038',
    'school@greenwood.edu',
    '9876500001',
    'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM schools WHERE school_code = 'GWIS-001');

-- 2. Demo School User (school@greenwood.edu / Password123!)
INSERT INTO users (id, full_name, email, mobile, password_hash, status)
SELECT 
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'Dr. Rajesh Verma',
    'school@greenwood.edu',
    '9876500001',
    '$2a$10$YHfA04pKpFm120UuLEVfieU3FBVIXMDzvJ9Bf5lia/jxfStyhAveu',
    'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'school@greenwood.edu');

-- Assign SCHOOL role to Dr. Rajesh Verma
INSERT INTO user_roles (user_id, role_id)
SELECT 
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    '22222222-2222-2222-2222-222222222222'
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles 
    WHERE user_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb' 
      AND role_id = '22222222-2222-2222-2222-222222222222'
);

-- Map user to Greenwood International School
INSERT INTO school_users (id, user_id, school_id, designation)
SELECT 
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0001',
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'Principal'
WHERE NOT EXISTS (
    SELECT 1 FROM school_users 
    WHERE user_id = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb' 
      AND school_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
);

-- 3. Demo Printing Agency: Apex Card Printers & Graphics
INSERT INTO agencies (id, agency_name, contact_person, email, mobile, address, production_capacity, capabilities, status)
SELECT 
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    'Apex Card Printers & Graphics',
    'Suresh Patel',
    'agency@apexprints.com',
    '9876500002',
    'Plot 45, Peenya Industrial Area, Phase II, Bengaluru',
    5000,
    'Thermal Transfer, Retransfer, UV Lamination, Embossing',
    'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM agencies WHERE email = 'agency@apexprints.com');

-- 4. Demo Agency User (agency@apexprints.com / Password123!)
INSERT INTO users (id, full_name, email, mobile, password_hash, status)
SELECT 
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    'Suresh Patel',
    'agency@apexprints.com',
    '9876500002',
    '$2a$10$YHfA04pKpFm120UuLEVfieU3FBVIXMDzvJ9Bf5lia/jxfStyhAveu',
    'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'agency@apexprints.com');

-- Assign PRINTING_AGENCY role to Suresh Patel
INSERT INTO user_roles (user_id, role_id)
SELECT 
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    '33333333-3333-3333-3333-333333333333'
WHERE NOT EXISTS (
    SELECT 1 FROM user_roles 
    WHERE user_id = 'dddddddd-dddd-dddd-dddd-dddddddddddd' 
      AND role_id = '33333333-3333-3333-3333-333333333333'
);

-- Map user to Apex Card Printers
INSERT INTO agency_users (id, user_id, agency_id, designation)
SELECT 
    'dddddddd-dddd-dddd-dddd-dddddddd0001',
    'dddddddd-dddd-dddd-dddd-dddddddddddd',
    'cccccccc-cccc-cccc-cccc-cccccccccccc',
    'Production Head'
WHERE NOT EXISTS (
    SELECT 1 FROM agency_users 
    WHERE user_id = 'dddddddd-dddd-dddd-dddd-dddddddddddd' 
      AND agency_id = 'cccccccc-cccc-cccc-cccc-cccccccccccc'
);

-- 5. Seed Demo Students for Greenwood International School
INSERT INTO students (id, school_id, admission_number, student_name, father_name, mother_name, date_of_birth, gender, blood_group, class_name, section, roll_number, address, mobile, status)
SELECT 
    '11111111-0000-0000-0000-000000000001',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'GW-2026-001',
    'Aarav Sharma',
    'Rajesh Sharma',
    'Sunita Sharma',
    '2011-04-12',
    'MALE',
    'O+',
    '10',
    'A',
    '01',
    'Flat 302, Palm Meadows, Indiranagar',
    '9876500011',
    'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM students WHERE school_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa' AND admission_number = 'GW-2026-001');

INSERT INTO students (id, school_id, admission_number, student_name, father_name, mother_name, date_of_birth, gender, blood_group, class_name, section, roll_number, address, mobile, status)
SELECT 
    '11111111-0000-0000-0000-000000000002',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'GW-2026-002',
    'Diya Patel',
    'Kiran Patel',
    'Meera Patel',
    '2011-08-23',
    'FEMALE',
    'B+',
    '10',
    'A',
    '02',
    'House 12, Defence Colony, Bengaluru',
    '9876500012',
    'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM students WHERE school_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa' AND admission_number = 'GW-2026-002');

INSERT INTO students (id, school_id, admission_number, student_name, father_name, mother_name, date_of_birth, gender, blood_group, class_name, section, roll_number, address, mobile, status)
SELECT 
    '11111111-0000-0000-0000-000000000003',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'GW-2026-003',
    'Rohan Kulkarni',
    'Anand Kulkarni',
    'Pooja Kulkarni',
    '2012-01-15',
    'MALE',
    'A+',
    '9',
    'B',
    '14',
    '77 4th Cross, Malleshwaram, Bengaluru',
    '9876500013',
    'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM students WHERE school_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa' AND admission_number = 'GW-2026-003');

-- 6. Seed Demo Approved ID Card Template (Greenwood Standard Card)
INSERT INTO card_templates (id, template_name, template_category, school_id, status)
SELECT 
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
    'Greenwood Classic Student Card',
    'STUDENT',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM card_templates WHERE template_name = 'Greenwood Classic Student Card');

-- Seed Template Version 1 (APPROVED)
INSERT INTO template_versions (id, template_id, version_number, card_width_mm, card_height_mm, bleed_margin_mm, front_design_json, back_design_json, approval_status, created_by, approved_by, approved_at)
SELECT 
    'ffffffff-ffff-ffff-ffff-ffffffffffff',
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
    1,
    85.60,
    54.00,
    3.00,
    '{"backgroundColor":"#ffffff","elements":[{"type":"text","text":"Greenwood International School","left":20,"top":10,"fontSize":14,"fontWeight":"bold","fill":"#1e3a8a"}]}',
    '{"backgroundColor":"#f8fafc","elements":[{"type":"text","text":"If found, please return to school office.","left":20,"top":20,"fontSize":10,"fill":"#64748b"}]}',
    'APPROVED',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM template_versions WHERE id = 'ffffffff-ffff-ffff-ffff-ffffffffffff');

-- Assign template to Greenwood High School
INSERT INTO template_assignments (id, template_id, school_id, academic_session, active)
SELECT 
    'eeeeffff-0000-0000-0000-000000000001',
    'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    '2026-2027',
    TRUE
WHERE NOT EXISTS (SELECT 1 FROM template_assignments WHERE id = 'eeeeffff-0000-0000-0000-000000000001');
