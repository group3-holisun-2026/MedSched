-- Fix: V1 created users.password instead of users.password_hash, which User.java has always
-- mapped to. The stray NOT NULL "password" column was never written by any code path
-- (login/register/DevDataSeeder all use passwordHash), so every insert into users failed.
--
-- Bug gasit ulterior (Modulul 4, la testarea migratiei V1->V7 pe un lant Flyway curat, nu pe
-- ddl-auto:update): V3__.sql redenumeste deja "password" -> "password_hash" (linia 74-75 din
-- V3__.sql). Pe un lant curat, cand ajunge aici V6, coloana "password" nu mai exista deloc -- a
-- doua redenumire pica cu "column password does not exist" si opreste orice pornire cu
-- ddl-auto: validate (adica exact profilul de productie). Nimeni nu a observat pentru ca
-- niciun mediu local nu a rulat Flyway cu adevarat pana acum (toata lumea foloseste
-- ddl-auto: update, care ocoleste complet migratiile). Facem operatia idempotenta -- nu rescriem
-- V3, doar ne asiguram ca V6 nu (mai) strica lantul, indiferent de starea de la care porneste.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'users' AND column_name = 'password'
    ) THEN
        ALTER TABLE users DROP COLUMN IF EXISTS password_hash;
        ALTER TABLE users RENAME COLUMN password TO password_hash;
    END IF;
END $$;
