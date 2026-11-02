-- Update default super admin user password to valid BCrypt hash for Password123!
UPDATE users 
SET password_hash = '$2a$10$YHfA04pKpFm120UuLEVfieU3FBVIXMDzvJ9Bf5lia/jxfStyhAveu'
WHERE email = 'admin@platform.com';
