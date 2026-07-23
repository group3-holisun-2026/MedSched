-- Restanta de la Modulul 3: tabela `appointments` nu a fost niciodata scrisa ca migratie Flyway
-- (a mers doar pe `ddl-auto: update` in dev), iar cele 3 constrangeri EXCLUDE promise in contractul
-- original (plasa de siguranta anti-coliziune la nivel de DB, sub concurenta reala) nu au fost scrise
-- deloc. Verificat empiric (Modulul 4): fara EXCLUDE, doua cereri concurente pe aceeasi camera/medic/
-- echipament trec AMANDOUA de verificarea Java (AvailabilityValidatorService), pentru ca cele doua
-- SELECT-uri de overlap ruleaza inainte ca oricare INSERT sa fi facut commit sub izolare READ_COMMITTED.
--
-- Nota pentru echipa: daca ai deja o baza `medsched_dev` locala pe care ai rulat aplicatia cu
-- `ddl-auto: update` inainte de aceasta migratie, Hibernate a creat deja `appointments` fara sa treaca
-- prin Flyway -- migratia de mai jos va esua cu "relation already exists". Sterge tabela
-- (`DROP TABLE appointments CASCADE;`) sau reseteaza baza de dev inainte de a porni aplicatia din nou,
-- la fel ca la orice alta migratie retroactiva din acest proiect.

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE appointments
(
    id           UUID                        NOT NULL,
    patient_id   UUID                        NOT NULL,
    doctor_id    UUID                        NOT NULL,
    room_id      UUID                        NOT NULL,
    service_id   UUID                        NOT NULL,
    equipment_id UUID,
    start_time   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    end_time     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status       VARCHAR(20)                 NOT NULL,
    notes        TEXT,
    version      BIGINT                      NOT NULL DEFAULT 0,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_appointments PRIMARY KEY (id)
);

ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_on_patient FOREIGN KEY (patient_id) REFERENCES patients (id);

ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_on_doctor FOREIGN KEY (doctor_id) REFERENCES doctors (id);

ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_on_room FOREIGN KEY (room_id) REFERENCES rooms (id);

ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_on_service FOREIGN KEY (service_id) REFERENCES services (id);

ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_on_equipment FOREIGN KEY (equipment_id) REFERENCES equipments (id);

-- Garantia "matematica" anti-coliziune (NFR-2): chiar daca doua tranzactii trec amandoua de
-- verificarea Java in aceeasi fereastra de timp, Postgres respinge a doua la commit.
-- Programarile CANCELLED/NO_SHOW nu mai ocupa resursa, deci sunt excluse din verificare
-- (acelasi filtru folosit deja de AppointmentRepository.findOverlappingFor*).

ALTER TABLE appointments
    ADD CONSTRAINT excl_appointments_doctor_overlap
        EXCLUDE USING gist (doctor_id WITH =, tsrange(start_time, end_time) WITH &&)
        WHERE (status NOT IN ('CANCELLED', 'NO_SHOW'));

ALTER TABLE appointments
    ADD CONSTRAINT excl_appointments_room_overlap
        EXCLUDE USING gist (room_id WITH =, tsrange(start_time, end_time) WITH &&)
        WHERE (status NOT IN ('CANCELLED', 'NO_SHOW'));

ALTER TABLE appointments
    ADD CONSTRAINT excl_appointments_equipment_overlap
        EXCLUDE USING gist (equipment_id WITH =, tsrange(start_time, end_time) WITH &&)
        WHERE (equipment_id IS NOT NULL AND status NOT IN ('CANCELLED', 'NO_SHOW'));
