CREATE SEQUENCE IF NOT EXISTS revinfo_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE work_schedules
(
    id          UUID                   NOT NULL,
    doctor_id   UUID                   NOT NULL,
    day_of_week VARCHAR(20)            NOT NULL,
    start_time  time WITHOUT TIME ZONE NOT NULL,
    end_time    time WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_work_schedules PRIMARY KEY (id)
);

CREATE TABLE doctors
(
    id                                     UUID         NOT NULL,
    user_id                                UUID,
    speciality                             VARCHAR(100) NOT NULL,
    standard_consultation_duration_minutes INTEGER      NOT NULL,
    CONSTRAINT pk_doctors PRIMARY KEY (id)
);

CREATE TABLE equipments
(
    id   UUID         NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_equipments PRIMARY KEY (id)
);

CREATE TABLE rooms
(
    id   UUID         NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_rooms PRIMARY KEY (id)
);

ALTER TABLE doctors
    ADD CONSTRAINT uc_doctors_user UNIQUE (user_id);

ALTER TABLE rooms
    ADD CONSTRAINT uc_rooms_name UNIQUE (name);

ALTER TABLE doctors
    ADD CONSTRAINT FK_DOCTORS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE work_schedules
    ADD CONSTRAINT fk_work_schedules_on_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id);

ALTER TABLE work_schedules
    ADD CONSTRAINT uc_doctor_day_start UNIQUE (doctor_id, day_of_week, start_time);

ALTER TABLE work_schedules
    ADD CONSTRAINT chk_work_schedule_time_order CHECK (end_time > start_time);