-- B7 (era optional in caietul Modulului 6): pretul se ingheata la momentul programarii.
--
-- Pana acum raportul de vanzari insuma `services.price`, adica pretul de AZI al serviciului.
-- Consecinta: daca adminul urca pretul unui consult, cifrele lunilor deja incheiate se schimbau
-- retroactiv — un raport financiar care nu e stabil in timp nu e un raport financiar.
--
-- Coloana se adauga nullable, se completeaza pentru istoricul existent din pretul curent al
-- serviciului (singura valoare pe care o mai avem — pentru randurile vechi ea devine, prin
-- definitie, "pretul la momentul programarii") si abia apoi devine NOT NULL.

ALTER TABLE appointments
    ADD price_at_booking NUMERIC(10, 2);

UPDATE appointments a
SET price_at_booking = s.price
FROM services s
WHERE a.service_id = s.id
  AND a.price_at_booking IS NULL;

ALTER TABLE appointments
    ALTER COLUMN price_at_booking SET NOT NULL;
