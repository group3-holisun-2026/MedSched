-- Modulul 5: tabela de outbox pentru notificari (F-501, F-502, NFR-2).
--
-- Canalul e email (decizie de echipa: Twilio ar fi fost peste nevoile demo-ului), dar coloana `type`
-- ramane pe enum-ul NotificationType (EMAIL/SMS) ca sa nu inchidem usa unui al doilea canal.
--
-- Nota pentru echipa, aceeasi capcana ca la V7__.sql: daca ai deja o baza `medsched_dev` locala pe
-- care ai rulat aplicatia cu `ddl-auto: update` inainte de aceasta migratie, Hibernate a creat deja
-- `notifications` fara sa treaca prin Flyway, iar migratia va esua cu "relation already exists".
-- Ruleaza `DROP TABLE notifications CASCADE;` sau reseteaza baza de dev inainte de a porni din nou.

CREATE TABLE notifications
(
    id                  UUID                        NOT NULL,
    appointment_id      UUID                        NOT NULL,
    type                VARCHAR(20)                 NOT NULL,
    notification_trigger VARCHAR(30)                NOT NULL,
    status              VARCHAR(20)                 NOT NULL,
    recipient_email     VARCHAR(255),
    subject             VARCHAR(255),
    body                TEXT,
    next_attempt_at     TIMESTAMP WITHOUT TIME ZONE,
    attempts            INTEGER                     NOT NULL DEFAULT 0,
    last_error          VARCHAR(500),
    sent_at             TIMESTAMP WITHOUT TIME ZONE,
    provider_message_id VARCHAR(64),
    confirmation_token  VARCHAR(32),
    created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id)
);

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_on_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments (id);

-- Token-ul din linkul de confirmare/anulare: unic, dar NULL pentru orice alt trigger decat
-- REMINDER_24H. In Postgres NULL-urile nu se ciocnesc intr-un UNIQUE, deci nu e nevoie de index
-- partial - mai multe randuri cu token NULL sunt acceptate.
ALTER TABLE notifications
    ADD CONSTRAINT uq_notifications_confirmation_token UNIQUE (confirmation_token);

-- Exact query-ul rulat de NotificationDispatcher la fiecare 15 secunde
-- (findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc).
CREATE INDEX idx_notifications_dispatch
    ON notifications (status, next_attempt_at);

-- Ecranul administrativ listeaza descrescator dupa createdAt, cu filtru optional pe programare.
CREATE INDEX idx_notifications_appointment
    ON notifications (appointment_id, created_at DESC);

-- Modulul 6: toate cele trei rapoarte filtreaza pe start_time.
CREATE INDEX idx_appointments_start_time
    ON appointments (start_time);
