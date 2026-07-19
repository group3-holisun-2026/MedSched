CREATE SEQUENCE IF NOT EXISTS revinfo_seq START WITH 1 INCREMENT BY 50;

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

CREATE TABLE doctors
(
    id                                     UUID         NOT NULL,
    user_id                                UUID         NOT NULL,
    speciality                             VARCHAR(150) NOT NULL,
    standard_consultation_duration_minutes INTEGER      NOT NULL,
    active                                 BOOLEAN      NOT NULL,
    CONSTRAINT pk_doctors PRIMARY KEY (id)
);

CREATE TABLE equipment_types
(
    id          UUID         NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT pk_equipment_types PRIMARY KEY (id)
);

CREATE TABLE equipments
(
    id                UUID         NOT NULL,
    name              VARCHAR(150) NOT NULL,
    equipment_type_id UUID         NOT NULL,
    room_id           UUID,
    active            BOOLEAN      NOT NULL,
    CONSTRAINT pk_equipments PRIMARY KEY (id)
);

CREATE TABLE patients
(
    id               UUID                  NOT NULL,
    first_name       VARCHAR(100)          NOT NULL,
    last_name        VARCHAR(100)          NOT NULL,
    phone            VARCHAR(20)           NOT NULL,
    cnp              VARCHAR(512),
    cnp_hash         VARCHAR(64),
    date_of_birth    date,
    email            VARCHAR(255),
    allergies        TEXT,
    medical_history  TEXT,
    profile_complete BOOLEAN DEFAULT FALSE NOT NULL,
    created_at       TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_patients PRIMARY KEY (id)
);

CREATE TABLE refresh_tokens
(
    id          UUID         NOT NULL,
    user_id     UUID         NOT NULL,
    token       VARCHAR(512) NOT NULL,
    expiry_date TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id)
);

CREATE TABLE revchanges
(
    rev        BIGINT NOT NULL,
    entityname VARCHAR(255)
);

CREATE TABLE revinfo
(
    rev      BIGINT NOT NULL,
    revtstmp BIGINT,
    CONSTRAINT pk_revinfo PRIMARY KEY (rev)
);

CREATE TABLE rooms
(
    id          UUID         NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    active      BOOLEAN      NOT NULL,
    CONSTRAINT pk_rooms PRIMARY KEY (id)
);

CREATE TABLE service_required_equipment_types
(
    equipment_type_id UUID NOT NULL,
    service_id        UUID NOT NULL,
    CONSTRAINT pk_service_required_equipment_types PRIMARY KEY (equipment_type_id, service_id)
);

CREATE TABLE services
(
    id                       UUID           NOT NULL,
    name                     VARCHAR(150)   NOT NULL,
    price                    DECIMAL(10, 2) NOT NULL,
    default_duration_minutes INTEGER        NOT NULL,
    active                   BOOLEAN        NOT NULL,
    CONSTRAINT pk_services PRIMARY KEY (id)
);

CREATE TABLE users
(
    id            UUID         NOT NULL,
    username      VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    phone         VARCHAR(20),
    city          VARCHAR(100),
    role          VARCHAR(255) NOT NULL,
    enabled       BOOLEAN,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE TABLE work_schedules
(
    id          UUID         NOT NULL,
    doctor_id   UUID         NOT NULL,
    day_of_week VARCHAR(255) NOT NULL,
    start_time  time WITHOUT TIME ZONE NOT NULL,
    end_time    time WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_work_schedules PRIMARY KEY (id)
);

ALTER TABLE consultation_records
    ADD CONSTRAINT uc_consultation_records_appointment_id UNIQUE (appointment_id);

ALTER TABLE doctors
    ADD CONSTRAINT uc_doctors_user UNIQUE (user_id);

ALTER TABLE equipment_types
    ADD CONSTRAINT uc_equipment_types_name UNIQUE (name);

ALTER TABLE patients
    ADD CONSTRAINT uc_patients_cnp_hash UNIQUE (cnp_hash);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT uc_refresh_tokens_token UNIQUE (token);

ALTER TABLE rooms
    ADD CONSTRAINT uc_rooms_name UNIQUE (name);

ALTER TABLE services
    ADD CONSTRAINT uc_services_name UNIQUE (name);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uc_users_username UNIQUE (username);

CREATE INDEX idx_patients_profile_complete ON patients (profile_complete);

ALTER TABLE doctors
    ADD CONSTRAINT FK_DOCTORS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE equipments
    ADD CONSTRAINT FK_EQUIPMENTS_ON_EQUIPMENT_TYPE FOREIGN KEY (equipment_type_id) REFERENCES equipment_types (id);

ALTER TABLE equipments
    ADD CONSTRAINT FK_EQUIPMENTS_ON_ROOM FOREIGN KEY (room_id) REFERENCES rooms (id);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT FK_REFRESH_TOKENS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE work_schedules
    ADD CONSTRAINT FK_WORK_SCHEDULES_ON_DOCTOR FOREIGN KEY (doctor_id) REFERENCES doctors (id);

ALTER TABLE revchanges
    ADD CONSTRAINT fk_revchanges_on_default_tracking_modified_entities_changelog FOREIGN KEY (rev) REFERENCES revinfo (rev);

ALTER TABLE service_required_equipment_types
    ADD CONSTRAINT fk_serreqequtyp_on_equipment_type FOREIGN KEY (equipment_type_id) REFERENCES equipment_types (id);

ALTER TABLE service_required_equipment_types
    ADD CONSTRAINT fk_serreqequtyp_on_service FOREIGN KEY (service_id) REFERENCES services (id);