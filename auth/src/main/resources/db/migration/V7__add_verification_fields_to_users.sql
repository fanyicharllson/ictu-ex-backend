-- Add verification code and expiry to users table
ALTER TABLE users ADD COLUMN verification_code VARCHAR(20);
ALTER TABLE users ADD COLUMN verification_code_expires_at TIMESTAMP;
ALTER TABLE users ADD COLUMN is_verified BOOLEAN DEFAULT FALSE;
