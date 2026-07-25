ALTER TABLE appointments
    ADD completed_at TIMESTAMP WITHOUT TIME ZONE;

CREATE INDEX idx_appointments_completed
    ON appointments (status, completed_at)
    WHERE status = 'COMPLETED';

ALTER TABLE users
DROP
COLUMN password;