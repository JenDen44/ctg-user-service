CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'EMPLOYEE')),
    token_version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_users_email ON users(email);

INSERT INTO users (full_name, email, password, role)
VALUES
    ('Super Admin', 'superadmin@example.com', '{bcrypt}$2a$12$.X74tctFnXlsFYIgYnijwu.o70aEoWUyxIt0Crx6kh0cewFs4T0/W', 'SUPER_ADMIN');