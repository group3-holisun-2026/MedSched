# Modulele 5 + 6 — Notificări SMS (Twilio) & Rapoarte + Export — Task-uri Backend

> Scop: **F-501/F-502** (notificări automate — la noi **SMS prin Twilio**, nu email) + **F-601/F-602**
> (rapoarte de utilizare/no-show/vânzări + export PDF și Excel) + bucata din **NFR-2** care ține de
> disponibilitatea serviciului de notificări (retrimitere automată după o cădere) + bucata din **NFR-1**
> care ține de rapoarte (medicul nu are acces la datele financiare).
>
> **Convenție de sprint — se schimbă față de Modulele 1-4:** au mai rămas **2 oameni pe backend**, deci
> nu mai împărțim pe "Persoana 1...Persoana 5". Mai jos sunt **două loturi** (Lotul A = Modulul 5,
> Lotul B = Modulul 6) și, în interiorul lor, task-uri numerotate. Împărțiți-le voi doi în grup —
> recomandarea naturală e **un om pe lot**, pentru că cele două loturi nu se ating deloc la nivel de
> fișiere (singura excepție: `pom.xml`, migrația SQL și `application.yml`, listate explicit la §4 ca să
> nu vă călcați pe ele).
>
> **Nu intră aici:** WebSocket/real-time (rămânem la polling, decizie din Modulul 3), alocarea de
> echipament (deja rezolvată în Modulul 3, `EquipmentAllocationService` există și e apelat), detecția
> automată de no-show (rămâne acțiune manuală de recepție, decizie din Modulul 4).

---

## 0. Status curent al codebase-ului (verificat direct în cod, nu presupus) — CITIȚI ÎNAINTE

Vestea bună, față de modulele anterioare: **de data asta `main` e la zi**. Modulele 1-4 sunt toate
merge-uite (`AppointmentController`, `AppointmentStateMachine`, `AvailabilityValidatorService`,
`EquipmentAllocationService`, `ConsultationRecordLockScheduler`, migrațiile `V1`-`V8`). Porniți branch-uri
noi din `main`, nu din vreun branch de modul vechi.

Ce e relevant pentru acest sprint, verificat concret:

1. **Există deja două enum-uri de notificări, complet nefolosite** — `enums/NotificationType.java`
   (`EMAIL`, `SMS`) și `enums/NotificationStatus.java` (`PENDING`, `SENT`, `FAILED`). Zero referințe la
   ele în tot codul (verificat cu grep pe `backend/src/main/java`). Sunt schelet lăsat de la Modulul 1 —
   **le refolosim, nu creăm enum-uri paralele**. Valoarea `EMAIL` rămâne în enum, dar nu scriem niciun
   fel de cod de email (vezi decizia 1.1).
2. **Zero infrastructură de notificări**: nicio entitate `Notification`, nicio tabelă, niciun
   `spring-boot-starter-mail`, niciun `@Async`/`@EnableAsync` în proiect.
3. **`@EnableScheduling` există deja** pe `BackendApplication` (adăugat în Modulul 4) și avem deja un
   exemplu funcțional de job periodic — `service/ConsultationRecordLockScheduler.java`. Dispatcher-ul de
   SMS-uri se scrie **în același tipar**, nu inventați altul.
4. **Zero cod de rapoarte**: niciun fișier care conține "report"/"Report" în `backend/src/main/java` sau
   în `frontend/src`. Pornim de la zero pe Modulul 6.
5. **`pom.xml` nu are nici Twilio, nici Apache POI, nici vreo librărie de PDF.** Toate trei se adaugă în
   acest sprint (vezi §4.1).
6. **`SecurityConfig` permite deja `/public/**` fără JWT** (`.requestMatchers("/api/auth/register",
   "/api/auth/login", "/api/auth/refresh", "/public/**").permitAll()`). Link-ul unic din SMS-ul de
   reminder (F-502.2) intră exact acolo — **nu modificați `SecurityConfig`**, nu e nevoie.
7. **`patients.phone` e `nullable = false`, `length = 20`, necriptat** (spre deosebire de `cnp`, care trece
   prin `AesEncryptionUtil`). Deci fiecare pacient are garantat un telefon — nu trebuie să tratăm cazul
   "pacient fără număr" la nivel de model, doar cazul "număr invalid" (vezi 1.5).
   Numerele din `DevDataSeeder` sunt fictive (`0721111111`, `0722222222`, ...) — **niciunul nu e un număr
   real și niciunul nu e verificat în contul Twilio trial**, deci vor eșua toate la trimitere. Vezi 1.6.
8. **Numele medicului nu există ca atare pe `Doctor`** — se ia din `doctor.user.username` (exact ce face
   `CalendarMapper` azi: `@Mapping(target = "doctorName", source = "doctor.user.username")`). Folosiți
   aceeași sursă și în SMS-uri, și în rapoarte, ca să nu apară două nume diferite pentru același medic.
9. **`Service` are `price` (`BigDecimal`, `precision = 10, scale = 2`)**, dar prețul **nu e copiat pe
   `Appointment`** la creare. Consecință reală pentru raportul de vânzări — vezi decizia 1.9.
10. **`WorkSchedule`** (`doctor_id`, `day_of_week` ca `DayOfWeek` STRING, `start_time`, `end_time`) e
    sursa pentru numitorul ratei de ocupare a medicilor. Un medic poate avea mai multe rânduri pe aceeași
    zi — **nu presupuneți unul singur**, faceți sumă.
11. **Ultima migrație e `V8__.sql`.** Migrația acestui sprint e `V9__.sql` (una singură, comună celor
    două loturi — vezi §4.2).
12. **`GlobalExceptionHandler` mapează `ResponseStatusException` la `ErrorResponse`** (prin
    `ResponseEntityExceptionHandler`), deci frontend-ul citește `error.response.data.message`. Folosiți
    `ResponseStatusException`, la fel ca `AppointmentService` — nu introduceți excepții custom noi.

---

## 1. Decizii de business fixate acum (ca să nu discutăm variante diferite în paralel)

### 1.1. SMS în loc de email — peste tot, fără excepție

Caietul (F-501/F-502) zice "email și SMS". **Decizia echipei: implementăm doar SMS, prin Twilio.** Nu
scriem `spring-boot-starter-mail`, nu scriem template-uri de email, nu adăugăm câmpuri de email nicăieri.
Enum-ul `NotificationType` rămâne cu ambele valori (nu costă nimic), dar în cod se scrie mereu
`NotificationType.SMS`.

**De semnalat clientului la prezentare:** `Patient.email` există și e populat, dar nu e folosit pentru
notificări — e o abatere conștientă față de caiet, nu o scăpare.

### 1.2. Arhitectură: tabelă de outbox + un singur job periodic (rezolvă F-501 și NFR-2 dintr-o mișcare)

**Nu folosim `@Async`.** Motivul concret: F-501 cere trimitere non-blocantă, iar NFR-2 cere ca
notificările eșuate să fie **stocate și retrimise automat când serviciul revine**. Cu `@Async` ai doar
prima parte — dacă Twilio e picat, thread-ul moare cu o excepție și notificarea se pierde definitiv. Cu o
tabelă de outbox le obții pe amândouă cu un singur mecanism:

1. Când se creează/mută/anulează o programare, `AppointmentService` **doar inserează un rând `PENDING`**
   în `notifications`, **în aceeași tranzacție** cu programarea. Insert local în Postgres = câteva
   milisecunde, deci răspunsul API rămâne rapid (F-501 bifat) și nu putem ajunge în situația "am trimis
   SMS pentru o programare care apoi a dat rollback".
2. Un `@Scheduled` (`NotificationDispatcher`) ia periodic rândurile scadente și le trimite. Dacă Twilio
   dă eroare temporară, rândul rămâne `PENDING` cu `nextAttemptAt` împins în viitor și se reia automat
   (NFR-2 bifat).

`@Scheduled` rulează implicit pe **un singur thread** în Spring, iar cu `fixedDelay` (nu `fixedRate`) o
rundă lentă nu se suprapune peste următoarea — deci **nu avem nevoie de lock distribuit / coloană
`SENDING` / `@Version` pe notificare**. Asta e valabil pentru o singură instanță de aplicație, ceea ce e
cazul nostru; dacă vreodată rulează două instanțe, aceeași notificare poate fi trimisă de două ori —
scriem asta explicit ca limitare cunoscută, nu o rezolvăm acum.

### 1.3. Cele 4 declanșatoare (F-502) și momentul lor

| Trigger | Când se inserează rândul | `nextAttemptAt` inițial |
|---|---|---|
| `CONFIRMATION` | în `AppointmentService.create()` | `now()` (pleacă la următoarea rundă a dispatcher-ului) |
| `REMINDER_24H` | în `AppointmentService.create()` | `startTime - 24h` |
| `RESCHEDULED` | în `AppointmentService.update()` | `now()` |
| `CANCELLED` | în `AppointmentService.cancel()` | `now()` |

Reguli suplimentare, fixate:

- **Programare făcută la mai puțin de 24h distanță:** `startTime - 24h` e deja în trecut → **nu inserăm
  deloc rândul de reminder**. Caietul cere "cu exact 24 de ore înainte"; a trimite un "reminder" la 3
  minute după confirmare e zgomot inutil și cost pe cont trial.
- **La reprogramare (`update`)**: reminder-ul vechi (rândul `PENDING` de tip `REMINDER_24H` al acelei
  programări) trece în `CANCELLED` și se inserează unul nou pentru noua oră. Altfel pacientul primește
  reminder pentru ora veche.
- **La anulare (`cancel`)**: reminder-ul `PENDING` trece în `CANCELLED` (nu se șterge — vrem urma în
  tabelă pentru raport/debug).
- **Tranzițiile de status din Modulul 4** (`confirm`, `check-in`, `no-show`, `complete`) **NU generează
  SMS.** F-502 listează exact trei situații (creare, reminder, modificare/anulare) — nu adăugăm
  notificări speculative peste ele. Un SMS de "consultul tău tocmai s-a terminat" nu e cerut de nimeni.
- **"Un medic devine indisponibil"** (F-502.3) nu are un flux propriu în aplicație — clinica reacționează
  mutând sau anulând programările, iar asta declanșează deja `RESCHEDULED`/`CANCELLED`. Nu construim un
  ecran separat de "dezactivare medic cu cascadă".

### 1.4. Ce scrie în SMS — și ce NU scrie (NFR-1, confidențialitate)

**Textele nu conțin diacritice.** Nu e o toană stilistică: SMS-urile fără diacritice intră în alfabetul
GSM-7 (160 caractere pe segment); cu un singur `ă` mesajul comută pe UCS-2 și segmentul scade la 70 de
caractere, deci același text costă de 2-3 ori mai mult și pe cont trial se consumă rapid.

**Nu punem în SMS:** diagnostic, anamneză, nimic din fișa de consultație, CNP, numele serviciului
(`Ecografie Abdominala` într-un SMS ajuns la telefonul greșit e exact scurgerea de informație pe care
persona Dr. Simona o reclamă), și **nici câmpul `notes` al programării** — e text liber scris de recepție,
fără nicio garanție că nu conține informație clinică, iar SMS-ul e un canal necriptat.

> Caietul (F-502.1) cere ca la creare să se trimită "Dată, Oră, Medic, Cabinet, **Instrucțiuni speciale**".
> Noi **omitem intenționat instrucțiunile speciale** din SMS, din motivul de mai sus. E o abatere
> conștientă de la specificație — **treceți-o pe lista de discutat la prezentare**, nu o ascundeți.

Template-urile (clasă `NotificationMessageFactory`, un singur loc, nu string-uri împrăștiate):

```
CONFIRMATION  : MedSched: programare confirmata pentru {dd.MM.yyyy}, ora {HH:mm}, Dr. {username}, {cabinet}. Detalii la {telefon_clinica}.
REMINDER_24H  : MedSched: maine {dd.MM} ora {HH:mm}, Dr. {username}, {cabinet}. Confirmati sau anulati: {link}
RESCHEDULED   : MedSched: programarea a fost mutata pe {dd.MM.yyyy}, ora {HH:mm}, Dr. {username}, {cabinet}.
CANCELLED     : MedSched: programarea din {dd.MM.yyyy}, ora {HH:mm} a fost anulata. Sunati la {telefon_clinica} pentru reprogramare.
```

`{telefon_clinica}` vine din config (`app.notifications.clinic-phone`), nu hardcodat.
Textul final se salvează în coloana `body` **exact așa cum a fost trimis** — nu se regenerează la
retrimitere (dacă între timp cineva a mutat programarea, retrimiterea unui reminder vechi cu ora nouă ar
fi derutantă; oricum, la mutare anulăm reminder-ul vechi, vezi 1.3).

### 1.5. Numere de telefon: normalizare la E.164

Twilio acceptă doar formatul E.164 (`+40721111111`). În baza noastră numerele sunt românești în format
local (`0721111111`). Nu se rezolvă cu un `"+4" + phone` aruncat în mijlocul dispatcher-ului — scrieți un
`util/PhoneNumberNormalizer.java` cu o singură metodă publică și teste unitare:

- elimină spații, `-`, `(`, `)`, `.`
- dacă începe cu `+` → îl lasă așa
- `00...` → `+...`
- `07XXXXXXXX` (10 cifre) → `+407XXXXXXXX`
- `40...` (11 cifre) → `+40...`
- orice altceva → aruncă `IllegalArgumentException` → notificarea e marcată direct `FAILED`
  (**fără retry** — un număr invalid nu devine valid dacă mai încerci de 5 ori)

Normalizarea se face **o singură dată, la inserarea în outbox**, și se salvează rezultatul în
`recipient_phone` — nu la fiecare încercare de trimitere.

### 1.6. Twilio: cont trial, deci reguli stricte

Contul e **trial** — poate trimite **doar către numere verificate în consola Twilio**. Consecințe pe care
le tratăm în cod, nu prin surprindere la demo:

- **Codul de eroare de la Twilio decide dacă se reîncearcă sau nu.** Împărțim în două categorii:
  - **permanent** (nu reîncercăm, marcăm `FAILED` imediat): număr invalid, număr neverificat pe trial,
    destinatar dezabonat. Exemple de coduri Twilio: `21211`, `21608`, `21610` — **verificați lista în
    documentația Twilio la implementare**, nu vă bazați doar pe ce scrie aici.
  - **temporar** (rămâne `PENDING`, se reîncearcă): timeout de rețea, `IOException`, HTTP 429, HTTP 5xx.
  Ăsta e exact scenariul din NFR-2 și e **singurul lucru pe care testul de NFR-2 chiar îl demonstrează** —
  vezi task-ul A7.
- **Kill switch obligatoriu:** `app.sms.enabled` (default `false`). Cu `false`, se injectează un
  `LoggingSmsSender` care doar scrie în log și întoarce un id fals (`LOG-<uuid>`) — **toată echipa poate
  rula aplicația și toate testele fără credențiale și fără să consume creditul trial**. Doar cine face
  demo-ul pornește cu `true`.
- **Credențialele NU intră niciodată în git.** Nici în `application.yml`, nici în `application-dev.yml`,
  nici într-un commit "temporar". Doar variabile de mediu (`TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`,
  `TWILIO_FROM_NUMBER`). Un token Twilio ajuns în istoricul git înseamnă rotirea lui imediată + curățare
  de istoric — nu e o formalitate.
- Pentru demo: modificați telefonul **unui singur pacient de test** (din UI, nu din seeder) cu numărul
  vostru verificat. Nu schimbați `DevDataSeeder` — ar pune numărul cuiva în repo.

### 1.7. Link-ul unic de confirmare/anulare (F-502.2)

- Token: **16 caractere URL-safe**, generat cu `SecureRandom` (12 bytes → Base64 URL-safe fără padding).
  Nu `UUID.toString()` — cratimele plus 36 de caractere mănâncă din cele 160 ale SMS-ului.
  96 de biți de entropie ≈ imposibil de ghicit prin forță brută, deci **nu adăugăm rate limiting** pe
  endpoint-ul public (ar fi muncă în plus fără câștig real aici).
- Token-ul stă pe rândul de notificare (`notifications.confirmation_token`, `UNIQUE`), populat doar pentru
  `REMINDER_24H`. Restul tipurilor au `NULL`.
- Trei endpoint-uri publice, sub `/public/**` (deja permis în `SecurityConfig`):
  `GET /public/appointments/{token}`, `POST /public/appointments/{token}/confirm`,
  `POST /public/appointments/{token}/cancel`.
- **Token-ul NU e single-use.** Rămâne valabil până la `appointment.startTime`; după acel moment →
  **410 Gone**. Motivul: un pacient care a confirmat poate vrea să anuleze o oră mai târziu, cu același
  SMS în telefon. Dubla confirmare e respinsă oricum de mașina de stări (409), nu are nevoie de un
  mecanism separat.
- Confirmarea/anularea trec **prin `AppointmentStateMachine`**, exact ca acțiunile de la recepție. Nu se
  scrie `setStatus()` direct — asta ar ocoli fix regula pe care DoD-ul (criteriul #2) cere să nu o poți
  ocoli.
- **Ce întoarce `GET /public/appointments/{token}`:** doar `startTime`, `endTime`, numele medicului,
  numele cabinetului, statusul, prenumele pacientului și două flag-uri `canConfirm`/`canCancel`.
  **Fără** nume de familie, CNP, telefon, email, serviciu, notițe, id-uri interne. E o pagină accesibilă
  fără autentificare — orice câmp în plus e o scurgere.
- Nu puneți `@Audited` pe endpoint-urile publice: `AuditLoggingAspect` citește userul din
  `SecurityContextHolder`, care pentru un request anonim e gol.

### 1.8. Rapoarte: cine are voie (NFR-1 + DoD criteriul #3)

**Toate cele trei rapoarte și toate exporturile sunt `ADMIN`-only.** Caietul e explicit: medicul "nu are
acces la rapoartele financiare ale clinicii", iar DoD-ul cere demonstrat că "un medic nu poate accesa
datele financiare administrative". Cel mai simplu de implementat și de demonstrat e o singură regulă
pentru tot controller-ul: `@PreAuthorize("hasRole('ADMIN')")` la nivel de clasă pe `ReportController`.

Dacă recepția va avea nevoie de raportul de no-show, e o modificare de o linie mai târziu — nu o facem
speculativ acum.

### 1.9. Ce se numără în rapoarte (fixat, ca să nu iasă cifre diferite la doi oameni)

- **Ocupare (F-601.1)** — numărător: minutele programărilor cu status **diferit de `CANCELLED` și
  `NO_SHOW`**. E exact filtrul folosit deja peste tot în `AppointmentRepository.findOverlappingFor*` și
  respectă decizia din Modulul 4 ("la `NO_SHOW` resursele sunt eliberate pentru rapoartele de utilizare").
  Numitor:
  - **medici**: minutele din `work_schedules` care cad în intervalul raportului (parcurgi zilele din
    interval, pentru fiecare zi aduni rândurile cu `day_of_week` potrivit — pot fi mai multe);
  - **cabinete**: cabinetele nu au orar propriu în model, deci folosim programul clinicii din config:
    `app.reports.clinic-open-time: "08:00"`, `app.reports.clinic-close-time: "20:00"`,
    `app.reports.clinic-working-days: MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY`. Valorile 08:00-20:00 nu
    sunt inventate — sunt exact limitele grilei de calendar deja folosite în frontend
    (`CALENDAR_MIN_TIME`/`CALENDAR_MAX_TIME` din `CalendarPage.jsx`).
  - dacă numitorul e 0 (medic fără orar în interval, sau interval numai din weekend) → **rata = 0**, nu
    `NaN`, nu împărțire la zero. Frontend-ul afișează `—` pentru rândurile cu numitor 0.
- **No-show (F-601.2)**: `noShows / total`, unde `total` = toate programările din interval **mai puțin
  cele `CANCELLED`** (o programare anulată din timp nu e o neprezentare și nu trebuie să dilueze rata).
- **Vânzări (F-601.3)**: doar programările **`COMPLETED`**. Nu `CONFIRMED`, nu `IN_PROGRESS` — banii se
  consideră încasați când consultul s-a încheiat. Valoarea = `service.price`.
- **Toate intervalele sunt `from`/`to` ca `LocalDate`, inclusive la ambele capete**, interpretate ca zile
  întregi (`from T00:00:00` → `to T23:59:59.999`). Nu acceptăm ore parțiale — simplifică enorm calculul
  numitorului de ocupare și nu pierdem nimic din ce cere caietul.
- **Filtrarea după timp se face pe `startTime`**, nu pe `createdAt`.

> **Problemă reală de semnalat, nu de ascuns:** prețul nu e înghețat pe programare. Raportul de vânzări
> citește `service.price` **de azi**, deci dacă adminul schimbă prețul unui serviciu, rapoartele din
> lunile trecute se schimbă retroactiv. Pentru un raport financiar e o slăbiciune reală. Reparația e
> mică (coloană `price_at_booking` pe `appointments`, populată în `create()`), dar atinge Lotul A și
> migrația — de aceea e trecută ca task opțional **B7**, de făcut doar dacă rămâne timp. Dacă nu se face,
> **scrieți limitarea în README/prezentare**.

---

## 2. LOTUL A — Modulul 5: Notificări SMS

**Pachete atinse:** `entity`, `enums`, `repository`, `service` (subpachet nou `service/notification`),
`controller`, `dto`, `util`, `config`.

### A1. Entitate + enum-uri + repository `[obligatoriu]`

- [ ] `enums/NotificationTrigger.java` (nou): `CONFIRMATION`, `REMINDER_24H`, `RESCHEDULED`, `CANCELLED`.
- [ ] `enums/NotificationStatus.java` — **adaugă valoarea `CANCELLED`** la cele existente
  (`PENDING`, `SENT`, `FAILED`). Enum-ul nu e folosit nicăieri azi, deci adăugarea e gratuită.
- [ ] `entity/Notification.java` (nou, tabela `notifications`):

  | Câmp | Tip | Note |
  |---|---|---|
  | `id` | `UUID` | `@GeneratedValue(strategy = UUID)`, ca peste tot |
  | `appointment` | `@ManyToOne(fetch = LAZY)`, `nullable = false` | FK spre `appointments` |
  | `type` | `NotificationType` `@Enumerated(STRING)` | mereu `SMS` |
  | `trigger` | `NotificationTrigger` `@Enumerated(STRING)` | atenție: `trigger` e cuvânt rezervat în unele dialecte SQL — mapați coloana ca `notification_trigger` |
  | `status` | `NotificationStatus` `@Enumerated(STRING)` | default `PENDING` |
  | `recipientPhone` | `String(20)` | deja normalizat E.164 (1.5) |
  | `body` | `TEXT` | textul exact trimis |
  | `nextAttemptAt` | `LocalDateTime` | momentul primei/următoarei încercări |
  | `attempts` | `int` | default 0 |
  | `lastError` | `String(500)` | nullable |
  | `sentAt` | `LocalDateTime` | nullable |
  | `providerMessageId` | `String(64)` | SID-ul Twilio, nullable |
  | `confirmationToken` | `String(32)`, `unique` | doar pentru `REMINDER_24H`, altfel `null` |
  | `createdAt` / `updatedAt` | `LocalDateTime` | `@PrePersist`/`@PreUpdate`, ca pe `Appointment` |

  **O singură coloană de programare în timp (`nextAttemptAt`)**, nu două (`scheduledFor` + `nextAttemptAt`) —
  fac același lucru și ar diverge.
- [ ] `repository/NotificationRepository.java`:
  - `List<Notification> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(NotificationStatus status, LocalDateTime now)`
  - `Optional<Notification> findByConfirmationToken(String token)`
  - `List<Notification> findByAppointmentIdAndTriggerAndStatus(UUID appointmentId, NotificationTrigger trigger, NotificationStatus status)`
    (pentru anularea reminder-ului la reprogramare/anulare)
  - `Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable)` + variantă filtrată pe status
    (alimentează ecranul de administrare din frontend, vezi A8)

### A2. Normalizare telefon + factory de mesaje `[obligatoriu]`

- [ ] `util/PhoneNumberNormalizer.java` — regulile din 1.5, plus test unitar cu toate cazurile
  (`0721111111`, `+40721111111`, `0040721111111`, `072 111 1111`, `123` → excepție).
- [ ] `service/notification/NotificationMessageFactory.java` — o metodă per trigger, textele din 1.4,
  **fără diacritice**, cu `DateTimeFormatter` pentru `dd.MM.yyyy` / `HH:mm`. Numele medicului din
  `appointment.getDoctor().getUser().getUsername()`, cabinetul din `appointment.getRoom().getName()`.
  Test unitar: mesajul generat **nu conține** niciun caracter în afara setului GSM-7 (verificare simplă
  cu regex pe `[A-Za-z0-9 .,:;()+\-/@]+`) și nu depășește 320 de caractere.

### A3. Outbox — inserarea notificărilor `[obligatoriu]`

- [ ] `service/notification/NotificationOutboxService.java` (interfață + impl, ca la
  `ConsultationRecordService`) cu metodele:
  - `void enqueueConfirmation(Appointment appointment)`
  - `void enqueueReminder(Appointment appointment)` — nu face nimic dacă `startTime - 24h <= now()`
  - `void enqueueRescheduled(Appointment appointment)` — anulează reminder-ul `PENDING` existent și
    reprogramează unul nou
  - `void enqueueCancelled(Appointment appointment)` — anulează reminder-ul `PENDING` existent
  - `void cancelPendingReminders(UUID appointmentId)` (folosită de ultimele două)
- [ ] Generarea token-ului (1.7) trăiește aici, la crearea rândului de reminder.
- [ ] **Hook-urile în `AppointmentService`** — trei apeluri, în metodele existente, la finalul lor,
  în aceeași tranzacție:
  - `create()` → `enqueueConfirmation(saved)` + `enqueueReminder(saved)`
  - `update()` → `enqueueRescheduled(saved)`
  - `cancel()` → `enqueueCancelled(appointment)`

  **Atât.** Nu adăugați apeluri în `confirm`/`checkIn`/`noShow`/`complete` (vezi 1.3). `AppointmentService`
  primește o singură dependință nouă (`NotificationOutboxService`) — nu importați `SmsSender` acolo.
  Dacă normalizarea telefonului aruncă excepție, rândul se inserează oricum, cu `status = FAILED` și
  `lastError` — **creare de programare nu trebuie să pice pentru că pacientul are un telefon aiurea**.

### A4. `SmsSender` + implementarea Twilio `[obligatoriu]`

- [ ] `service/notification/SmsSender.java`:
  ```java
  public interface SmsSender {
      /** @return id-ul mesajului la provider (SID la Twilio) */
      String send(String toE164, String body);
  }
  ```
- [ ] Două excepții proprii în `exception/`: `SmsPermanentException` (nu se reîncearcă) și
  `SmsTransientException` (se reîncearcă). Ambele `RuntimeException`, ambele cu mesaj + cauză.
- [ ] `service/notification/TwilioSmsSender.java` —
  `@ConditionalOnProperty(name = "app.sms.enabled", havingValue = "true")`. Inițializare `Twilio.init(sid,
  token)` într-un `@PostConstruct`. Maparea erorilor Twilio pe cele două excepții, conform 1.6.
- [ ] `service/notification/LoggingSmsSender.java` — `@ConditionalOnMissingBean(SmsSender.class)`,
  loghează `to` + `body` la nivel `INFO` și întoarce `"LOG-" + UUID.randomUUID()`. Ăsta e bean-ul activ
  implicit pentru toată echipa.
- [ ] Config în `application.yml` (valori goale, doar placeholder-e de env — vezi §4.3).

### A5. Dispatcher-ul periodic `[obligatoriu]`

- [ ] `service/notification/NotificationDispatcher.java` — `@Component`, în **exact același tipar** ca
  `ConsultationRecordLockScheduler` (citiți-l întâi):
  ```java
  @Scheduled(fixedDelay = 15_000)
  public void dispatchPending() { ... }
  ```
  `fixedDelay`, **nu** `fixedRate` — apelurile către Twilio sunt I/O lent și cu `fixedRate` rundele s-ar
  putea suprapune (vezi 1.2).
- [ ] Pentru fiecare rând scadent:
  - succes → `status = SENT`, `sentAt = now()`, `providerMessageId = <SID>`, `lastError = null`
  - `SmsPermanentException` → `status = FAILED`, `lastError = <mesaj>`, **fără** reprogramare
  - `SmsTransientException` (sau orice `RuntimeException` neașteptată) → `attempts++`; dacă
    `attempts >= 5` → `FAILED`, altfel rămâne `PENDING` cu
    `nextAttemptAt = now() + BACKOFF[attempts - 1]`, unde
    `BACKOFF = {1 min, 5 min, 15 min, 1 h, 6 h}`
  - **fiecare rând se procesează într-un `try/catch` propriu** — o notificare stricată nu are voie să
    oprească lotul (exact ca `catch (RuntimeException)`-ul din `ConsultationRecordLockScheduler`)
- [ ] Log la nivel `WARN` pentru fiecare eșec, cu id-ul notificării. **Nu logați `body`-ul integral la
  fiecare eroare** — sunt date de pacient în fișierele de log.

### A6. Endpoint-urile publice de confirmare/anulare `[obligatoriu]`

- [ ] `dto/PublicAppointmentResponse.java` — **strict** câmpurile din 1.7:
  ```java
  public record PublicAppointmentResponse(
      String patientFirstName, LocalDateTime startTime, LocalDateTime endTime,
      String doctorName, String roomName, AppointmentStatus status,
      boolean canConfirm, boolean canCancel
  ) {}
  ```
- [ ] `controller/PublicAppointmentController.java` — `@RequestMapping("/public/appointments")`,
  **fără `@PreAuthorize`** (e sub `/public/**`, deja permis în `SecurityConfig` — nu atingeți
  `SecurityConfig`):
  - `GET /{token}` → 200 / 404 (token inexistent) / 410 (`startTime` a trecut)
  - `POST /{token}/confirm` → trece prin `appointmentService.confirm(id)` (deci prin state machine)
  - `POST /{token}/cancel` → trece prin `appointmentService.cancel(id)`
  - `canConfirm` = statusul curent e `SCHEDULED`; `canCancel` = statusul e `SCHEDULED` sau `CONFIRMED`
    (aceeași matrice ca `AppointmentStateMachine` — citiți-o de acolo, nu o rescrieți)

### A7. Teste `[obligatoriu]`

- [ ] `create()` inserează exact 2 rânduri `PENDING` (confirmare + reminder) pentru o programare peste
  3 zile, și **doar 1** (confirmarea) pentru una peste 2 ore.
- [ ] `update()` pune reminder-ul vechi pe `CANCELLED` și creează unul nou cu `nextAttemptAt` corect.
- [ ] `cancel()` pune reminder-ul pe `CANCELLED` și creează un rând `CANCELLED`-trigger `PENDING`.
- [ ] **Testul de NFR-2 (cel care chiar contează la prezentare):** un `SmsSender` mock care aruncă
  `SmsTransientException` la prima rulare → rândul rămâne `PENDING`, `attempts == 1`,
  `nextAttemptAt > now()`. La a doua rulare, cu mock-ul reparat → `SENT`. Ăsta e "sistemul stochează
  notificările eșuate și le retrimite când serviciul revine", demonstrat, nu presupus.
- [ ] `SmsPermanentException` → `FAILED` din prima, `attempts == 1`, fără reprogramare.
- [ ] Endpoint public: token inexistent → 404; programare din trecut → 410; `confirm` pe o programare
  deja `CANCELLED` → 409; iar răspunsul de la `GET /public/appointments/{token}` **nu conține** `cnp`,
  `phone`, `email`, `notes` (asertați pe JSON-ul brut, nu pe DTO).
- [ ] `PhoneNumberNormalizerTest` (A2) și testul de charset pe `NotificationMessageFactory` (A2).

### A8. Endpoint de administrare a cozii `[obligatoriu, dar mic]`

Alimentează pagina de administrare din frontend (P5 frontend):

- [ ] `GET /api/notifications?status=&appointmentId=&page=&size=` — `ADMIN` only, paginat, ordonat
  descrescător după `createdAt`. Ambele filtre sunt opționale.
  DTO `NotificationResponse(id, appointmentId, trigger, status, recipientPhone, body,
  attempts, lastError, nextAttemptAt, sentAt, createdAt)`.
  **Maschează telefonul** în răspuns (`+4072***1111`) — e o listă administrativă, nu are nevoie de
  numărul complet.
  > Filtrul `appointmentId` nu e decorativ: frontend-ul (P5.2) afișează starea SMS-urilor direct în
  > modalul de detalii al programării din calendar. E o linie în plus în repository — **confirmați în
  > grup când intră**, pentru că e singura lor dependință de acest task.
- [ ] `POST /api/notifications/{id}/retry` — `ADMIN` only. Doar pentru rândurile `FAILED`: resetează
  `status = PENDING`, `attempts = 0`, `nextAttemptAt = now()`. Pe orice alt status → 409.

---

## 3. LOTUL B — Modulul 6: Rapoarte și Export

**Pachete atinse:** `dto/report` (nou), `repository`, `service/report` (nou), `controller`, `config`.

### B1. DTO-urile de raport `[obligatoriu — se face primul, e contractul pentru frontend]`

Toate ca `record`, în `dto/report/`. **Ratele se întorc ca fracție `0..1`** (ex. `0.734`), nu ca 73.4 —
motivul e la B6: Excel are nevoie de fracție + format `0.0%` ca celula să fie procent nativ, nu text.
Frontend-ul înmulțește cu 100 la afișare (e scris explicit în doc-ul lor).

```java
record ResourceOccupancyRow(UUID resourceId, String name, long bookedMinutes,
                            long availableMinutes, double occupancyRate) {}
record OccupancyReportResponse(LocalDate from, LocalDate to,
                               List<ResourceOccupancyRow> doctors,
                               List<ResourceOccupancyRow> rooms) {}

record PatientNoShowRow(UUID patientId, String patientName, long total, long noShows, double rate) {}
record WeekdayNoShowRow(DayOfWeek dayOfWeek, long total, long noShows, double rate) {}
record NoShowReportResponse(LocalDate from, LocalDate to, long total, long noShows, double rate,
                            List<PatientNoShowRow> byPatient, List<WeekdayNoShowRow> byWeekday) {}

record SalesRow(UUID id, String name, long appointments, BigDecimal total) {}
record SalesReportResponse(LocalDate from, LocalDate to, BigDecimal grandTotal,
                           List<SalesRow> byService, List<SalesRow> byDoctor) {}
```

### B2. Query-uri agregate `[obligatoriu]`

**Agregarea se face în SQL, nu în Java.** Nu scrieți `appointmentRepository.findAll()` urmat de
`.stream().filter(...)` — pe un interval de un an ar trage toată tabela în memorie pentru un raport.

În `AppointmentRepository` (sau, dacă preferați, un `ReportRepository` separat cu `@Query` native — alegeți
unul și rămâneți la el):

- [ ] **Vânzări pe serviciu / pe medic** — JPQL cu expresie de constructor:
  ```java
  @Query("""
      SELECT new com.holisun.backend.dto.report.SalesRow(s.id, s.name, COUNT(a), SUM(s.price))
      FROM Appointment a JOIN a.service s
      WHERE a.status = com.holisun.backend.enums.AppointmentStatus.COMPLETED
        AND a.startTime >= :from AND a.startTime < :to
      GROUP BY s.id, s.name
      ORDER BY SUM(s.price) DESC
      """)
  List<SalesRow> sumSalesByService(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
  ```
  Analog pentru medic (`GROUP BY d.id, u.username`, cu `JOIN a.doctor d JOIN d.user u`).
  Atenție: `COUNT` întoarce `Long`, `SUM(s.price)` întoarce `BigDecimal` — componentele record-ului
  trebuie să se potrivească exact, altfel Hibernate nu găsește constructorul.
- [ ] **No-show pe pacient** — `GROUP BY p.id`, cu
  `COUNT(a)` și `SUM(CASE WHEN a.status = NO_SHOW THEN 1 ELSE 0 END)`, filtrat
  `a.status <> CANCELLED` (vezi 1.9). Rata se calculează în Java, din cele două numere — nu în SQL
  (evită împărțirea la zero în dialect).
- [ ] **Minute ocupate pe medic / pe cabinet** — aici JPQL nu are o funcție portabilă de diferență între
  timestamp-uri, iar proiectul e oricum legat de Postgres (`btree_gist`, `EXCLUDE USING gist` în `V7`),
  deci folosiți **native query**:
  ```sql
  SELECT doctor_id AS resource_id,
         COALESCE(SUM(EXTRACT(EPOCH FROM (end_time - start_time)) / 60), 0) AS booked_minutes
  FROM appointments
  WHERE status NOT IN ('CANCELLED', 'NO_SHOW')
    AND start_time >= :from AND start_time < :to
  GROUP BY doctor_id
  ```
  (+ varianta cu `room_id`). Mapați pe o interfață de proiecție Spring Data
  (`interface ResourceMinutes { UUID getResourceId(); double getBookedMinutes(); }`).

### B3. `ReportService` — calculul propriu-zis `[obligatoriu]`

- [ ] `service/report/ReportService.java` cu trei metode publice
  (`occupancy(from, to)`, `noShow(from, to)`, `sales(from, to)`).
- [ ] Numitorul ratei de ocupare (regulile din 1.9): pentru medici, parcurgeți zilele din interval și
  însumați rândurile de `WorkSchedule` cu `dayOfWeek` potrivit (**pot fi mai multe pe aceeași zi** —
  vezi §0.10); pentru cabinete, `(clinic-close - clinic-open) × numărul de zile lucrătoare din interval`.
- [ ] Validare de input, cu 400 clar (`ResponseStatusException(BAD_REQUEST, ...)`):
  `from` și `to` obligatorii, `from <= to`, interval maxim **366 de zile** (un raport pe 10 ani nu are
  sens și transformă exportul într-un timeout).
- [ ] Toate medicii/cabinetele **active** apar în raport, inclusiv cele cu 0 programări (rând cu
  `bookedMinutes = 0`) — altfel adminul nu vede exact ce vrea să vadă: resursa nefolosită.

### B4. `ReportController` `[obligatoriu]`

- [ ] `controller/ReportController.java`, `@RequestMapping("/api/reports")`,
  **`@PreAuthorize("hasRole('ADMIN')")` la nivel de clasă** (decizia 1.8):

  | Method | Path | Query params | Răspuns |
  |---|---|---|---|
  | GET | `/api/reports/occupancy` | `from`, `to` (`yyyy-MM-dd`) | `OccupancyReportResponse` |
  | GET | `/api/reports/no-show` | `from`, `to` | `NoShowReportResponse` |
  | GET | `/api/reports/sales` | `from`, `to` | `SalesReportResponse` |
  | GET | `/api/reports/{type}/export` | `from`, `to`, `format=pdf\|xlsx` | fișier binar |

  `type` ∈ `occupancy` \| `no-show` \| `sales`; orice altceva → 400.
  Datele se leagă cu `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)`, la fel ca `from`/`to` din
  `AppointmentController.getCalendar`.

### B5. Export PDF `[obligatoriu]`

- [ ] `service/report/ReportPdfExporter.java`, cu **OpenPDF** (`com.github.librepdf:openpdf`) — nu iText 7
  (licență AGPL, ne obligă să publicăm tot proiectul sub AGPL). OpenPDF e fork-ul LGPL/MPL al lui iText 4,
  API practic identic cu tutorialele vechi de iText.
- [ ] Format: **A4 landscape** (tabelele au 5-6 coloane), antet cu numele clinicii + tipul raportului +
  intervalul + data generării, `PdfPTable` cu `setHeaderRows(1)` (capul de tabel se repetă pe fiecare
  pagină), număr de pagină în subsol.
- [ ] **Fără diacritice în PDF**, din același motiv practic ca la SMS: fontul implicit (Helvetica,
  Cp1252) nu redă corect `ș`/`ț` și textul iese cu semne de întrebare. Dacă cineva chiar vrea diacritice,
  trebuie înglobat un TTF în resurse — task separat, opțional, nu blocați exportul pe el.

### B6. Export Excel `[obligatoriu]`

- [ ] `service/report/ReportExcelExporter.java`, cu **Apache POI** (`poi-ooxml`), `XSSFWorkbook`.
- [ ] **Cerința explicită din F-602 — "interpretarea nativă a valorilor numerice în Excel"** — înseamnă
  concret:
  - numerele se scriu cu `cell.setCellValue(double)` / `setCellValue(bigDecimal.doubleValue())`,
    **niciodată** ca string;
  - procentele se scriu ca **fracție** (`0.734`) cu `CellStyle` de format `0.0%` — nu ca text `"73.4%"`;
  - sumele de bani cu format `#,##0.00`;
  - datele calendaristice cu `CellStyle` de format `dd.mm.yyyy` pe o valoare de tip dată.
  Verificarea e simplă și trebuie făcută manual o dată: deschideți fișierul și dați `SUM` pe o coloană —
  dacă nu se însumează, e text.
- [ ] Un sheet per secțiune (`Medici`, `Cabinete` la ocupare; `Pacienti`, `Zile` la no-show;
  `Servicii`, `Medici` la vânzări), rând de antet **bold + freeze pane**, `autoSizeColumn` la final.
- [ ] Header-ele HTTP: `Content-Type` corect
  (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` pentru xlsx, `application/pdf`
  pentru pdf) + `Content-Disposition: attachment; filename="raport-ocupare_2026-07-01_2026-07-31.xlsx"`.
  **Numele fișierului doar cu caractere ASCII** (fără diacritice, fără spații) — altfel unele browsere
  strică descărcarea.

### B7. Înghețarea prețului pe programare `[opțional — doar dacă rămâne timp]`

Repară problema din 1.9 (rapoarte financiare care se schimbă retroactiv):

- [ ] `Appointment.priceAtBooking : BigDecimal, nullable` + coloană în `V9__.sql`.
- [ ] Populată în `AppointmentService.create()` din `service.getPrice()`.
- [ ] Raportul de vânzări folosește `COALESCE(a.price_at_booking, s.price)` — programările vechi (fără
  snapshot) continuă să funcționeze pe prețul curent.
- [ ] Coordonare obligatorie cu Lotul A înainte: atinge `AppointmentService.create()`, unde intră și
  hook-urile de notificări (A3).

### B8. Teste `[obligatoriu]`

- [ ] **Testul cerut explicit de DoD (criteriul #3):** `DOCTOR` pe `GET /api/reports/sales` → **403**.
  Plus `RECEPTION` → 403, `ADMIN` → 200. `@WebMvcTest`, exact în tiparul lui
  `AppointmentControllerSecurityTest` (citiți-l întâi, nu inventați alt setup de securitate).
- [ ] Test de calcul pentru ocupare, pe date construite de mână: un medic cu orar Luni 08:00-12:00
  (240 min) și o programare de 60 min luni → `occupancyRate == 0.25`. Un medic fără orar → rata 0, nu
  `NaN`, nu excepție.
- [ ] Test că programările `CANCELLED`/`NO_SHOW` **nu** intră în minutele ocupate, dar `NO_SHOW` **intră**
  în raportul de no-show.
- [ ] Test de vânzări: doar `COMPLETED` se numără; `grandTotal` == suma rândurilor.
- [ ] Test pe export: `GET /api/reports/sales/export?format=xlsx` întoarce 200, `Content-Type` corect și
  un body ne-gol care începe cu semnătura ZIP (`PK`) — un `.xlsx` e o arhivă zip, deci ăsta e un
  smoke-test suficient fără să parsați fișierul înapoi.

---

## 4. Zone comune celor două loturi — anunțați-vă înainte de a le atinge

Sunt singurele trei fișiere pe care lucrează amândoi. Ca să nu ieșiți cu conflicte:

### 4.1. `pom.xml`

Se adaugă **într-un singur commit, la început, de către unul dintre voi**, toate cele trei dependințe
deodată (nu în trei PR-uri separate):

```xml
<!-- Modulul 5 -->
<dependency>
  <groupId>com.twilio.sdk</groupId>
  <artifactId>twilio</artifactId>
  <version>__verificati_ultima_versiune__</version>
</dependency>

<!-- Modulul 6 -->
<dependency>
  <groupId>org.apache.poi</groupId>
  <artifactId>poi-ooxml</artifactId>
  <version>__verificati_ultima_versiune__</version>
</dependency>
<dependency>
  <groupId>com.github.librepdf</groupId>
  <artifactId>openpdf</artifactId>
  <version>__verificati_ultima_versiune__</version>
</dependency>
```

Verificați versiunile pe mvnrepository la momentul implementării — nu copiați numere din acest document.

### 4.2. `V9__.sql` — o singură migrație, scrisă la final

Scrieți-o **după** ce ambele loturi au entitățile stabile, exact ca la V5/V7/V8: rulați în dev cu
`ddl-auto: update`, apoi transcrieți schema reală generată. Conține:

- `CREATE TABLE notifications (...)` + FK spre `appointments` + `UNIQUE` pe `confirmation_token`
- index pe `(status, next_attempt_at)` — e query-ul rulat de dispatcher la fiecare 15 secunde
- index pe `appointments (start_time)` — toate cele trei rapoarte filtrează pe el
- (opțional, dacă se face B7) `ALTER TABLE appointments ADD price_at_booking NUMERIC(10,2)`

**Atenție, aceeași capcană ca la `V7__.sql`:** dacă aveți deja o bază `medsched_dev` locală pe care ați
pornit aplicația cu `ddl-auto: update` înainte de migrație, Hibernate a creat deja `notifications` fără
să treacă prin Flyway, iar `V9` va eșua cu "relation already exists". `DROP TABLE notifications CASCADE;`
sau resetați baza de dev înainte de a porni din nou.

### 4.3. `application.yml`

```yaml
app:
  sms:
    enabled: ${SMS_ENABLED:false}      # kill switch — default OFF pentru toată echipa
    account-sid: ${TWILIO_ACCOUNT_SID:}
    auth-token: ${TWILIO_AUTH_TOKEN:}
    from-number: ${TWILIO_FROM_NUMBER:}
  notifications:
    public-base-url: ${PUBLIC_BASE_URL:http://localhost:5173}
    clinic-phone: "0264 000 000"
  reports:
    clinic-open-time: "08:00"
    clinic-close-time: "20:00"
    clinic-working-days: MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY
```

Valorile reale ale credențialelor **doar din variabile de mediu** (vezi 1.6). Nimic secret în git.

### 4.4. `module5_6_endpoints.md`

La final, cine termină primul publică `documents/module5_6/module5_6_endpoints.md`, în același format ca
`module4_endpoints.md` (tabel de endpoint-uri + shape-uri TS + coduri de răspuns). Frontend-ul are 5
oameni care așteaptă contractul — **shape-urile din §2/§3 de aici sunt suficient de stabile ca ei să
înceapă imediat**, dar documentul final rămâne sursa de adevăr.

---

## 5. Ordine de lucru recomandată

1. **Împărțiți loturile** (recomandat: un om pe lot) și faceți **commit-ul de `pom.xml`** (§4.1) primul,
   ca amândoi să aveți dependințele.
2. **Lotul B poate porni imediat** — nu depinde de nimic din Lotul A. Ordinea internă: B1 (DTO-uri) →
   B2 (query-uri) → B3 (calcul) → B4 (controller) → B6 (Excel) → B5 (PDF) → B8 (teste, în paralel).
   **B1 se face în prima zi și se anunță în grup**, pentru că 3 din cei 5 oameni de pe frontend sunt
   blocați pe forma exactă a răspunsurilor.
3. **Lotul A**: A1 (entitate) → A2 (util) → A3 (outbox + hook-uri) → A4 (sender) → A5 (dispatcher) →
   A6 (public) → A8 (admin) → A7 (teste, în paralel de la A3 încolo).
   **A4 cu `LoggingSmsSender` se face devreme** — restul echipei trebuie să poată rula aplicația fără
   credențiale Twilio.
4. **Migrația `V9__.sql` se scrie ultima**, de comun acord, după ce ambele entități sunt stabile (§4.2).
5. **Testați cu Twilio real o singură dată, la final**, cu un pacient de test al cărui telefon e numărul
   vostru verificat. Nu lăsați `app.sms.enabled=true` pe branch — se merge-uiește cu `false`.

## 6. Ce NU e în scopul acestui sprint (ca să nu ne pierdem)

- Email (F-501/F-502 partea de email) — înlocuit conștient cu SMS, vezi 1.1.
- WebSocket/STOMP pentru sincronizare live (NFR-2) — rămânem la polling, decizie din Modulul 3.
- Rate limiting / captcha pe endpoint-urile publice — token de 96 de biți, vezi 1.7.
- Retrimitere manuală a unui SMS de confirmare din ecranul de programare — există doar retry pe rândurile
  `FAILED` (A8), atât.
- Grafice/diagrame în rapoarte (backend întoarce numere; dacă frontend-ul vrea bare, le desenează el).
- Rapoarte programate/trimise automat pe email — nu e cerut nicăieri.
- Multi-instanță / lock distribuit pe dispatcher — limitare documentată, vezi 1.2.
