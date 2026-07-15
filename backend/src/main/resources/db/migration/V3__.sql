-- V3: net-new tables for F-102/F-103, plus corrections to the F-101 tables
-- V2 already created. Nothing here re-creates a table that already exists.

CREATE TABLE equipment_types
(
    id          UUID         NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT pk_equipment_types PRIMARY KEY (id)
);

ALTER TABLE equipment_types
    ADD CONSTRAINT uc_equipment_types_name UNIQUE (name);

CREATE TABLE services
(
    id                       UUID           NOT NULL,
    name                     VARCHAR(150)   NOT NULL,
    price                    DECIMAL(10, 2) NOT NULL,
    default_duration_minutes INTEGER        NOT NULL,
    active                   BOOLEAN        NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_services PRIMARY KEY (id)
);

ALTER TABLE services
    ADD CONSTRAINT uc_services_name UNIQUE (name);

CREATE TABLE service_required_equipment_types
(
    equipment_type_id UUID NOT NULL,
    service_id        UUID NOT NULL,
    CONSTRAINT pk_service_required_equipment_types PRIMARY KEY (equipment_type_id, service_id)
);

ALTER TABLE service_required_equipment_types
    ADD CONSTRAINT fk_serreqequtyp_on_equipment_type FOREIGN KEY (equipment_type_id) REFERENCES equipment_types (id);

ALTER TABLE service_required_equipment_types
    ADD CONSTRAINT fk_serreqequtyp_on_service FOREIGN KEY (service_id) REFERENCES services (id);

-- equipments: V2 only created id/name; Equipment entity needs type/room/active
ALTER TABLE equipments
    ADD COLUMN equipment_type_id UUID,
    ADD COLUMN room_id           UUID,
    ADD COLUMN active            BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE equipments
    ALTER COLUMN equipment_type_id SET NOT NULL;

ALTER TABLE equipments
    ADD CONSTRAINT FK_EQUIPMENTS_ON_EQUIPMENT_TYPE FOREIGN KEY (equipment_type_id) REFERENCES equipment_types (id);

ALTER TABLE equipments
    ADD CONSTRAINT FK_EQUIPMENTS_ON_ROOM FOREIGN KEY (room_id) REFERENCES rooms (id);

-- rooms: V2 only created id/name; Room entity needs description/active
ALTER TABLE rooms
    ADD COLUMN description VARCHAR(255),
    ADD COLUMN active      BOOLEAN NOT NULL DEFAULT TRUE;

-- doctors: V2 created it without `active`, and with a nullable user_id
ALTER TABLE doctors
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE doctors
    ALTER COLUMN user_id SET NOT NULL;

-- Doctor.speciality is annotated length = 150, V2's column was VARCHAR(100)
ALTER TABLE doctors
ALTER COLUMN speciality TYPE VARCHAR(150);

-- users.password was created as "password" in V1; User entity maps
-- passwordHash -> "password_hash"
ALTER TABLE users
    RENAME COLUMN password TO password_hash;