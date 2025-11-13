CREATE TABLE employees (
                           id BIGSERIAL PRIMARY KEY,
                           employee_id VARCHAR(50) UNIQUE NOT NULL,
                           email VARCHAR(255) UNIQUE NOT NULL,
                           zoho_erec_no VARCHAR(50) UNIQUE NOT NULL,
                           full_name VARCHAR(255) NOT NULL,
                           is_active BOOLEAN DEFAULT true,
                           created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           CONSTRAINT chk_email_domain CHECK (email LIKE '%@eservecloud.in')
);

-- Add indexes for performance optimization
CREATE INDEX idx_employee_id ON employees (employee_id);
CREATE INDEX idx_email ON employees (email);
CREATE INDEX idx_is_active ON employees (is_active);


-- Create the ENUM type (only if it doesn’t exist)
DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'employee_role') THEN
            CREATE TYPE employee_role AS ENUM ('USER', 'ADMIN');
        END IF;
    END
$$;


ALTER TABLE employees ADD COLUMN role employee_role DEFAULT 'USER' NOT NULL;

CREATE INDEX IF NOT EXISTS idx_role ON employees (role);

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

ALTER TABLE employees
    ADD COLUMN sid UUID DEFAULT uuid_generate_v4();

ALTER TABLE employees
    DROP CONSTRAINT employees_pkey;

ALTER TABLE employees
    DROP COLUMN id;

ALTER TABLE employees
    ADD CONSTRAINT employees_pkey PRIMARY KEY (sid);

