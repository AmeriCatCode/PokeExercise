-- Registered users (secondary entity: user management).
-- Username and email are stored normalized (trimmed, lower case) by the application,
-- so plain unique constraints are enough to forbid duplicates in any letter case.
CREATE TABLE app_user (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(30)  NOT NULL,
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_app_user_username UNIQUE (username),
    CONSTRAINT uk_app_user_email UNIQUE (email),
    CONSTRAINT ck_app_user_role CHECK (role IN ('USER', 'ADMIN'))
);
