# Modulele 5 + 6 — stare finala dupa integrare

> Actualizat 02.08.2026, dupa merge-ul PR-urilor #154-#160, runda de reparatii care a urmat si
> inchiderea celor doua restante deschise (B7 si filtrul de calendar) — vezi §4.
> Versiunea initiala a acestui document era o lista de restante; toate cele functionale sunt
> rezolvate acum, asa ca a fost rescris ca stare finala. Ce a ramas neterminat, constient, e la §5.
>
> **Canalul de notificare e emailul.** Twilio/SMS a iesit din scop (decizie de echipa: un cont SMS
> platit ar fi fost peste nevoile demo-ului). F-501, F-502 si NFR-2 sunt acoperite integral pe email.
> De semnalat la prezentare ca abatere constienta fata de caiet, impreuna cu omiterea
> "instructiunilor speciale" din corpul mesajului (motiv de confidentialitate, F-502.1).

## 1. Verificat pe aplicatia pornita, nu doar compilat

Backend: 97 de teste trec (94 + cele 3 din `AppointmentPriceAtBookingTest`). Frontend: `vite build`
curat. Aplicatia a fost pornita pe o baza goala, cu Flyway rulandu-si toate cele 10 migratii, si
fiecare modul a fost exersat prin API si prin UI.

| Cerinta | Cum a fost verificata | Rezultat |
|---|---|---|
| F-101/102/103 | `GET /api/doctors`, `/rooms`, `/equipment`, `/services` | 8 medici, 10 cabinete, 11 servicii |
| F-201 | `GET /api/patients` | 10 pacienti |
| F-202 + DoD #3 | `POST`/`GET /api/appointments/{id}/record`; acelasi GET ca RECEPTION | 201/200; receptia **403** |
| F-301/302 | creare programare din UI si API | 201 |
| DoD #4 | a doua rezervare pe acelasi medic/interval | **409** "Medicul are deja o programare" |
| F-401 + DoD #2 | `confirm` → `check-in` → `complete`; plus doua tranzitii ilegale | legale 200, ilegale **409** |
| F-402 | fisa devine read-only la `COMPLETED` | acoperit de suita existenta |
| F-501/502 | creare → 2 randuri in outbox; dispatcher trimite confirmarea in <15s | CONFIRMATION `SENT`, REMINDER_24H `PENDING` la `startTime - 24h` |
| F-502.2 | link din email → pagina publica → Confirm | 200 → 204, mesaj de confirmare pe ecran |
| F-502.3 | reprogramare si anulare | reminder vechi `CANCELLED`, unul nou pentru ora noua, notificare `RESCHEDULED`/`CANCELLED` |
| NFR-2 | `SmsTransientException` → ramane `PENDING` cu backoff; a doua rulare → `SENT` | test dedicat |
| F-601.1/2/3 | cele trei rapoarte, din UI | cifre reale, formatare RON, bare de procent |
| F-602 | 6 exporturi (3 rapoarte × pdf/xlsx) | 200, `%PDF` / `PK`, numere native in Excel, format `0.0%` |
| NFR-1 | `DOCTOR` si `RECEPTION` pe `/api/reports/*` si `/api/notifications` | **403** peste tot; ADMIN 200 |
| NFR-1 audit | citire fisa → intrare in `audit_log` atribuita userului real | 1 intrare, `READ ConsultationRecord` |
| NFR-3 | 360px si 768px pe paginile noi | zero scroll orizontal |

## 2. Trei probleme de fond, gasite la verificare

**Flyway nu rulase niciodata, nicaieri.** Lipseau `spring-boot-flyway` (in Boot 4 autoconfiguratiile
sunt module separate, `flyway-core` singur nu porneste nimic) si `flyway-database-postgresql`.
Simptomul e tacut: nicio linie de log, iar schema exista oricum pentru ca o crea `ddl-auto: update`.
Practic V1-V8 erau fisiere moarte. Acum profilul dev ruleaza pe `ddl-auto: validate` si Flyway
detine schema.

> **O singura data, pentru fiecare:** daca ai deja o baza `medsched_dev` construita de Hibernate,
> Flyway va esua cu "relation already exists". Reseteaz-o:
> `DROP DATABASE medsched_dev; CREATE DATABASE medsched_dev;`

**Auditul nu retinea cine.** `AuditLoggingAspect` citea userul din `getCredentials()`, dar
`JwtAuthenticationFilter` il pune ca *principal*. Toate intrarile se scriau pe UUID-ul zero — adica
exact intrebarea pe care NFR-1 o pune ("cine a deschis fisa pacientului X") ramanea fara raspuns.

**Suita de teste era nedeterminista.** `@EnableScheduling` statea pe `BackendApplication`, deci
dispatcher-ul real pornea si in `@SpringBootTest` si se bata pe aceleasi mock-uri cu apelul manual
din test — `dispatch_permanentException_failsImmediately` pica la o rulare din doua. In plus, trei
clase de test rulau pe profilul `dev`, adica pe baza de dezvoltare a fiecaruia.

## 3. Ce s-a reparat, pe scurt

Securitate: `@PreAuthorize` pe coada de notificari (expunea corpul mesajelor si adresele pacientilor
oricui era logat); `AuditLogService` era un stub care arunca 500; atributia userului in audit.

Modulul 5: reprogramarea anula reminderul fara sa puna altul (pacientul ramanea fara reamintire);
linkul din email ducea pe o ruta inexistenta; frontend-ul trimitea `PATCH` unde backend-ul asteapta
`POST` (405); pagina pacientului ramanea alba pentru ca citea campuri care nu exista in DTO;
sectiunea din calendar era text fix, acum citeste notificarile reale si doar pentru ADMIN.

Modulul 6: exportul PDF/Excel exista complet in backend dar nu-l chema nimeni — implementat in
frontend cu toate cele trei capcane (eroare citita din `Blob`, nume de fisier construit local pentru
ca `Content-Disposition` nu e expus prin CORS, `revokeObjectURL`); raportul de ocupare rula pe date
inventate; nume de campuri aliniate la DTO-uri peste tot.

Cauza comuna a majoritatii: **`module5_6_endpoints.md` nu fusese publicat**. Exista acum si e sursa
de adevar — daca schimbati un camp, schimbati-l intai acolo.

## 4. Cele doua restante deschise, acum inchise

**`priceAtBooking` (B7).** Implementat. `appointments` are coloana `price_at_booking` (migratia
`V10__.sql`: se adauga nullable, se completeaza istoricul din pretul curent al serviciului — singura
valoare pe care o mai aveam — si abia apoi devine `NOT NULL`). `AppointmentService.create()` o
fotografiaza din serviciu, iar cele doua interogari de vanzari din `ReportRepository` insumeaza
`a.priceAtBooking` in loc de `SUM(s.price)`. La reprogramare pretul **se pastreaza** daca serviciul
ramane acelasi (pacientul a acceptat tariful de atunci) si se refotografiaza doar daca se schimba
serviciul. Trei teste noi in `AppointmentPriceAtBookingTest` fixeaza exact aceste trei reguli.
Nu mai e nimic de semnalat la prezentare pe tema asta.

**Filtrarea pe medic din calendar (PR #159).** Reintegrata din `main` curat, fara `CalendarPage.jsx`
de pe branch-ul original (cel cu `} finally {` fara `try` si fara blocul de `return`). Ce s-a
schimbat efectiv in `pages/Calendar/CalendarPage.jsx`:

- `DoctorFilterMenu` e randat langa titlu si e singura sursa a filtrului; pana raporteaza prima data,
  pagina nu cere programari, ca sa nu plece doua cereri la fiecare montare.
- `doctorIds`/`roomId` merg mai departe la `appointmentApi.getCalendarAppointments`.
- **Capcana**: backendul trateaza lista goala de `doctorIds` ca "toti medicii activi", deci
  "deselecteaza tot" nu trimite cererea deloc — altfel utilizatorul primea exact opusul a ce a cerut.
  Calendarul ramane gol, cu mesaj explicit.
- Culorile din `doctorColors.js` sunt o dunga in stanga blocului, nu fundalul: fundalul ramane
  statusul, ca sa nu se piarda informatia pe care se iau deciziile. Aceleasi culori ca punctele din
  filtru; nota de legenda apare doar la ADMIN/RECEPTION.
- `DoctorResponse` a primit campul `active` — `GET /api/doctors` intoarce si medicii dezactivati,
  deci filtrul "doar medici activi" era pana acum un no-op cu un avertisment in consola.

## 5. Ce a ramas neterminat (constient)

1. **Multi-instanta.** Dispatcher-ul nu are lock distribuit: cu doua instante ale aplicatiei aceeasi
   notificare poate pleca de doua ori. Limitare documentata, nu o rezolvam acum.
2. **Diacritice in PDF.** Fontul implicit (Helvetica/Cp1252) nu reda `ș`/`ț`, deci textele din PDF si
   din emailuri sunt fara diacritice. Ar necesita un TTF inglobat — task separat.

## 6. Doua capcane gasite pe drum

**`DevDataSeederIntegrationTest` rula pe baza de dezvoltare, nu pe cea de test.** Testul e
`@ActiveProfiles({"test", "dev"})` — profilul `dev` e ultimul, deci `application-dev.yml` suprascria
si `spring.datasource.url`, si `ddl-auto`. Testul deschidea `medsched_dev`, ii **stergea si recrea
programarile**, si o valida cu `validate` desi acolo schema e detinuta de Flyway, care in profilul de
test e oprit. Efectul: prima coloana noua pe orice entitate pica in acest test, cu un mesaj care nu
are nimic de-a face cu seeder-ul. Acum conexiunea si `ddl-auto` sunt fixate explicit pe test.

**Baza de dev trebuie resetata o singura data, si acum chiar trebuie.** Verificarea s-a facut pe o
baza noua (`medsched_verify`), construita de Flyway de la zero: toate cele 10 migratii, inclusiv
`V10__.sql`, se aplica curat, iar `ddl-auto: validate` trece. Pe `medsched_dev` de pe masina de
dezvoltare Flyway inca pica la V2 cu "relation work_schedules already exists" — e baza construita
candva de Hibernate, exact cazul din nota de la §2. `DROP DATABASE medsched_dev; CREATE DATABASE
medsched_dev;` si aplicatia porneste singura mai departe.
