# Modulul 3 — Motorul de Programări și Calendarul Interactiv — Împărțire Task-uri Backend

> Scop: F-301 (Interfața Calendar — partea de API care o alimentează), F-302 (Algoritmul de Verificare a
> Disponibilității) + bucata din NFR-2 care ține strict de acest modul (concurență la rezervare — două
> recepții nu pot rezerva simultan aceeași resursă).
>
> **Nu intră aici:**
> - Mașina de stări completă a programării (`Confirmat` → `Consultație Activă` → `Finalizat`/`Neprezentat`,
>   validarea tranzițiilor, blocarea fișei la `Finalizat`) — asta e F-401/F-402, **Modulul 4**. Aici
>   `Appointment` are un câmp `status`, pleacă din `SCHEDULED` la creare și poate ajunge în `CANCELLED`
>   printr-un endpoint simplu de anulare — atât. Restul tranzițiilor (drag&drop pe calendar, confirmare,
>   "consultație activă", finalizare) se implementează în Modulul 4, peste ce construim acum.
> - Notificări email/SMS la creare/reminder/anulare (F-501/F-502) — Modulul 5. `Appointment` doar există
>   ca entitate pe care Modulul 5 se va putea agăța (creăm hook-uri minime unde are sens, nu logica reală).
> - Rapoarte + export (F-601/F-602) — Modulul 6.
> - Sincronizare live pe ecranul altor utilizatori conectați (partea de WebSocket/STOMP din NFR-2) — nu
>   există deloc infrastructură de WebSocket în proiect încă (verificat: zero `spring-boot-starter-websocket`
>   în `pom.xml`, zero `WebSocketConfig`). Pentru acest modul rămânem la **polling simplu din frontend**
>   (re-fetch calendar la interval scurt) — suficient ca să respectăm "aproape instant, fără reload complet"
>   fără să deschidem un front nou de lucru (STOMP e o bucată mare, o discutăm separat dacă echipa vrea
>   real-time adevărat mai târziu).

**Convenție echipă:** de data asta avem un singur agregat central (`Appointment`), nu 3 entități separate
ca la Modulul 2 — deci împărțirea nu e "fiecare cu entitatea lui", ci "fiecare cu o bucată de business logic
peste aceeași entitate". **Controller-ul** (unul singur, `AppointmentController`) îl fac eu (P5), ca de
obicei, ca să-l pot lega direct de frontend. **Migrația SQL** (`V5__.sql`) e a lui **P1** (owner-ul entității)
de data asta — nu mai avem o persoană dedicată doar migrațiilor ca la Modulul 2, pentru că e un singur tabel
nou (plus 2 constrângeri de tip exclusion, nu 3 tabele separate). Restul lucrează cu `ddl-auto: update`
(profil `dev`) și nu ating fișierele din `db/migration/` direct — trimit lui P1 coloanele de care au nevoie.

---

## 0. Contract comun — de citit ÎNAINTE de a scrie cod (P1, P2, P3, P4)

Fixăm acum forma entității `Appointment` și cine scrie ce query în `AppointmentRepository`, ca să nu ne
călcăm pe picioare pe același fișier. Dacă cineva descoperă că trebuie schimbat ceva, se anunță în grup
înainte de a modifica — schimbă minim 2 persoane.

### Ce există deja și pe care ne bazăm (verificat în cod, nu presupus)

- `enums/AppointmentStatus.java` **există deja**, neutilizat de nimic încă:
  `SCHEDULED, CONFIRMED, IN_PROGRESS, COMPLETED, NO_SHOW, CANCELLED`. Îl folosim ca atare — nu-l
  redenumiți, Modulul 4 depinde de exact aceste valori (vezi diagrama din caietul de sarcini).
- `service/WorkScheduleValidator.java` are deja un comentariu explicit: *"Nu verifică conflicte față de
  programări existente (asta e responsabilitatea viitorului `AvailabilityValidatorService`, la nivel de
  appointment)"* — deci numele clasei de mai jos (P2) nu e ales de noi acum, era deja anticipat în cod.
  Respectați acest nume exact.
- `entity/ConsultationRecord.java` are deja `appointmentId: UUID` **fără FK** (comentariu: "tabela
  appointments nu există încă") și un `boolean locked` cu un hook `lock(UUID appointmentId)` expus explicit
  pentru Modulul 3/4 să-l apeleze. **Nu creați FK spre `consultation_records` din `Appointment`** — relația
  rămâne "prin id gol" până vine Modulul 4 și decide cum leagă tranziția spre `COMPLETED` de acest hook.
  Nu e treaba noastră acum să apelăm `lock()`.
- `entity/Service.java` (business-wise = "ClinicalService", tabela `services`, controller-ul e deja
  `/api/services`) are `defaultDurationMinutes` și `requiredEquipmentTypes: Set<EquipmentType>` — sursa de
  adevăr pentru cât durează o programare și ce tip(uri) de echipament sunt obligatorii.
- `entity/Doctor.java` are `weeklySchedule: List<WorkSchedule>` (zi + interval orar) și
  `standardConsultationDurationMinutes` — folosite doar ca fallback/validare orar de lucru, **nu** ca sursă
  de durată a programării (vezi decizia de mai jos).
- `entity/Equipment.java` are `equipmentType` (obligatoriu) și `room` (opțional — echipament mobil).

### Decizie de business nouă (fixată acum, ca să nu discutăm în paralel variante diferite)

**Durata unei programări = `service.defaultDurationMinutes`, nu `doctor.standardConsultationDurationMinutes`.**
Motiv: caietul de sarcini leagă durata de *serviciul* prestat (F-103: "fiecare serviciu are asociat... o
durată implicită"), nu de medic — un consult pediatric simplu și o ecografie durează diferit chiar la
același medic. Câmpul de pe `Doctor` rămâne ca informație descriptivă / eventual fallback dacă cineva
vrea să adauge ulterior programări fără serviciu explicit (nu e cazul acum — `service` e obligatoriu la
creare). `endTime` **nu se trimite din frontend** — se calculează pe backend din `startTime + service.defaultDurationMinutes`, ca să nu putem primi un `endTime` inconsistent de la client.

### `Appointment` (owner: P1)
```
id            : UUID (PK)
patient       : Patient, @ManyToOne, not null            -- FK -> patients(id)
doctor        : Doctor, @ManyToOne, not null              -- FK -> doctors(id)
room          : Room, @ManyToOne, not null                 -- FK -> rooms(id)
service       : Service, @ManyToOne, not null                -- FK -> services(id) ("ClinicalService")
equipment     : Equipment, @ManyToOne, nullable               -- instanța concretă alocată; null dacă
                                                                   serviceul nu cere niciun tip de echipament
startTime     : LocalDateTime, not null
endTime       : LocalDateTime, not null              -- calculat pe server, vezi decizia de mai sus
status        : AppointmentStatus, not null, default SCHEDULED
notes         : String (TEXT), nullable               -- "instrucțiuni speciale" (F-502), text liber recepție
version       : long, @Version                          -- optimistic locking, vezi nota P1 mai jos
createdAt     : LocalDateTime, not null
updatedAt     : LocalDateTime, not null
```

**De ce `@Version`:** pe lângă verificarea de disponibilitate (P2) care previne *coliziuni între
programări diferite*, `@Version` previne un caz separat — doi utilizatori editează **aceeași** programare
simultan (ex: unul o reprogramează, altul o anulează în paralel). JPA aruncă
`OptimisticLockingFailureException` automat la al doilea `save`, fără cod suplimentar — trebuie doar
mapat la 409 în `GlobalExceptionHandler` (P1 face asta, e legat direct de entitatea lui).

### Cine scrie ce în `AppointmentRepository` (fișier comun, coordonare obligatorie)

Toți patru lucrați pe **același** fișier `repository/AppointmentRepository.java` — ca să nu apară conflicte
de merge inutile, împărțim clar cine adaugă ce metodă și **P1 le scrie pe primele 3 chiar din prima zi**
(sunt fundația de care depind P2 și P3):

- P1 (chiar de la început, blocant pentru P2/P3):
  - `List<Appointment> findOverlappingForDoctor(UUID doctorId, LocalDateTime start, LocalDateTime end, UUID excludeAppointmentId)`
  - `List<Appointment> findOverlappingForRoom(UUID roomId, LocalDateTime start, LocalDateTime end, UUID excludeAppointmentId)`
  - `List<Appointment> findOverlappingForEquipment(UUID equipmentId, LocalDateTime start, LocalDateTime end, UUID excludeAppointmentId)`

  Toate trei exclud programările `CANCELLED`/`NO_SHOW` din verificare (o programare anulată nu mai ocupă
  resursa) — filtrați `status NOT IN (CANCELLED, NO_SHOW)` direct în `@Query`. `excludeAppointmentId` e
  `nullable` (folosit la `update`/reprogramare, ca o programare să nu se considere în conflict cu ea
  însăși) — pasați `null` la `create`.
- P3 (după ce P1 a livrat cele de mai sus): eventual o variantă care caută toate echipamentele **libere**
  dintr-un tip dat într-un interval (vezi task-ul lui P3, secțiunea 3) — dacă se poate scrie ca un singur
  `@Query` peste `findOverlappingForEquipment`, mai bine, nu duplicați logica de overlap.
- P4 (independent, nu depinde de P2/P3): finder-e de listare pentru calendar —
  `findByDoctorIdInAndStartTimeLessThanAndEndTimeGreaterThan(...)` (sau `@Query` echivalent, mai lizibil),
  `findByRoomIdAndStartTimeBetween(...)`. Nu ating query-urile de overlap ale lui P1.

### Roluri (`enums/Role.java`: `ADMIN`, `DOCTOR`, `RECEPTION`) — cine poate crea/vedea programări

Conform NFR-1 din caiet: *"Medic: Vizualizează doar propriul calendar... Recepționer: Poate crea pacienți
și programări, poate schimba stările programărilor"*. Deci:
- Creare/reprogramare/anulare (`POST`, `PUT`, `PATCH /cancel`) → `ADMIN`, `RECEPTION`. **Nu `DOCTOR`** —
  medicul nu-și creează singur programări în acest flux (recepția face asta), doar le vede.
- Citire/calendar (`GET`) → `ADMIN`, `DOCTOR`, `RECEPTION` — dar pentru `DOCTOR`, filtrarea la "propriul
  calendar" se impune **la nivel de query params validați în controller** (P5): dacă rolul e `DOCTOR`,
  ignorăm orice `doctorIds` trimis din frontend și forțăm `doctorId = doctorul curent` (derivat din JWT,
  nu din request) — altfel un medic ar putea cere calendarul altui medic doar schimbând un query param.
  Detaliez asta la task-ul lui P5.

---

## 1. Persoana 1 — `Appointment` (entitate, repository, DTO, service CRUD) + migrație

**Pachete:** `entity`, `repository`, `service`, `dto`, `db/migration`

- [ ] `entity/Appointment.java` — conform contractului de mai sus. `@PrePersist`/`@PreUpdate` pentru
  `createdAt`/`updatedAt`. `endTime` se calculează în service (P1), **nu** în `@PrePersist` — la
  `@PrePersist` `service` s-ar putea să nu fie încă rezolvat din DTO în entitate în ordinea corectă;
  calculați explicit în `AppointmentService.create/update` înainte de `save`.
- [ ] `repository/AppointmentRepository.java extends JpaRepository<Appointment, UUID>` — cele 3 metode de
  overlap din contract (secțiunea 0), livrate primele, restul echipei se leagă de ele.
- [ ] `dto/AppointmentRequest.java` — `patientId`, `doctorId`, `roomId`, `serviceId`, `startTime`
  (`@NotNull`), `notes` (opțional). **Fără `endTime`, fără `equipmentId`** — `endTime` se calculează pe
  server, `equipment` se alocă automat de `EquipmentAllocationService` (P3), nu se alege manual de
  recepție (evită situația din persona Dr. Simona: "un ecograf deja rezervat în alt cabinet" — dacă
  recepția ar alege manual echipamentul, ar putea alege unul ocupat; alocarea automată elimină clasa asta
  de eroare).
- [ ] `dto/AppointmentResponse.java` — toate câmpurile entității, cu sub-obiecte "ușoare" pentru relații
  (nu serializați `Patient`/`Doctor` întregi — un `PatientSummary { id, firstName, lastName }`,
  `DoctorSummary { id, firstName, lastName, speciality }`, la fel `RoomSummary`/`ServiceSummary`/
  `EquipmentSummary` cu doar `id` + `name`). Coordonați cu P4 — el are nevoie de un DTO similar dar mai
  "slim" pentru vederea de calendar (vezi task-ul lui, poate refolosească aceste summary-uri).
- [ ] `service/AppointmentService.java`
  - `AppointmentResponse create(AppointmentRequest dto)`:
    1. Rezolvă `patient`/`doctor`/`room`/`service` din id-uri → 404 dacă vreunul nu există.
    2. Calculează `endTime = startTime + service.defaultDurationMinutes`.
    3. Apelează `AvailabilityValidatorService.validate(doctorId, roomId, startTime, endTime,
       excludeAppointmentId=null)` (P2) — propagă excepția lui dacă există conflict (nu o prinde, nu o
       transformă, `GlobalExceptionHandler` o mapează la 409).
    4. Dacă `service.requiredEquipmentTypes` nu e gol, cere de la `EquipmentAllocationService` (P3) o
       instanță liberă → dacă nu găsește, 409 ("Niciun echipament de tip X disponibil în acest interval").
    5. Salvează cu `status = SCHEDULED`.
  - `AppointmentResponse update(UUID id, AppointmentRequest dto)` — reprogramare: aceiași pași 1-4, dar cu
    `excludeAppointmentId = id`. **Nu poate reprograma o programare `COMPLETED` sau `CANCELLED`** → 409
    (regulă minimă de stare, restul mașinii de stări complete vine în Modulul 4, dar atât e nevoie acum
    ca să nu se poată "învia"/muta o programare deja închisă).
  - `AppointmentResponse getById(UUID id)` → 404 dacă nu există.
  - `void cancel(UUID id)` — setează `status = CANCELLED`; 409 dacă e deja `COMPLETED` sau `CANCELLED`.
    **Nu ștergeți programări** (nu există `delete`, la fel ca la `Patient` — păstrare istoric).
  - Mapper privat `toResponse(Appointment)` (stil `RoomService`, vezi
    [RoomService.java](../../backend/src/main/java/com/holisun/backend/service/RoomService.java)).
- [ ] Adaugă în `GlobalExceptionHandler` (fișier comun, doar adăugați un `@ExceptionHandler`, nu rescrieți
  restul) maparea `OptimisticLockingFailureException` → 409 cu mesaj clar ("Programarea a fost modificată
  între timp, reîncărcați și încercați din nou").
- [ ] `db/migration/V5__.sql` — după ce P2/P3/P4 au stabilizat ce coloane/relații le trebuie (nu porniți
  migrația în prima zi, la fel ca V3/V4 la modulele anterioare):
  - Tabel `appointments`: coloanele din contract, `FK -> patients/doctors/rooms/services/equipment`
    (equipment nullable), convenții `pk_appointments`, `fk_appointments_on_doctor` etc. (ca în V2/V3).
  - `CREATE EXTENSION IF NOT EXISTS btree_gist;` + **3 constrângeri `EXCLUDE` la nivel de Postgres**, ca
    plasă de siguranță împotriva coliziunilor chiar și sub concurență reală (NFR-2 — "sistemul trebuie să
    respingă automat a doua cerere"; verificarea din service (P2) e suficientă în 99% din cazuri, dar
    două tranzacții pot trece amândouă de verificarea Java dacă pornesc în același milisecond — constrângerea
    din DB e ce garantează *matematic* imposibilitatea, nu doar "de obicei"):
    ```sql
    ALTER TABLE appointments ADD CONSTRAINT excl_appointments_doctor_overlap
      EXCLUDE USING gist (doctor_id WITH =, tsrange(start_time, end_time) WITH &&)
      WHERE (status NOT IN ('CANCELLED', 'NO_SHOW'));

    ALTER TABLE appointments ADD CONSTRAINT excl_appointments_room_overlap
      EXCLUDE USING gist (room_id WITH =, tsrange(start_time, end_time) WITH &&)
      WHERE (status NOT IN ('CANCELLED', 'NO_SHOW'));

    ALTER TABLE appointments ADD CONSTRAINT excl_appointments_equipment_overlap
      EXCLUDE USING gist (equipment_id WITH =, tsrange(start_time, end_time) WITH &&)
      WHERE (equipment_id IS NOT NULL AND status NOT IN ('CANCELLED', 'NO_SHOW'));
    ```
    Coordonați exact acest SQL cu P2 înainte de a-l scrie definitiv — el scrie testul de concurență care
    trebuie să confirme că a doua tranzacție primește o excepție de constrângere (mapată tot la 409).

---

## 2. Persoana 2 — `AvailabilityValidatorService` (F-302, motorul de coliziuni)

**Pachete:** `service`, `exception`

- [ ] `exception/ResourceConflictException.java extends RuntimeException` — câmpuri `ResourceType
  conflictingResource` (enum nou, local: `DOCTOR, ROOM, EQUIPMENT`) + mesaj human-readable **specific
  resursei** (cerință explicită F-302: *"sistemul va afișa explicit resursa care generează conflictul
  (ex: 'Cabinetul 3 este deja ocupat în acest interval')"* — nu un mesaj generic "conflict").
- [ ] `service/AvailabilityValidatorService.java` (numele e deja anticipat în comentariul din
  `WorkScheduleValidator.java` — vezi contractul, secțiunea 0):
  - `void validate(UUID doctorId, UUID roomId, LocalDateTime start, LocalDateTime end, UUID
    excludeAppointmentId)`:
    1. `appointmentRepository.findOverlappingForDoctor(...)` (metodă P1) — dacă neget, aruncă
       `ResourceConflictException(DOCTOR, "Medicul are deja o programare în acest interval")`.
    2. Același lucru pentru `room` (`findOverlappingForRoom`).
    3. Verifică și orarul de lucru al medicului: intervalul cerut trebuie să fie complet conținut într-un
       `WorkSchedule` al lui (aceeași zi a săptămânii, `startTime`/`endTime` acoperă intervalul) — dacă nu,
       `ResourceConflictException(DOCTOR, "Medicul nu are program în acest interval")`. Refolosiți
       `DoctorRepository`/relația deja încărcată, nu duplicați parsing-ul de orar care există deja în
       `WorkScheduleValidator` dacă are o metodă utilă expusă — dacă nu are, adăugați acolo o metodă
       publică reutilizabilă în loc să copiați logica.
    - **Notă:** verificarea de echipament **nu** e aici — `EquipmentAllocationService` (P3) o face separat,
      pentru că alegerea instanței de echipament e simultan "verificare disponibilitate" ȘI "alocare", nu
      doar o validare binară. Nu duplicați.
  - Apelată din `AppointmentService.create/update` (P1) — voi doi vă coordonați pe semnătura exactă chiar
    din prima zi, ca P1 să poată integra fără să aștepte implementarea completă (livrați semnătura goală
    cu `// TODO` în corp dacă trebuie, ca P1 să compileze).
- [ ] **Teste de concurență** (cerință #4 din Definition of Done, măsurabilă): două thread-uri (sau două
  tranzacții paralele într-un test de integrare) încearcă să rezerve **aceeași** cameră pe **exact
  același interval** simultan — testul trebuie să demonstreze că exact una reușește, cealaltă primește
  409 (fie de la validarea Java, fie de la constrângerea `EXCLUDE` din DB dacă ambele trec de verificarea
  Java în aceeași fereastră de timp — testați cu un `CountDownLatch`/`ExecutorService` ca să forțați
  situația de "amândouă pornesc simultan", nu doar secvențial). Scrieți testul **și** pentru medic **și**
  pentru cameră (2 cazuri separate).

---

## 3. Persoana 3 — `EquipmentAllocationService` (alocare automată echipament)

**Pachete:** `service`

- [ ] `service/EquipmentAllocationService.java`:
  - `Equipment allocate(Service clinicalService, UUID roomId, LocalDateTime start, LocalDateTime end,
    UUID excludeAppointmentId)`:
    1. Dacă `clinicalService.getRequiredEquipmentTypes()` e gol → întoarce `null` imediat (serviciul nu
       cere echipament, ex. "Consult Pediatric" simplu).
    2. Pentru fiecare `EquipmentType` cerut (de regulă unul singur în practică, dar tratați lista generic):
       găsește toate `Equipment` cu acel `equipmentType` și `active = true`
       (`EquipmentRepository.findByEquipmentTypeIdAndActiveTrue`, adăugați-o dacă nu există), apoi
       filtrează-le pe cele **fără** conflict folosind `appointmentRepository.findOverlappingForEquipment`
       (metoda lui P1) pentru fiecare candidat, până găsește una liberă.
    3. Dacă serviciul cere mai multe tipuri de echipament simultan (posibil în teorie, per F-103 —
       "listă de resurse hardware"), pentru moment alocați **doar primul tip** din listă și documentați
       explicit limitarea într-un comment (`Appointment.equipment` e un singur `@ManyToOne`, nu o listă —
       dacă echipa decide că unele servicii chiar au nevoie de 2+ echipamente simultan, e o schimbare de
       schemă care trebuie discutată în grup, nu improvizată aici).
    4. Dacă nu găsește niciun echipament liber din tipul cerut → `ResourceConflictException(EQUIPMENT,
       "Niciun echipament de tip <nume> disponibil în acest interval")` (reutilizează excepția lui P2,
       coordonați pe constructor).
  - Preferință opțională, nu obligatorie: dacă mai multe echipamente sunt libere, preferă unul care are
    deja `room == roomId` cerut (echipament fix în cameră) față de unul mobil din altă cameră — reduce
    nevoia ca cineva să mute fizic aparatul. Dacă implementați asta, un simplu `sorted`/`Comparator`
    înainte de a lua primul liber e suficient, nu supra-inginerați.
  - Apelată din `AppointmentService.create/update` (P1) — coordonați semnătura devreme, la fel ca P2.

---

## 4. Persoana 4 — `CalendarService` (F-301, interogări pentru vederea de calendar)

**Pachete:** `service`, `dto`

- [ ] `dto/CalendarAppointmentResponse.java` — DTO "slim" special pentru randare pe grilă (nu reutilizați
  `AppointmentResponse` întreg dacă are mai multe date decât are nevoie o celulă de calendar):
  ```
  id            : UUID
  doctorId      : UUID
  doctorName    : String        -- "firstName lastName", precalculat, ca frontend să nu mai facă join
  roomId        : UUID
  roomName      : String
  patientName   : String        -- "firstName lastName", NU CNP/alergii/istoric (nu e nevoie pe calendar)
  serviceName   : String
  startTime     : LocalDateTime
  endTime       : LocalDateTime
  status        : AppointmentStatus
  ```
  Coordonați cu P1 dacă vreți să extrageți un mapper comun (`AppointmentSummaryMapper`) sau dacă fiecare
  își scrie propriul `toX()` — nu e critic, dar anunțați decizia în grup ca să nu aveți 2 clase aproape
  identice.
- [ ] `service/CalendarService.java`:
  - `List<CalendarAppointmentResponse> getByDateRangeAndDoctors(LocalDateTime from, LocalDateTime to,
    List<UUID> doctorIds)` — `doctorIds` **opțional** (`null`/gol = toți medicii activi); folosită de
    vederea "toți medicii" din calendar și, esențial, de **filtrul hamburger multi-doctor de la recepție**
    cerut pe frontend (vezi `frontend_module3_tasks.md`) — frontend-ul trimite exact lista de `doctorId`
    bifați în meniul hamburger.
  - `List<CalendarAppointmentResponse> getByDateRangeAndRoom(LocalDateTime from, LocalDateTime to, UUID
    roomId)` — vederea "pe cabinet" cerută explicit de F-301.
  - `List<CalendarAppointmentResponse> getForDoctor(UUID doctorId, LocalDateTime from, LocalDateTime to)`
    — folosită de `AppointmentController` (P5) când rolul curent e `DOCTOR`, ca să forțeze "doar propriul
    calendar" fără să treacă prin filtrul multi-doctor (vezi regula de roluri din contract, secțiunea 0).
  - Toate 3 sunt read-only, fără validare de business — filtrare + mapare, nimic mai mult. Dacă intervalul
    `from`/`to` lipsește sau `from > to`, `400` (`IllegalArgumentException`, mapat deja generic sau
    adăugați un handler dacă nu există).
- [ ] Adaugă finder-ele necesare în `AppointmentRepository` (secțiunea "cine scrie ce" din contract) —
  nu atingeți metodele de overlap ale lui P1.

---

## 5. Persoana 5 (eu, PM) — Controller Layer

**Pachet:** `controller`

- [ ] `AppointmentController.java` — `/api/appointments`

  | Method | Path | Body / Query | Response | Acces |
  |---|---|---|---|---|
  | POST | `/api/appointments` | `AppointmentRequest` | `AppointmentResponse` (201) | ADMIN, RECEPTION |
  | PUT | `/api/appointments/{id}` | `AppointmentRequest` | `AppointmentResponse` (200) | ADMIN, RECEPTION |
  | PATCH | `/api/appointments/{id}/cancel` | — | `AppointmentResponse` (200) | ADMIN, RECEPTION |
  | GET | `/api/appointments/{id}` | — | `AppointmentResponse` | ADMIN, DOCTOR, RECEPTION |
  | GET | `/api/appointments/calendar?from=&to=&doctorIds=&roomId=` | — | `CalendarAppointmentResponse[]` | ADMIN, DOCTOR, RECEPTION |

  Pe ultimul endpoint, logica de "cine vede ce" (regulă din contract, secțiunea 0) trăiește **în
  controller**, nu în service — pentru că depinde de rolul din `SecurityContext`, nu de business logic pur:
  - Dacă `role == DOCTOR`: ignoră `doctorIds`/`roomId` din query, rezolvă `doctorId`-ul curent din
    `Doctor` legat de userul autenticat (`DoctorRepository.findByUserId(...)`, verifică dacă există deja —
    dacă nu, adăugați-o, e trivială) și apelează `calendarService.getForDoctor(doctorId, from, to)`.
  - Dacă `role in (ADMIN, RECEPTION)` și `roomId` prezent: `getByDateRangeAndRoom`.
  - Altfel: `getByDateRangeAndDoctors(from, to, doctorIds)` (doctorIds poate fi gol → toți medicii).
- [ ] Validare param `from`/`to` obligatorii pe `/calendar` (`@RequestParam` fără default, Spring întoarce
  400 automat dacă lipsesc pe un tip non-opțional — verificați că mesajul de eroare rămâne clar).
- [ ] Teste de securitate (măsurabile, cerința #3 din Definition of Done): un `DOCTOR` care încearcă
  `POST /api/appointments` primește 403; un `DOCTOR` care cere `/calendar?doctorIds=<alt-medic>` primește
  **doar propriile programări** (nu 403 — doar ignoră parametrul, testați explicit că răspunsul nu conține
  programări ale altui medic, nu doar codul de status).
- [ ] După ce P1-P4 au semnăturile de service stabile, public `module3_endpoints.md` (același format ca
  [module2_endpoints.md](../module2/module2_endpoints.md)) pentru integrarea cu frontend-ul.

---

## Ordine de lucru recomandată

1. **P1 pornește imediat** — entitatea + cele 3 query-uri de overlap din `AppointmentRepository` sunt
   fundația literală de care depind P2 și P3. Livrează-le în prima zi, chiar înainte de restul serviciului.
2. **P2, P3, P4 pornesc în paralel** imediat ce P1 are entitatea + query-urile de overlap compilabile
   (nu trebuie să aștepte `AppointmentService` complet, doar `Appointment` + `AppointmentRepository`).
3. **P1 integrează** apelurile către `AvailabilityValidatorService` (P2) și `EquipmentAllocationService`
   (P3) în `AppointmentService.create/update` de îndată ce au semnăturile fixate (nu implementarea 100%).
4. **P5 (eu)** încep controller-ul imediat ce toate serviciile au semnături publice stabile — anunțăm în
   grup orice schimbare de semnătură pe parcurs.
5. **P1 scrie migrația `V5__.sql`** ultima, după ce vede schema reală generată de `ddl-auto: update` din
   toate cele 4 seturi de modificări, exact ca la V3/V4.

## Ce NU e în scopul acestui modul (ca să nu ne pierdem)
- Mașina de stări completă (`Confirmat`/`Consultație Activă`/`Finalizat`/`Neprezentat`, validarea
  tranzițiilor, blocarea fișei la finalizare) — Modulul 4.
- Drag-and-drop de schimbare rapidă a statusului din calendar (F-401) — UI-ul poate exista static acum
  (afișare), dar acțiunea de schimbare a statusului vine cu Modulul 4.
- Notificări email/SMS (F-501/F-502) — Modulul 5.
- Rapoarte, rata de ocupare, export PDF/Excel (F-601/F-602) — Modulul 6.
- WebSocket/STOMP pentru sincronizare live reală — rămânem la polling, vezi nota de la începutul
  documentului. Dacă echipa decide ulterior că e nevoie de STOMP, e un modul de infrastructură separat.
