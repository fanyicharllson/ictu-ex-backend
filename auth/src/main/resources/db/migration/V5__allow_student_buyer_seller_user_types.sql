ALTER TABLE users DROP CONSTRAINT IF EXISTS users_user_type_check;

ALTER TABLE users
    ADD CONSTRAINT users_user_type_check
        CHECK (user_type IN ('STUDENT', 'BUYER', 'SELLER'));

ALTER TABLE users ALTER COLUMN user_type SET DEFAULT 'STUDENT';

UPDATE users
SET user_type = 'STUDENT'
WHERE user_type IS NULL OR user_type NOT IN ('STUDENT', 'BUYER', 'SELLER');

