ALTER TABLE users ALTER COLUMN student_id TYPE VARCHAR(100);

-- Remove SELLER/BUYER constraint if it exists
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_user_type;

-- Set default to STUDENT
ALTER TABLE users ALTER COLUMN user_type SET DEFAULT 'STUDENT';

-- Update any NULL or invalid values
UPDATE users SET user_type = 'STUDENT' WHERE user_type IS NULL OR user_type NOT IN ('STUDENT');

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_student_id ON users(student_id);