-- Fix: V1 created users.password instead of users.password_hash, which User.java has always
-- mapped to. The stray NOT NULL "password" column was never written by any code path
-- (login/register/DevDataSeeder all use passwordHash), so every insert into users failed.
ALTER TABLE users DROP COLUMN IF EXISTS password_hash;
ALTER TABLE users RENAME COLUMN password TO password_hash;
