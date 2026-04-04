CREATE TABLE IF NOT EXISTS users (
                                     id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    display_name  VARCHAR(255) NOT NULL,
    student_id    VARCHAR(100) NOT NULL UNIQUE,
    user_type     VARCHAR(20)  NOT NULL CHECK (user_type IN ('SELLER', 'BUYER')),
    password_hash VARCHAR(255) NOT NULL,
    profile_image_url VARCHAR(500),
    created_at    TIMESTAMP NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP NOT NULL DEFAULT now()
    );