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
