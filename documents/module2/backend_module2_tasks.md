# Modulul 2 — Gestiune Pacienți și Fișe Medicale — Împărțire Task-uri Backend

> Scop: F-201 (Nomenclator Pacienți), F-202 (Fișa de Consultație) + bucățile din NFR-1 care țin
> strict de acest modul (criptare CNP, audit log, RBAC pe datele clinice).
> Nu intră aici: motorul de programări (Modulul 3), mașina de stări completă a programării (Modulul 4),
> notificări (Modulul 5), rapoarte (Modulul 6). Acestea vin în module ulterioare.

**Convenție echipă:** entity → repository → service → DTO se fac de fiecare persoană pe verticala ei.
**Controller-ele** (toate) le fac eu (P5), ca să le pot lega direct de frontend.
**Migrațiile SQL** (Flyway) le face doar P4 — restul lucrează cu `ddl-auto: update` (profil `dev`) și nu ating
fișierele din `db/migration/`.

---

## 0. Contract comun — de citit ÎNAINTE de a scrie cod (P1, P2, P3, P4)

Ca să putem lucra în paralel fără să ne blocăm unii pe alții la integrare, ne fixăm acum forma exactă
a celor 3 entități noi. Dacă cineva descoperă că trebuie schimbat ceva, se anunță în grup înainte de a
modifica — schimbarea afectează minim 2 persoane (owner-ul entității + P4 la migrație, uneori și P5).

### `Patient` (owner: P1)
```
id               : UUID (PK)
firstName        : String, not null, length 100
lastName         : String, not null, length 100
phone            : String, not null, length 20   -- singurele 3 câmpuri obligatorii la creare rapidă
cnp              : String, nullable             -- text CRIPTAT (AES), NU e unic la nivel de coloană
cnpHash          : String, nullable, unique      -- hash determinist al CNP-ului în clar, pt. unicitate + căutare exactă
dateOfBirth      : LocalDate, nullable
email            : String, nullable, length 255
allergies        : String (TEXT), nullable
medicalHistory   : String (TEXT), nullable
profileComplete  : boolean, not null, default false
createdAt        : LocalDateTime, not null
```
**De ce `cnpHash` separat:** AES cu IV/nonce aleator (cum trebuie făcut ca să fie sigur) produce un
ciphertext diferit de fiecare dată chiar pentru același CNP. Dacă am pune `unique` direct pe coloana
criptată sau am căuta cu `WHERE cnp = ...`, nu ar funcționa niciodată. De-asta ținem separat un hash
determinist (SHA-256/HMAC, fără sare aleatoare) doar pentru unicitate și lookup exact — el nu e "PII în
clar" utilizabil (nu se poate inversa), dar e stabil pentru același CNP. `cnp` rămâne coloana criptată,
reversibilă, folosită doar pentru afișare către utilizatori autorizați. `cnpHash` fiind `nullable +
unique`, poate avea oricâte valori `NULL` simultan fără să încalce constrângerea (comportament standard
Postgres — `NULL` nu se consideră egal cu alt `NULL`), deci mai mulți pacienți "incompleți" fără CNP
încă introdus nu se blochează unul pe altul.

**Decizie de business nouă (cerere PM):** recepția înregistrează un pacient nou pe loc (la telefon sau
la ghișeu) doar cu **nume + prenume + telefon** — restul (CNP, data nașterii, email, alergii, istoric)
se completează abia când pacientul vine fizic, completează/semnează formularul pe hârtie, iar recepția
transcrie datele în sistem. Până atunci, pacientul e "cu profil incomplet" și trebuie să iasă în
evidență undeva ca să nu se piardă din vedere — vezi widget-ul de mai jos (F-201 extins, nu e un modul
separat).

`profileComplete` e un flag persistat (nu calculat la fiecare citire), recalculat de service la fiecare
`save`: `true` doar când `cnp` **și** `dateOfBirth` sunt ambele completate (email/alergii/istoric rămân
opționale chiar și pe un profil "complet" — nu orice pacient are alergii cunoscute). Motivul pentru care
e coloană persistată și nu doar getter calculat pe entitate: dashboard-ul are nevoie să filtreze/sorteze
direct în SQL (`WHERE profile_complete = false ORDER BY ...`) pe liste potențial mari, nu să încarce
totul în memorie și să filtreze în Java.

### `ConsultationRecord` (owner: P2)
```
id                   : UUID (PK)
appointmentId        : UUID, not null, unique   -- FĂRĂ FK deocamdată (tabela appointments nu există încă, e Modulul 3)
presentationMotive   : String (TEXT)
anamnesis            : String (TEXT)
clinicalExam         : String (TEXT)
diagnosis            : String (TEXT)
prescription         : String (TEXT)
locked               : boolean, not null, default false
createdAt            : LocalDateTime, not null
updatedAt            : LocalDateTime, not null
```
**Decizie:** F-402 zice "blocat după COMPLETED", dar starea `Appointment.status` nu există încă
(Modulul 3/4). Pentru acest modul, `locked` e un flag local pe `ConsultationRecord` însuși — un
substitut temporar. Service-ul expune un `lock(appointmentId)` intern; când Modulul 3/4 implementează
tranziția către `COMPLETED`, va apela acest hook în loc să reinventăm logica. Nu blocați pe asta acum.

### `AuditLog` (owner: P3) — append-only
```
id           : UUID (PK)
userId       : UUID, not null   -- FK -> users(id)
action       : String (enum AuditAction: CREATE/READ/UPDATE/DELETE — există deja în enums/)
entityName   : String, not null, length 100
entityId     : UUID, not null
timestamp    : LocalDateTime, not null
```
Repository-ul NU extinde `JpaRepository`/`CrudRepository` (astea au `delete*` gratis) — extinde direct
`Repository<AuditLog, UUID>` și declară explicit doar `save(...)` și finder-ele de care avem nevoie, ca
să fie "insert/select only" impus la compilare, nu doar prin convenție.

### Roluri existente (`enums/Role.java`): `ADMIN`, `DOCTOR`, `RECEPTION`
Nu există rol `PATIENT` — pacienții nu se autentifică în sistem, sunt doar date administrate de
clinică. Regula de confidențialitate din persona (Dr. Simona Marin) se referă strict la **fișa de
consultație** (`ConsultationRecord` — diagnostic, anamneză etc.), NU la datele administrative din
`Patient` (nume, telefon, CNP) pe care recepția are nevoie să le vadă/editeze ca să programeze pacienți.
Deci:
- `PatientController` → accesibil oricărui staff autentificat (`ADMIN`, `DOCTOR`, `RECEPTION`).
- `ConsultationController` → `@PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")`, **RECEPTION exclus explicit**.

### Notă despre securitate (context, actualizat)
`SecurityConfig` cere acum JWT valid pe orice endpoint în afară de `/api/auth/**` și `/public/**` (a
fost integrat între timp, PR "Auth Implemented"/`backend-security`). `@EnableMethodSecurity` (P3) tot
trebuie adăugat separat — fără el, `@PreAuthorize`-urile de pe controllere (P5) sunt ignorate complet,
chiar dacă filtrul JWT e activ.

---

## 1. Persoana 1 — Domeniul `Patient` (F-201)

**Pachete:** `entity`, `repository`, `service`, `dto` (folosește structura existentă din `com.holisun.backend`)

- [ ] `entity/Patient.java` — conform contractului de mai sus. Pune `@Convert(converter =
  AesEncryptionUtil.class)` pe câmpul `cnp` — clasa o livrează P3, dar poți compila de pe acum cu un
  stub gol (`encrypt`/`decrypt` = identitate) ca să nu aștepți după el; înlocuiești cu implementarea
  reală când o livrează. Nu uita `@PrePersist` pentru `createdAt` și recalcularea lui `profileComplete`
  (vezi regula din contract) la fiecare `@PrePersist`/`@PreUpdate`, sau explicit în service înainte de
  `save` — alegeți o singură locație, nu duplicați regula în ambele.
- [ ] `repository/PatientRepository.java extends JpaRepository<Patient, UUID>`
  - `Optional<Patient> findByCnpHash(String cnpHash)` — pt. verificare duplicat la creare
  - `List<Patient> searchByNameOrPhoneOrCnp(String keyword)` — caută pe `firstName`/`lastName`/`phone`
    cu `LIKE` (query custom `@Query`); **CNP-ul criptat nu poate intra în acest LIKE** — dacă `keyword`
    arată ca un CNP (13 cifre), caută separat prin `cnpHash` calculat din `keyword` (exact match, nu
    LIKE). Documentează asta într-un comment scurt, e non-evident.
  - `Page<Patient> findByProfileCompleteFalse(Pageable pageable)` (sau `List<>` + `Sort` dacă nu vrem
    paginare încă) — pentru widget-ul de "pacienți incompleți"; `Pageable`/`Sort` acoperă sortarea
    ascendent/descendent pe `lastName` sau pe `createdAt` fără cod suplimentar (Spring Data traduce
    `Sort` direct în `ORDER BY`).
  - `Page<Patient> searchIncompleteByName(String keyword, Pageable pageable)` — varianta cu căutare +
    filtrul de incomplete combinate (widget-ul are și search, per cerința PM), `@Query` custom cu `LIKE`
    pe `firstName`/`lastName` și `profile_complete = false`.
- [ ] `service/PatientService.java` (`@Service`, stil similar cu `RoomService` — vezi
  [RoomService.java](../backend/src/main/java/com/holisun/backend/service/RoomService.java) ca referință de
  stil, interfață separată opțional)
  - `List<PatientResponse> search(String keyword)`
  - `PatientResponse getById(UUID id)` → 404 (`EntityNotFoundException`) dacă nu există
  - `PatientResponse create(PatientQuickCreateRequest dto)` — creare rapidă, doar `firstName`/`lastName`/
    `phone` obligatorii (restul rămân `null`); `profileComplete` iese mereu `false` din acest apel. →
    409 (`IllegalStateException`) dacă `cnpHash` există deja (n-ar trebui să fie cazul aici, dar rămâne
    valabil dacă cineva trimite și CNP direct de la creare, ex. un pacient care vine cu tot pe loc).
  - `PatientResponse update(UUID id, PatientRequest dto)` — folosit atât pentru editare normală cât și
    pentru "completarea" profilului (recepția revine și adaugă CNP/data nașterii/etc. după ce pacientul
    semnează formularul pe hârtie); `profileComplete` se recalculează automat la fiecare `update`, nu
    există un endpoint separat de "marchează complet" — devine `true` de îndată ce `cnp` și
    `dateOfBirth` sunt ambele nenule în urma acelui `update`.
  - `Page<PatientResponse> getIncomplete(String keyword, Pageable pageable)` — sursa de date pentru
    widget-ul de pe dashboard: pacienți cu `profileComplete = false`, opțional filtrați după `keyword`
    (nume), sortabili ascendent/descendent (numele coloanei de sortare vine din `Pageable.getSort()`,
    trimis de frontend ca query param — vezi lista de endpoint-uri la P5).
  - Nu există `delete` — pacienții nu se șterg (păstrare istoric legal), conform diagramei (doar
    GET/POST/PUT în `PatientController`).
- [ ] `dto/PatientQuickCreateRequest.java` — `firstName`, `lastName`, `phone` (`@NotBlank` pe toate 3,
  nimic altceva).
- [ ] `dto/PatientRequest.java` (folosit la `update`/completare) — toate câmpurile din contract, dar
  **fără `@NotNull` pe cnp/dateOfBirth/email/allergii/istoric** (opționale — recepția poate completa
  doar o parte din ele într-o singură trecere, nu trebuie totul dintr-o dată). Validați totuși formatul
  când e prezent (`@Pattern` pe CNP dacă e trimis, `@Past` pe `dateOfBirth` dacă e trimis).
- [ ] `dto/PatientResponse.java` — include și `profileComplete` și `createdAt`, ca frontend-ul să poată
  arăta badge-ul "Incomplet"/sorta widget-ul fără un al doilea request.

**Coordonare cu P3:** te aliniezi cu el pe semnătura exactă a `AesEncryptionUtil` și pe cum se
calculează `cnpHash` (probabil tot P3 expune un `CnpHasher`/metodă statică, ca să fie același algoritm
peste tot).

---

## 2. Persoana 2 — Domeniul `ConsultationRecord` (F-202)

**Pachete:** `entity`, `repository`, `service`, `dto`

- [ ] `entity/ConsultationRecord.java` — conform contractului. Pune `@PrePersist`/`@PreUpdate` pentru
  `createdAt`/`updatedAt`.
- [ ] `repository/ConsultationRecordRepository.java extends JpaRepository<ConsultationRecord, UUID>`
  - `Optional<ConsultationRecord> findByAppointmentId(UUID appointmentId)`
- [ ] `service/ConsultationRecordService.java`
  - `ConsultationRecordResponse getByAppointmentId(UUID appointmentId)` → 404 dacă nu există
  - `ConsultationRecordResponse create(UUID appointmentId, ConsultationRecordRequest dto)` → 409
    (`IllegalStateException`) dacă deja există o fișă pentru acel `appointmentId` (relație 1-1)
  - `ConsultationRecordResponse update(UUID appointmentId, ConsultationRecordRequest dto)` → **409
    (`IllegalStateException`) dacă `locked == true`** — asta e regula critică F-402, scrie un test
    dedicat pentru ea
  - `void lock(UUID appointmentId)` — setează `locked = true`; metodă publică dar nu e apelată de
    nimeni încă în acest modul (hook pentru Modulul 3/4). Documentează cu un comment scurt de ce există.
- [ ] `dto/ConsultationRecordRequest.java`, `dto/ConsultationRecordResponse.java`

**Notă de securitate a conținutului:** service-ul nu trebuie să știe nimic despre roluri — asta se
impune la nivel de `@PreAuthorize` pe controller (P5) + eventual `@Audited` (P3). Tu doar implementezi
regula de business (immutabilitate + 1-1).

---

## 3. Persoana 3 — Infrastructură Securitate & Audit (NFR-1)

**Pachete:** `util` (sau `security`), `aop`, `entity`, `repository`, `config`

- [ ] `util/AesEncryptionUtil.java` — `implements AttributeConverter<String, String>`, `@Converter`.
  AES/GCM cu IV aleator per valoare (nu ECB!). Cheia vine dintr-un `@ConfigurationProperties` nou
  (ex: `EncryptionProperties` cu `secretKey`), citit din `application.yml`/env var — **nu hardcodat**.
- [ ] Un `CnpHasher` (poate fi în aceeași clasă sau una separată, static): HMAC-SHA256 determinist
  (fără IV aleator) folosit pentru `Patient.cnpHash`. Coordonezi direct cu P1 asupra API-ului (ex:
  `CnpHasher.hash(String cnpPlain) : String`).
- [ ] `entity/AuditLog.java` + `repository/AuditLogRepository.java` (vezi contractul — `Repository<>`,
  nu `JpaRepository`/`CrudRepository`)
  - `findByUserAndDateRange(UUID userId, LocalDateTime from, LocalDateTime to)` — folosit de
    `AuditController` (P5)
- [ ] `@interface Audited` (adnotare custom, în `aop`) cu `action: AuditAction`, `entityName: String` —
  ca să nu legăm aspectul de nume de clase de controller (fragil la refactor). Ex:
  ```java
  @Audited(action = AuditAction.READ, entityName = "ConsultationRecord")
  ```
- [ ] `aop/AuditLoggingAspect.java` (`@Aspect @Component`) — `@Around` pe
  `@annotation(com.holisun.backend.aop.Audited)`, extrage `entityId` dintr-un parametru al metodei
  (convenție: primul `UUID` din argumente), userul curent din `SecurityContextHolder` (dacă auth-ul nu
  e gata încă, pune un fallback documentat, ex. `null`/`"system"`, nu arunca excepție), salvează în
  `AuditLogRepository`.
- [ ] Activează `@EnableMethodSecurity` (fie direct pe `SecurityConfig`, fie o clasă nouă de config) —
  fără asta, `@PreAuthorize` pus de P5 pe controllere e complet ignorat.

**Livrezi devreme (nu la final):** semnătura `AesEncryptionUtil` + `CnpHasher` + adnotarea `@Audited`,
ca P1/P2/P5 să se poată agăța de ele fără să aștepte implementarea completă.

---

## 4. Persoana 4 — Migrații SQL (Flyway) — DOAR ATÂT

**Nu atingi** `entity/`, `service/`, `repository/`, `controller/`. Lucrezi exclusiv în
`backend/src/main/resources/db/migration/`.

- [ ] Creezi `V4__.sql` (următorul număr liber după `V3__.sql`), cu tabelele `patients`,
  `consultation_records`, `audit_log` — coloane, tipuri și constrângeri **exact ca în contractul din
  secțiunea 0** de mai sus. Respectă convențiile deja folosite în V1-V3: `pk_<tabel>`,
  `uc_<tabel>_<coloana>`, `fk_<tabel>_on_<referinta>`, `chk_<regula>`.
  - `patients`: doar `first_name`, `last_name`, `phone` sunt `NOT NULL` (creare rapidă de recepție — vezi
    decizia de business din contract). `cnp` ca `TEXT`/`VARCHAR(500)` **nullable** (ciphertext + IV pot
    fi mai lungi decât CNP-ul original, lasă spațiu), `cnp_hash VARCHAR(64) UNIQUE` **nullable** (SHA-256
    hex are 64 caractere — nullable+unique e ok în Postgres, mai mulți `NULL` nu se contrazic între ei),
    `date_of_birth`, `email`, `allergies`, `medical_history` toate nullable (`allergies`/`medical_history`
    ca `TEXT`). Adaugă și `profile_complete BOOLEAN NOT NULL DEFAULT FALSE` și
    `created_at TIMESTAMP NOT NULL DEFAULT now()` — folosite de widget-ul de "pacienți incompleți" de pe
    dashboard (căutare + sortare pe ele, vezi task-urile P1/P5). Un index pe `profile_complete` (parțial,
    `WHERE profile_complete = false`, dacă vreți să optimizați) ajută dacă baza crește mult — nu e
    obligatoriu de la V4, dar notează-l ca opțiune.
  - `consultation_records`: `appointment_id UUID NOT NULL UNIQUE` — **fără FK** (tabela `appointments`
    nu există încă, o adaugă cine face Modulul 3, într-o migrare viitoare).
  - `audit_log`: `user_id UUID NOT NULL`, cu `FK -> users(id)` (tabela `users` există din V1).
- [ ] Nu finaliza coloanele pe baza presupunerilor tale — cere de la P1/P2/P3 numele exacte de coloană
  generate de Hibernate (rulează `dev` profile cu `ddl-auto: update`, uită-te la SQL logat, sau
  întreabă-i direct) înainte să închei PR-ul, exact cum s-a procedat și la `V3__.sql` (vezi comentariul
  din acel fișier: "V3: net-new tables ... plus corrections"). E normal să vină după ceilalți trei, nu
  în paralel de la ora zero.

---

## 5. Persoana 5 (eu, PM) — Controller Layer

**Pachet:** `controller`

- [ ] `PatientController.java` — `/api/patients`
  | Method | Path | Body | Response | Acces |
  |---|---|---|---|---|
  | GET | `/api/patients?search=` | — | `PatientResponse[]` | ADMIN, DOCTOR, RECEPTION |
  | GET | `/api/patients/{id}` | — | `PatientResponse` | ADMIN, DOCTOR, RECEPTION |
  | POST | `/api/patients` | `PatientQuickCreateRequest` | `PatientResponse` (201) | ADMIN, DOCTOR, RECEPTION |
  | PUT | `/api/patients/{id}` | `PatientRequest` | `PatientResponse` (200) | ADMIN, DOCTOR, RECEPTION |
  | GET | `/api/patients/incomplete?search=&sort=lastName,asc` | — | `Page<PatientResponse>` | ADMIN, DOCTOR, RECEPTION |

  Ultimul endpoint alimentează widget-ul de pe dashboard (secțiunea "pacienți de completat" cerută de
  PM) — acceptă `sort` ca parametru standard Spring Data (`<coloană>,<asc|desc>`, ex.
  `lastName,asc` sau `createdAt,desc`); mapează direct pe `Pageable` din controller
  (`@PageableDefault(sort = "createdAt", direction = Sort.Direction.ASC)`), nu reinventa parsing-ul.
  `POST /api/patients` trece acum la `PatientQuickCreateRequest` (doar nume+telefon) — orice completare
  ulterioară de profil se face tot prin `PUT /api/patients/{id}` cu `PatientRequest`, nu printr-un
  endpoint separat de "completează".

- [ ] `ConsultationController.java` — `/api/appointments/{appointmentId}/record`,
  `@PreAuthorize("hasAnyRole('DOCTOR','ADMIN')")` **la nivel de clasă** (RECEPTION exclus — testează
  explicit că ia 403, e cerința #3 din Definition of Done a caietului de sarcini)
  | Method | Path | Body | Response |
  |---|---|---|---|
  | GET | `/api/appointments/{id}/record` | — | `ConsultationRecordResponse` |
  | POST | `/api/appointments/{id}/record` | `ConsultationRecordRequest` | `ConsultationRecordResponse` (201) |
  | PUT | `/api/appointments/{id}/record` | `ConsultationRecordRequest` | `ConsultationRecordResponse` (200) |

  Pune `@Audited(action = ..., entityName = "ConsultationRecord")` pe fiecare metodă (contract de la P3).

- [ ] `AuditController.java` — `/api/audit-log?user&date`, `@PreAuthorize("hasRole('ADMIN')")`, GET only.

- [ ] Teste de securitate (measurable — cerința #3 din §5 Criterii de Acceptanță): un `RECEPTION` nu
  poate accesa `/api/appointments/{id}/record` (403), un `DOCTOR`/`ADMIN` poate.

- [ ] După ce cele de mai sus sunt stabile, public un `module2_endpoints.md` (același format ca
  [module1_endpoints.md](module1_endpoints.md)) pentru integrarea cu frontend-ul.

---

## Ordine de lucru recomandată

1. **P1, P2, P3 pornesc în paralel de acum**, pe baza contractului din secțiunea 0 — nu trebuie să se
   aștepte reciproc, `ddl-auto: update` le generează schema locală automat.
2. **P3 livrează devreme** semnăturile (`AesEncryptionUtil`, `CnpHasher`, `@Audited`) chiar dacă
   implementarea internă nu e completă — P1 și P5 depind de forma lor, nu de conținut.
3. **P5 (eu)** încep controller-ele imediat ce P1/P2 au service-urile cu semnăturile fixate (nu trebuie
   să aștept implementarea 100%, doar interfața publică — dacă schimbăm o semnătură pe parcurs, anunțăm
   în grup).
4. **P4 vine ultimul** — scrie migrarea finală după ce văd schema reală generată de ceilalți trei,
   exact cum s-a întâmplat și la Modulul 1 (`V3__.sql`).

## Ce NU e în scopul acestui modul (ca să nu ne pierdem)
- Motorul de disponibilitate/coliziuni (`AvailabilityValidatorService`, `AppointmentService`) — Modulul 3.
- Mașina de stări completă a programării (`AppointmentStateService`) — Modulul 4.
- Notificări email/SMS — Modulul 5.
- Rapoarte + export PDF/Excel — Modulul 6.
