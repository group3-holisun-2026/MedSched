# Modulul 4 — Gestiune Status și Validare Flux — Împărțire Task-uri Backend

> Scop: F-401 (recepția schimbă rapid statusul unei programări din calendar) + F-402 (fișa de consultație
> se blochează definitiv după finalizarea programării, **cu o perioadă de grație de 30 de minute** — vezi
> decizia de business explicită de mai jos) + partea din secțiunea 2.2 a caietului (mașina de stări
> completă a programării).
>
> **Nu intră aici:**
> - Alocarea automată de echipament (F-103/F-302, `EquipmentAllocationService`) — a rămas neimplementată
>   din Modulul 3 (`Appointment.equipment` e mereu `null` azi, vezi status codebase mai jos). E restanță de
>   Modul 3, nu o preluăm aici — dacă echipa vrea să o repare, e un task separat.
> - Notificări email/SMS la schimbare de status (F-501/F-502, ex. "Modificare/Anulare: notificare
>   instantanee dacă un medic devine indisponibil") — Modulul 5. Aici doar schimbăm statusul; cine trimite
>   notificarea pe schimbare e treaba Modulului 5 (poate agăța un listener pe evenimentele noastre mai
>   târziu, nu implementăm noi hook-uri speculative acum).
> - Rapoarte de utilizare/no-show (F-601) — Modulul 6. Statusurile pe care le scriem aici (`NO_SHOW`,
>   `COMPLETED`) sunt sursa de adevăr pe care se va baza raportul, dar raportul propriu-zis nu e în scop.
> - WebSocket/STOMP — rămânem la polling (deja stabilit în Modulul 3, `CalendarPage` reface fetch la 20s).

---

## 0. Status curent al codebase-ului (verificat direct în cod, nu presupus) — CITIȚI ÎNAINTE DE A ÎNCEPE

Asta contează mai mult ca de obicei la acest modul, pentru că am găsit discrepanțe reale între ce e pe
`main` și ce a construit deja echipa:

1. **Backend-ul Modulului 3 NU e mergeuit pe `main`.** Tot ce ține de `AppointmentController`,
   `AvailabilityValidatorService`, `CalendarService`, `ResourceConflictException` există doar pe branch-ul
   `module3_backend` (toate PR-urile #130-#138 au avut ca target `module3_backend`, nu `main` — verificat cu
   `gh pr view`). Pe `main`, `AppointmentService.create()` are încă `// TODO(P2)` / `// TODO(P3)` necompletate
   și **nu există deloc `AppointmentController`**. **Modulul 4 pornește din `module3_backend`, nu din
   `main`** — fie cineva face întâi merge-ul `module3_backend → main`, fie lucrați direct pe un branch nou
   pornit din `module3_backend`. Anunțați explicit în grup ce ați ales, ca să nu se piardă munca de la
   Modulul 3 într-un merge greșit.
2. **Nu există migrație Flyway pentru tabela `appointments`.** `V5__.sql` (pe `main`) creează
   `audit_log`/`consultation_records`/`patients`, dar nimeni nu a mai scris migrația pentru `appointments`
   promisă în contractul Modulului 3 (tabela + cele 3 constrângeri `EXCLUDE`). În dev merge cu
   `ddl-auto: update`, dar `application-prod.yml` are `ddl-auto: validate` — **în producție, pornirea
   aplicației ar eșua azi** (Hibernate validează schema așteptată față de cea reală și nu găsește tabela).
   Preluăm asta la P1 (secțiunea 1), pentru că oricum atingem același tabel pentru `completed_at`.
3. **`ConsultationRecord.lock(UUID appointmentId)` există deja** (scris în Modulul 2, cu comentariul
   explicit "Hook for Module 3/4") **dar nu e apelat de nimeni în cod de producție** — doar dintr-un test.
   Exact acest apel e inima F-402 la noi.
4. **`AppointmentService.cancel()` și `.update()` există deja** (pe `module3_backend`) și blochează
   deja `COMPLETED`/`CANCELLED` — dar nu și `IN_PROGRESS`/`NO_SHOW`. Le strângem în secțiunea 1.

---

## 1. Decizii de business fixate acum (ca să nu discutăm variante diferite în paralel)

### 1.1. Mașina de stări — tranzițiile exacte permise

Diagrama din caiet (secțiunea 2.2) e citită strict, fără tranziții "bonus" neindicate acolo:

```
SCHEDULED  -> CONFIRMED   (recepția confirmă)
SCHEDULED  -> CANCELLED
CONFIRMED  -> IN_PROGRESS (pacientul intră în cabinet)
CONFIRMED  -> NO_SHOW     (ora a trecut, pacientul nu s-a prezentat)
CONFIRMED  -> CANCELLED
IN_PROGRESS -> COMPLETED  (medicul finalizează consultul)
```
Orice altă pereche (inclusiv orice tranziție înapoi, orice salt peste o stare, orice acțiune pe o stare
terminală `COMPLETED`/`CANCELLED`/`NO_SHOW`) e respinsă cu **409**. Nu există tranziție automată
`CONFIRMED -> NO_SHOW` doar pentru că a trecut ora — rămâne o acțiune manuală de la recepție (F-401 o descrie
explicit ca acțiune de recepție "din ecranul principal de calendar", nu ca job automat; dacă echipa vrea
detecție automată de no-show mai târziu, e o discuție separată, nu o introducem speculativ acum).

### 1.2. Cine poate declanșa fiecare tranziție

F-401 vorbește explicit despre recepție, dar `Confirmă`/`Cabinetul` din diagramă nu descriu cine apasă
"pacientul a intrat" sau "finalizează consultul" — le fixăm aici, coerent cu persona Dr. Simona (folosește
tableta *în cabinet*) și cu regula deja stabilită în Modulul 3 (un `DOCTOR` e mereu forțat pe *propriile*
programări, verificat din JWT, nu din ce trimite clientul):

| Tranziție | Endpoint | Rol(uri) |
|---|---|---|
| `SCHEDULED -> CONFIRMED` | `PATCH /confirm` | `ADMIN`, `RECEPTION` |
| `CONFIRMED -> IN_PROGRESS` | `PATCH /check-in` | `ADMIN`, `RECEPTION`, `DOCTOR` (doar propria programare) |
| `CONFIRMED -> NO_SHOW` | `PATCH /no-show` | `ADMIN`, `RECEPTION` |
| `* -> CANCELLED` | `PATCH /cancel` (există deja) | `ADMIN`, `RECEPTION` |
| `IN_PROGRESS -> COMPLETED` | `PATCH /complete` | `ADMIN`, `DOCTOR` (doar propria programare) |

Motivul pentru care `COMPLETED` e restricționat la medicul alocat (nu recepție): finalizarea consultului e
legată direct de salvarea fișei clinice (F-202) — recepția n-are voie să "închidă" un consult pe care nu
l-a făcut ea. Verificarea "doar propria programare" pentru `DOCTOR` se face **exact ca la
`AppointmentController.getCalendar` din Modulul 3** — rezolvi `Doctor` din `doctorRepository.findByUserId`,
compari cu `appointment.getDoctor().getId()`, 403 dacă nu coincid (nu 404 — vrem mesaj clar, nu ambiguitate).

### 1.3. F-402 — blocarea fișei, cu grație de 30 de minute (precizare explicită de business)

**Nu blocăm fișa instant la `COMPLETED`.** Motiv (dat explicit): dacă un consult se lungește peste ora
programată, medicul trebuie să poată reveni și completa/corecta fișa fără presiune, fără să fie forțat să
scrie totul în grabă exact în secunda în care apasă "Finalizează". Regula fixată:

- La tranziția `IN_PROGRESS -> COMPLETED`, `Appointment` primește un timestamp nou, `completedAt = now()`
  (câmp nou, vezi contractul P1). **Nu se apelează `consultationRecordService.lock()` în acest moment.**
- Fișa rămâne editabilă (`locked = false`) până la `completedAt + 30 minute`. Un job programat (P2,
  secțiunea 3) verifică periodic și apelează `lock()` abia după ce trec cele 30 de minute.
- Dacă un consult nu are deloc fișă creată până la expirarea celor 30 de minute, `lock()` tot se apelează
  pe orice fișă care există la momentul respectiv (dacă nu există fișă, hook-ul P2 nu are ce bloca — rămâne
  posibil ca medicul să creeze fișa după expirare, dar odată creată nu mai poate fi editată; e o gaură
  minoră acceptată, semnalată explicit aici ca să nu fie surpriză — dacă echipa vrea `POST` blocat și el
  după expirare, discutați separat, nu e cerut explicit de caiet).

---

## 1. Persoana 1 — `Appointment` (stare, migrație) + `AppointmentStateMachine`

**Pachete:** `entity`, `service`, `db/migration`

- [ ] `entity/Appointment.java` — adaugă `completedAt : LocalDateTime, nullable` (setat o singură dată, la
  tranziția spre `COMPLETED`, niciodată modificat după — nu are `@PreUpdate`, se setează explicit în
  service).
- [ ] `service/AppointmentStateMachine.java` (nou, `@Component`) — sursă unică de adevăr pentru tranzițiile
  valide (secțiunea 1.1), ca să nu fie duplicată logica de `switch` pe mai multe metode din
  `AppointmentService`:
  ```java
  private static final Map<AppointmentStatus, Set<AppointmentStatus>> ALLOWED = Map.of(
      SCHEDULED,   Set.of(CONFIRMED, CANCELLED),
      CONFIRMED,   Set.of(IN_PROGRESS, NO_SHOW, CANCELLED),
      IN_PROGRESS, Set.of(COMPLETED),
      COMPLETED,   Set.of(),
      NO_SHOW,     Set.of(),
      CANCELLED,   Set.of()
  );

  public void assertTransition(AppointmentStatus from, AppointmentStatus to) {
      if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
          throw new ResponseStatusException(HttpStatus.CONFLICT,
              "Tranziția din " + from + " în " + to + " nu este permisă.");
      }
  }
  ```
  Folosiți `ResponseStatusException` direct (nu o excepție custom nouă) — e exact stilul deja folosit de
  `AppointmentService.update()`/`.cancel()` pe `module3_backend`, nu introduceți un al doilea tipar de
  eroare pentru același modul.
- [ ] `service/AppointmentService.java` — adaugă metodele de tranziție, fiecare apelând
  `stateMachine.assertTransition(appointment.getStatus(), <target>)` înainte de `save`:
  - `confirm(UUID id)` → `CONFIRMED`.
  - `checkIn(UUID id, UUID callerUserId, boolean isDoctorRole)` → `IN_PROGRESS`. Dacă `isDoctorRole`,
    verifică (prin `DoctorRepository.findByUserId`) că `appointment.getDoctor().getId()` corespunde
    medicului curent → 403 altfel (verificarea de rol/ownership stă în controller, la fel ca la
    `/calendar` în Modulul 3 — vezi P5; metoda de service primește deja `doctorId`-ul rezolvat, nu
    userId-ul brut, ca service-ul să nu depindă de `SecurityContext`).
  - `complete(UUID id, UUID callingDoctorId)` → `COMPLETED`; setează `completedAt = LocalDateTime.now()`.
    **Nu apelează `consultationRecordService.lock()` aici** (vezi decizia 1.3 — blocarea vine din job-ul
    P2, nu sincron la tranziție).
  - `noShow(UUID id)` → `NO_SHOW`.
  - **Modifică `cancel()` existent**: restrânge sursele valide la `SCHEDULED`/`CONFIRMED` (folosind
    `stateMachine.assertTransition`, elimină `switch`-ul vechi care doar bloca `COMPLETED`/`CANCELLED` —
    acum blochează implicit și `IN_PROGRESS`/`NO_SHOW`, corect conform diagramei).
  - **Modifică `update()` existent** (reprogramare): la fel, restrânge la `SCHEDULED`/`CONFIRMED` — nu se
    poate reprograma o consultație aflată `IN_PROGRESS`.
- [ ] `db/migration/V7__.sql` (verificați numărul exact contra `main` în momentul în care începeți, dacă
  între timp `module3_backend` s-a mergeuit și a adus `V6__.sql` — vezi status codebase, punctul 2):
  - **Preluați restanța Modulului 3**: `CREATE TABLE appointments (...)` cu toate coloanele din contractul
    original (vezi `backend_module3_tasks.md`, secțiunea 0), FK-uri, `CREATE EXTENSION IF NOT EXISTS
    btree_gist;` + cele 3 constrângeri `EXCLUDE` (doctor/room/equipment) — nimeni nu le-a scris până acum,
    iar fără ele garanția "matematică" anti-coliziune din NFR-2 nu există de fapt în producție (doar
    verificarea Java o mai acoperă, ceea ce contractul original spunea explicit că nu e suficient sub
    concurență reală).
  - Adaugă coloana nouă `completed_at TIMESTAMP WITHOUT TIME ZONE` (nullable).
  - Un index parțial pentru query-ul job-ului de blocare (P2): pe `(status, completed_at)` filtrat
    `WHERE status = 'COMPLETED'` — tabela va crește, iar job-ul rulează des (vezi P2), merită indexul de la
    început.

---

## 2. Persoana 2 — Blocarea fișei (F-402) + restricția "doar medicul alocat"

**Pachete:** `service`, `controller`, `config`

- [ ] `BackendApplication.java` — adaugă `@EnableScheduling` (nu există deloc azi în proiect — sunteți
  primii care introduc `@Scheduled`, verificat: zero adnotări `@Scheduled`/`@EnableScheduling` în tot
  codul).
- [ ] `service/ConsultationRecordLockScheduler.java` (nou) — `@Component`, o metodă `@Scheduled(fixedRate =
  60_000)` (o dată pe minut e suficient de des pentru o grație de 30 de minute, nu are sens mai des):
  - Interoghează `appointmentRepository` pentru programări `status = COMPLETED AND completedAt <= now() -
    30 minute` (adăugați metoda de repository necesară — filtrare simplă, fără join complex).
  - Pentru fiecare, apelează `consultationRecordService.lock(appointmentId)` — hook-ul deja există din
    Modulul 2, e idempotent (verifică `if (!record.isLocked())` înainte de `save`), deci re-rularea pe
    aceeași programare la runda următoare nu strică nimic dacă nu ați marcat-o altfel ca "procesată" —
    **nu e nevoie de un flag suplimentar "processed"**, interogarea `status = COMPLETED AND completedAt <=
    ...` tot le-ar regăsi la infinit altfel; ca să evitați asta, verificați în service (sau în query) că
    fișa asociată nu e deja `locked` înainte de a o include în lot (`JOIN consultation_records cr ON
    cr.appointment_id = a.id WHERE ... AND cr.locked = false`, sau echivalent în JPQL/query derivat).
  - Prindeți excepțiile per-programare (dacă `lock()` aruncă `EntityNotFoundException` pt. o programare
    fără fișă creată încă — vezi decizia 1.3, e un caz acceptat, nu o tratați ca eroare de log zgomotos,
    doar `continue` la următoarea).
- [ ] **"Doar medicul alocat poate edita fișa"** (text explicit din caiet la starea `IN_PROGRESS`) — azi
  `ConsultationController`/`ConsultationRecordServiceImpl` permit oricărui `DOCTOR` să editeze fișa oricărei
  programări, nu doar a propriilor pacienți (verificat: nicio verificare de ownership în cod). Adăugați
  verificarea în `ConsultationRecordServiceImpl.create/update` (sau într-un pas nou în controller, discutați
  cu P5 unde e mai natural): rezolvați `Appointment` după `appointmentId` (are nevoie de
  `AppointmentRepository` ca dependință nouă în acest service), comparați `appointment.getDoctor().getId()`
  cu medicul curent (din JWT) — dacă rolul e `DOCTOR` și nu coincide, `403`. `ADMIN` rămâne exceptat (poate
  edita orice fișă, ca acum).
- [ ] **Teste** (vezi și P3, dar scrieți-le pe ale voastre aici, nu le lăsați doar pe seama P3):
  - Job-ul de blocare: folosiți un `Clock` injectabil (sau setați direct `completedAt` în trecut într-un
    test de integrare) — verificați că o programare cu `completedAt` acum 31 de minute devine `locked =
    true` după o rulare a job-ului, iar una cu `completedAt` acum 10 minute rămâne `locked = false`.
  - Un `DOCTOR` diferit de cel alocat primește 403 la `PUT
    /api/appointments/{id}/record` — chiar dacă rolul lui e `DOCTOR` valid.

---

## 3. Persoana 3 — Teste de mașină de stări (cerința #2 și #4 din Definition of Done)

**Pachete:** `test`

DoD-ul modulului cere explicit ca mașina de stări "să nu poată fi ocolită prin operațiuni directe
neautorizate în baza de date sau API" — asta se demonstrează cu teste, nu se presupune:

- [ ] Test parametrizat pe **toate cele 36 de perechi** `(from, to)` din `AppointmentStatus × AppointmentStatus`
  — pentru fiecare pereche NEpermisă (conform tabelului din secțiunea 1.1), verifică că endpoint-ul
  corespunzător (sau apelul direct de service, dacă nu există un endpoint 1:1 pentru fiecare pereche —
  atunci testați `AppointmentStateMachine.assertTransition` direct, unitar, e mai simplu și acoperă
  matricea completă fără să inventați endpoint-uri care nu există) aruncă 409. Pentru perechile permise,
  verifică că trece.
- [ ] Test de securitate (`@WebMvcTest`, la fel ca `AppointmentControllerSecurityTest` din Modulul 3):
  - `RECEPTION` pe `PATCH /api/appointments/{id}/complete` → 403.
  - `DOCTOR` (alt medic decât cel alocat) pe `PATCH /api/appointments/{id}/complete` → 403.
  - `DOCTOR` (medicul alocat) pe `PATCH /api/appointments/{id}/complete` → 200.
  - `RECEPTION` pe `PATCH /api/appointments/{id}/confirm` și `/no-show` → 200 (au voie).
- [ ] Test de integrare: o programare ajunsă `COMPLETED` nu mai poate fi mutată în `update()`
  (reprogramare) — 409, nu 200 cu date schimbate silențios.
- [ ] Coordonați cu P1/P2 pe măsură ce scriu metodele — nu așteptați ca totul să fie "gata" înainte să
  începeți, scrieți testele contra semnăturilor stabilite din capul locului (același flux ca la Modulul 3).

---

## 4. Persoana 5 (eu, PM) — Controller Layer

**Pachet:** `controller`

- [ ] Extinde `AppointmentController.java` (`/api/appointments`) cu endpoint-urile din tabelul 1.2:

  | Method | Path | Response | Acces |
  |---|---|---|---|
  | PATCH | `/api/appointments/{id}/confirm` | `AppointmentResponse` (200) | ADMIN, RECEPTION |
  | PATCH | `/api/appointments/{id}/check-in` | `AppointmentResponse` (200) | ADMIN, RECEPTION, DOCTOR (propria) |
  | PATCH | `/api/appointments/{id}/no-show` | `AppointmentResponse` (200) | ADMIN, RECEPTION |
  | PATCH | `/api/appointments/{id}/complete` | `AppointmentResponse` (200) | ADMIN, DOCTOR (propria) |
  | PATCH | `/api/appointments/{id}/cancel` | `AppointmentResponse` (200) | ADMIN, RECEPTION (existent, doar tăiat la sursele noi restrânse de P1) |

  Pentru `check-in`/`complete`, logica de "doar propria programare" pentru `DOCTOR` trăiește **în
  controller** (exact ca la `/calendar` în Modulul 3, aceeași justificare: depinde de `SecurityContext`, nu
  de business logic pur) — rezolvați medicul curent din `doctorRepository.findByUserId(currentUserId())` și
  paseasă `doctor.getId()` mai departe la service ca să compare, în loc să lăsați service-ul să interogheze
  `SecurityContextHolder` direct.
- [ ] `AppointmentResponse` deja are `equipment`/etc. — adăugați `completedAt` în DTO (nullable, `null`
  pentru orice status diferit de `COMPLETED`) ca frontend să poată afișa contorul de 30 de minute (vezi
  `frontend_module4_tasks.md`).
- [ ] Publică `module4_endpoints.md` (același format ca `module2_endpoints.md`/`module3_endpoints.md`) după
  ce P1/P2 au semnăturile de service stabile.

---

## Ordine de lucru recomandată

1. **Rezolvați întâi punctul 0.1** — decideți ca echipă dacă mergeuiți `module3_backend` pe `main` acum sau
   lucrați direct pe un branch nou pornit din `module3_backend`. Fără asta, oricine pornește de pe `main`
   riscă să reimplementeze de la zero controller-ul/validatorul din Modulul 3.
2. **P1 pornește imediat** cu `AppointmentStateMachine` + modificările pe `AppointmentService` — e
   fundația de care depinde tot restul (P2 pentru `complete`, P5 pentru controller).
3. **P2 pornește în paralel**, independent de P1 la început (job-ul de scheduling și verificarea de
   ownership pe `ConsultationRecordServiceImpl` nu depind de `AppointmentStateMachine`), dar are nevoie de
   `completedAt` pe `Appointment` (P1) înainte de a termina job-ul — coordonați ziua 1.
4. **P3 scrie testele matricei de tranziții contra `AppointmentStateMachine` direct**, nu așteaptă
   controller-ul complet de la P5.
5. **P5 (eu)** leg controller-ul de îndată ce P1 are metodele de service cu semnături stabile.
6. **P1 scrie migrația `V7__.sql` ultima** (inclusiv restanța de la Modulul 3), după ce vede schema reală
   generată de `ddl-auto: update` cu toate modificările de mai sus incluse — exact ca la V5/V6.

## Ce NU e în scopul acestui modul (ca să nu ne pierdem)
- Alocare automată de echipament (`EquipmentAllocationService`) — restanță de Modul 3, separată.
- Detecție automată de no-show la trecerea orei (fără acțiune de recepție) — nu e cerută explicit de F-401,
  nu o introducem speculativ.
- Notificări la schimbare de status (F-501/F-502) — Modulul 5.
- Rapoarte de utilizare/no-show (F-601/F-602) — Modulul 6.
