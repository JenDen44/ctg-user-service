CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'EMPLOYEE'))
);

CREATE UNIQUE INDEX idx_users_email ON users(email);

INSERT INTO users (full_name, email, password, role)
VALUES
    ('Super Admin', 'superadmin@example.com', '{bcrypt}$2a$12$.X74tctFnXlsFYIgYnijwu.o70aEoWUyxIt0Crx6kh0cewFs4T0/W', 'SUPER_ADMIN');