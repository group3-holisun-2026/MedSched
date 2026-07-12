CREATE SEQUENCE IF NOT EXISTS revinfo_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE doctors
(
    id                                     UUID         NOT NULL,
    user_id                                UUID,
    speciality                             VARCHAR(100) NOT NULL,
    standard_consultation_duration_minutes INTEGER      NOT NULL,
    weekly_schedule                        JSONB,
    CONSTRAINT pk_doctors PRIMARY KEY (id)
);

CREATE TABLE equipments
(
    id   UUID         NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_equipments PRIMARY KEY (id)
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
    id   UUID         NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_rooms PRIMARY KEY (id)
);

CREATE TABLE users
(
    id       UUID         NOT NULL,
    username VARCHAR(100) NOT NULL,
    email    VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone    VARCHAR(20),
    city     VARCHAR(100),
    role     VARCHAR(255) NOT NULL,
    enabled  BOOLEAN,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE TABLE working_hours
(
    id          BIGINT   NOT NULL,
    day_of_week SMALLINT NOT NULL,
    start_time  time WITHOUT TIME ZONE NOT NULL,
    end_time    time WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_working_hours PRIMARY KEY (id)
);

ALTER TABLE doctors
    ADD CONSTRAINT uc_doctors_user UNIQUE (user_id);

ALTER TABLE rooms
    ADD CONSTRAINT uc_rooms_name UNIQUE (name);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uc_users_username UNIQUE (username);

ALTER TABLE doctors
    ADD CONSTRAINT FK_DOCTORS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE doctors
    ADD CONSTRAINT FK_DOCTORS_ON_WEEKLY_SCHEDULE FOREIGN KEY (weekly_schedule) REFERENCES working_hours (id);

ALTER TABLE revchanges
    ADD CONSTRAINT fk_revchanges_on_default_tracking_modified_entities_changelog FOREIGN KEY (rev) REFERENCES revinfo (rev);