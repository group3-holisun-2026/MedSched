CREATE TABLE audit_log
(
    id          UUID         NOT NULL,
    user_id     UUID         NOT NULL,
    action      VARCHAR(255) NOT NULL,
    entity_name VARCHAR(255) NOT NULL,
    entity_id   UUID         NOT NULL,
    timestamp   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_audit_log PRIMARY KEY (id)
);

CREATE TABLE consultation_records
(
    id                  UUID    NOT NULL,
    appointment_id      UUID    NOT NULL,
    presentation_motive TEXT,
    anamnesis           TEXT,
    clinical_exam       TEXT,
    diagnosis           TEXT,
    prescription        TEXT,
    locked              BOOLEAN NOT NULL,
    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_consultation_records PRIMARY KEY (id)
);

CREATE TABLE patient
(
    id               UUID         NOT NULL,
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    phone            VARCHAR(20)  NOT NULL,
    cnp              VARCHAR(512),
    cnp_hash         VARCHAR(64),
    date_of_birth    date,
    email            VARCHAR(255),
    allergies        TEXT,
    medical_history  TEXT,
    profile_complete BOOLEAN      NOT NULL,
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_patient PRIMARY KEY (id)
);

ALTER TABLE consultation_records
    ADD CONSTRAINT uc_consultation_records_appointment_id UNIQUE (appointment_id);

ALTER TABLE patient
    ADD CONSTRAINT uc_patient_cnp_hash UNIQUE (cnp_hash);

ALTER TABLE work_schedules
ALTER
COLUMN day_of_week TYPE VARCHAR(255) USING (day_of_week::VARCHAR(255));

ALTER TABLE equipments
ALTER
COLUMN name TYPE VARCHAR(150) USING (name::VARCHAR(150));