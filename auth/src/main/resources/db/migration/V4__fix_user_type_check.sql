-- Drop old check constraints
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_user_type_check;
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_user_type;

-- Ensure default + data are valid
ALTER TABLE users ALTER COLUMN user_type SET DEFAULT 'STUDENT';
UPDATE users
SET user_type = 'STUDENT'
WHERE user_type IS NULL OR user_type NOT IN ('STUDENT');

-- Add correct check constraint
ALTER TABLE users
    ADD CONSTRAINT users_user_type_check
        CHECK (user_type IN ('STUDENT'));
