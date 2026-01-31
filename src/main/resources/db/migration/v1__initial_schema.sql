CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    rfid_tag VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'USER',
    clocked_in BOOLEAN DEFAULT FALSE,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE time_entries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    punch_in TIMESTAMP NOT NULL,
    punch_out TIMESTAMP,
    work_date DATE NOT NULL,
    break_minutes INTEGER DEFAULT 0,
    auto_closed_out BOOLEAN DEFAULT FALSE
);

CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    action VARCHAR(50) NOT NULL,
    user_id BIGINT,
    rfid_used VARCHAR(255),
    details VARCHAR(500)
);

CREATE INDEX idx_users_rfid ON users(rfid_tag);
CREATE INDEX idx_time_entries_user ON time_entries(user_id);
CREATE INDEX idx_time_entries_date ON time_entries(work_date);
CREATE INDEX idx_audit_log_user ON audit_log(user_id);