# Frontend — Ce mai e de făcut pentru Modulul 1 și Modulul 2

> Stare la 2026-07-15. Nu e o împărțire pe persoane — e un TODO în ordine de prioritate, ca oricine
> liber să poată lua următorul item nescris de altcineva. Bifează pe măsură ce se rezolvă.

## 0. Ce există deja (ca să nu reinventăm)

**Merged pe `main`:**
- Routing de bază (`App.jsx`) cu `/login` și `/dashboard`, `AuthProvider` schelet (`AuthContext.jsx`)
  cu `login/logout/tryRefresh/authFetch` — dar `LoginPage` e încă un placeholder gol pe `main` (formularul
  real e într-un PR nemerge-uit, vezi mai jos).
- `ToastContext`/`ToastContainer` (FE-UX-002), montat în `main.jsx` — dar **nimic nu-l folosește încă**
  (niciun `useToast()` în afara definiției lui). E infrastructură gata, neconectată.
- 12 fișiere goale (0 bytes) — `pages/Doctor/{DoctorPage,DoctorForm,DoctorTable}.jsx`, la fel pentru
  `Rooms/`, `Equipment/`, `ClinicalService/`. Astea sunt doar schelet de nume de fișier dintr-un commit
  de breakdown ("Adaugat breakdown frontend Modulul 1"), **nu conțin nimic** — tot Modulul 1 e de scris
  de la zero pe partea de UI, deși backend-ul e complet (Doctor/Room/Equipment/EquipmentType/ClinicalService).
- Backend-ul cere acum JWT (`Bearer <token>`) pe **orice** endpoint în afară de `/api/auth/register`,
  `/api/auth/login`, `/api/auth/refresh` și `/public/**` (`SecurityConfig`, mers pe `main` prin PR #112
  "Auth Implemented", `backend-security`). Asta înseamnă că nicio pagină CRUD (Doctor/Room/Equipment/
  Service) nu va funcționa până nu există un client HTTP care atașează token-ul corect (vezi §1.1).

**Deschise, ne-merge-uite (relevante pentru Modulul 1):**
| PR | Titlu | Conține |
|---|---|---|
| [#111](../../pull/111) `alex/module1-login-auth` | LoginPage, PrivateRoute și Navbar cu logout (FE-AUTH-001) | Formular real de login, `PrivateRoute.jsx`, `Navbar` cu link-uri condiționate pe rol |
| [#109](../../pull/109) `andraModule1` | FEsetup003 | Tailwind v4 + `postcss.config.js` + kit UI de bază: `Button`, `Card`, `Input`, `Modal` |
| [#105](../../pull/105) `alex/module1-fixes` | Alex/module1 fixes | **Pare depășit** — conține commit-uri deja intrate pe `main` prin #106/#107/#108 (backend Doctor/Room/Equipment/ClinicalService, ToastContext). Verificați dacă mai aduce ceva nou înainte să-l lucrați; altfel închideți-l ca fiind duplicat, nu-l lăsați "open" fără rost. |

Niciunul din aceste 3 PR-uri nu e mergeuit — pân-atunci, oricine pornește o pagină CRUD nouă nu are
nici formular de login funcțional, nici componentele de bază (`Button`/`Input`/`Card`), nici stilurile
Tailwind. **Primul lucru de făcut e să treceți #109 și #111 prin review și să le integrați** (vezi §1).

---

## 1. Blocante — de rezolvat înainte de orice pagină nouă

### 1.1. Doi clienți HTTP diferiți, cu chei de token diferite (bug real, nu ipotetic)
- `context/AuthContext.jsx` salvează token-ul în `localStorage` sub cheile `"accessToken"` /
  `"refreshToken"`, și expune un `authFetch()` propriu (fetch nativ + retry pe 401).
- `services/apiClient.js` (axios) citește token-ul din `localStorage.getItem('token')` — **cheie
  diferită**, deci interceptor-ul lui nu va găsi niciodată token-ul salvat de `AuthContext` după login.
- **Rezultat:** orice pagină care va folosi `apiClient` (axios) pentru CRUD (Doctor/Room/Equipment/
  Service/Patient) va trimite requesturi fără `Authorization`, care acum iau 401 de la backend (vezi
  §0 — totul e protejat de JWT).
- **De făcut:** alegeți UN singur client HTTP (recomandare: păstrați `apiClient.js`/axios, e mai
  potrivit pentru interceptors + retry global, dar mutați `AuthContext` să scrie/citească aceeași cheie
  de `localStorage`, sau — mai curat — treceți `apiClient` să citească `"accessToken"` din context/
  storage, nu `"token"`). Ștergeți/consolidați `api/api.js` (fetch nativ, doar auth) în același loc.

### 1.2. `module1_endpoints.md` e depășit — nu construiți pe el direct
Am comparat [module1_endpoints.md](module1_endpoints.md) cu DTO-urile reale din backend (`dto/*.java`)
și au divergat de când a fost scris documentul:

| În `module1_endpoints.md` (vechi) | În cod, acum |
|---|---|
| `DoctorCreateRequest.specialty` | `speciality` (cu "i" — typo păstrat intenționat, verifică cu backend înainte să "corectezi") |
| `DoctorCreateRequest.contactInfo` | **nu există** pe backend |
| `DoctorResponse` fără `userId` | `DoctorResponse.userId` există |
| `weeklySchedule: WorkingHoursDto[]` | câmpul se numește `schedule` |
| `WorkingHoursDto.dayOfWeek: "MONDAY"\|"TUESDAY"\|...` (string) | `dayOfWeek: Short` (1–7, cu `@Min(1)@Max(7)`), NU string |
| `ClinicalServiceRequest.requiredEquipmentIds: string[]` | `requiredEquipmentTypeIds` — leagă de **tipuri** de echipament, nu de echipamente individuale |
| `ClinicalServiceResponse.requiredEquipment: EquipmentResponse[]` | `requiredEquipmentTypes: EquipmentTypeResponse[]` |
| `EquipmentRequest { name }` | are și `equipmentTypeId` (obligatoriu) și `roomId` (opțional) |
| `EquipmentResponse { id, name, active }` | are și `equipmentTypeId`, `roomId` |
| lipsește complet | există `EquipmentTypeController` (`/api/equipment-types`, CRUD complet) — o entitate nouă, nedocumentată, necesară ca să poți crea Equipment/Service din UI |

**De făcut:** cineva regenerează `module1_endpoints.md` citind direct DTO-urile din
`backend/src/main/java/com/holisun/backend/dto/`, înainte ca altcineva să înceapă un formular pe baza
lui. Altfel pierdem timp pe formulare care trimit shape-ul greșit.

### 1.3. Conflict între PR #109 și #111
Ambele rescriu `frontend/src/index.css` în moduri incompatibile (#109 îl înlocuiește complet cu tema
Tailwind, #111 doar editează fișierul vechi). Cine le integrează pe amândouă trebuie să rezolve
conflictul manual — probabil păstrând varianta Tailwind din #109 și reaplicând peste ea micile
modificări din #111 (line-height pe `h1`, etc.), nu invers.

### 1.4. Nepotrivire de nume de rol în `Navbar` (din PR #111)
`Navbar`-ul din #111 verifică `role === 'ADMINISTRATOR'`, `'MEDIC'`, `'RECEPTIONIST'`. Enum-ul real de
pe backend (`enums/Role.java`, folosit și în `UserResponse.role`) are valorile **`ADMIN`, `DOCTOR`,
`RECEPTION`**. Cu string-urile din PR, niciun link condiționat pe rol nu s-ar afișa vreodată. De
corectat la merge, nu după.

### 1.5. Fișier parazit în PR #111
A fost commit-uit accidental un fișier literal numit `how --stat af1fb83` (arată a `git show --stat`
scris greșit și redirecționat într-un fișier în loc de terminal). De șters înainte de merge.

### 1.6. TODO-uri învechite în `api/api.js`
Comentariile zic că `/auth/login`, `/auth/refresh`, `/auth/logout` "nu sunt încă implementate pe
backend" — sunt (PR #112, mergeuit). Shape-ul răspunsului (`accessToken`, `refreshToken`, `user`) chiar
se potrivește cu `AuthResponse`/`UserResponse` de pe backend, deci nu trebuie schimbată logica, doar
șterse comentariile stale ca să nu inducă în eroare pe cineva nou.

---

## 2. TODO Modulul 1 (Administrare Resurse Clinică) — ca să fie "gata"

Backend-ul e complet (F-101/F-102/F-103). Pe frontend nu există nimic funcțional încă — de construit:

- [ ] Rezolvă blocantele din §1 (client HTTP unic, contract DTO proaspăt, merge #109 + #111 fără conflicte).
- [ ] **Pagină + tabel + formular Doctori** (`pages/Doctor/*`) — listă, creare, editare, ștergere,
  inclusiv editorul de `schedule` (`WorkingHoursDto[]`: zi 1–7 + interval orar). `DoctorCreateRequest`
  cere `userId` — clarificați cu backend cum se alege userul (dropdown din `/api/auth`? nu există încă
  un endpoint de listare useri — posibil blocaj de coordonat cu backend).
- [ ] **Pagină + tabel + formular Cabinete** (`pages/Rooms/*`) — CRUD simplu (`name`, `description`).
- [ ] **Pagină + tabel + formular Tipuri de Echipament** (nu există pagină deloc, entitate nedocumentată
  în vechiul contract) — CRUD simplu (`name`, `description`), necesară înainte de Equipment.
- [ ] **Pagină + tabel + formular Echipamente** (`pages/Equipment/*`) — CRUD, cu select de
  `equipmentTypeId` (obligatoriu) și `roomId` (opțional, echipament mobil fără cameră fixă).
- [ ] **Pagină + tabel + formular Servicii Clinice** (`pages/ClinicalService/*`) — CRUD, cu multi-select
  de `requiredEquipmentTypeIds` (validare: cel puțin unul).
- [ ] **Rutare**: adaugă toate paginile de mai sus în `App.jsx`, în spatele `PrivateRoute` (din #111),
  cu link-uri de navigare (probabil sub un meniu "Administrare", vizibil doar pentru `ADMIN`, per NFR-1).
- [ ] **Erori vizuale**: folosește `ToastContainer`/`useToast()` (există, dar neconectat) pentru mesaje
  de succes/eroare la create/update/delete — cerința NFR-3 explicit interzice `alert()`/`confirm()`
  native ale browserului.
- [ ] **Loading/empty states** minime pe tabele (deocamdată nu există nimic construit, deci implicit
  nici stările astea).
- [ ] Decizie: are sens un ecran/rută de `/register` pentru `/api/auth/register`, sau conturile se
  creează doar de admin din alt flux? În lipsa unei pagini, endpoint-ul de pe backend rămâne neatins de
  frontend.

## 3. TODO Modulul 2 (Gestiune Pacienți și Fișe Medicale) — de la zero

Backend-ul pentru Modulul 2 (`Patient`, `ConsultationRecord`, `AuditLog`) **nu există încă** — vezi
[backend_module2_tasks.md](backend_module2_tasks.md) pentru împărțirea pe backend. Pe frontend nu s-a
scris nimic (zero fișiere/referințe la "patient" sau "consultation" în `frontend/src`), deci acesta e
punctul de plecare, nu o listă de reparații:

- [ ] **Decizie de design confirmată (cerere PM) — profil de pacient în doi pași:** recepția nu
  completează fișa întreagă a pacientului pe loc. La telefon/ghișeu se creează pacientul doar cu
  **nume, prenume, telefon** (`POST /api/patients` cu `PatientQuickCreateRequest` — vezi
  `backend_module2_tasks.md`); CNP-ul, data nașterii, email-ul, alergiile și istoricul se completează
  abia când pacientul vine fizic, completează și semnează formularul pe hârtie, iar recepția transcrie
  datele (`PUT /api/patients/{id}` cu restul câmpurilor). Până atunci pacientul e "cu profil incomplet"
  (`PatientResponse.profileComplete === false`). Din asta rezultă 3 piese de UI, nu doar una:
  1. **Formular rapid de adăugare** (poate fi un `Modal`, nu neapărat o pagină întreagă) — doar 3
     câmpuri (nume, prenume, telefon), accesibil de oriunde din flux (pagina de pacienți, dar și direct
     din widget-ul de mai jos, ca acțiune "Adaugă pacient nou" în timp ce ești acolo).
  2. **Widget "Pacienți de completat" deasupra `DashboardPage`** — listă/tabel cu pacienții unde
     `profileComplete === false`, alimentat de `GET /api/patients/incomplete?search=&sort=`. Trebuie să
     aibă: căutare după nume (query param `search`), sortare (cel puțin ascendent/descendent după nume
     și după data adăugării — `sort=lastName,asc` / `sort=createdAt,desc`, format standard Spring Data,
     nu inventați alt format), și un buton/link pe fiecare rând care deschide formularul complet de
     pacient pre-completat cu ce există deja. E prima nevoie reală de conținut pentru `DashboardPage`
     (acum complet gol) — nu așteptați Modulul 3 (calendar) ca să puneți ceva util acolo.
  3. **Formular complet de pacient** (`pages/Patient/PatientForm.jsx`) — folosit atât pentru
     "completare" (deschis din widget) cât și pentru editare normală ulterioară; toate câmpurile în
     afară de nume/prenume/telefon trebuie tratate ca opționale la nivel de formular (pot fi goale la
     prima afișare), nu bifate `required` global.
- [ ] **Pagină + tabel + formular Pacienți** (`pages/Patient/*`) — listă cu search
  (`GET /api/patients?search=`), creare rapidă/editare (vezi mai sus), câmpuri conform F-201 (nume, CNP,
  dată naștere, email, telefon, alergii, istoric medical). Accesibilă oricărui staff autentificat
  (ADMIN/DOCTOR/RECEPTION) — nu e ecranul restricționat, ăla e fișa de consultație. Tabelul trebuie să
  arate vizual (badge/etichetă) care pacienți sunt încă incompleți, nu doar widget-ul de pe dashboard.
  - CNP-ul e criptat pe backend — pe frontend tratați-l ca text simplu la input (backend face
    criptarea/hash-ul), dar **nu afișați CNP-ul în clar în liste/tabele** decât dacă chiar e nevoie —
    discutați cu backend dacă `PatientResponse` întoarce CNP-ul complet sau mascat (ultimele 4 cifre).
- [ ] **Ecran Fișă de Consultație** (`pages/Consultation/*`, de creat), legat de o programare
  (`/api/appointments/{id}/record` — dar `Appointment`/programările sunt Modulul 3, deci acest ecran nu
  poate fi testat end-to-end până nu există și o pagină minimă de programări; poate fi construit
  izolat cu un `appointmentId` introdus manual/mock, pentru dezvoltare în paralel).
  - Restricție de rol pe UI: **RECEPTION nu trebuie să vadă acest ecran deloc** (nici link în navbar,
    nici acces direct pe rută) — e cerința #3 din Definition of Done a caietului de sarcini. Backend-ul
    va întoarce 403 oricum, dar UI-ul trebuie să ascundă opțiunea, nu doar să eșueze urât la request.
  - Formularul trebuie să devină read-only quando fișa e `locked` (răspunsul din backend include acest
    flag) — fără buton de editare activ, mesaj clar "Fișă finalizată, needitabilă".
- [ ] **Ecran Audit Log** (`pages/Audit/*`, de creat), doar pentru `ADMIN` — tabel simplu cu filtrare pe
  user/interval de dată, consumă `GET /api/audit-log?user&date`.
- [ ] Actualizează `Navbar`-ul (din #111, odată mergeuit) cu link-urile noi, condiționate pe rol.
- [ ] Un `module2_endpoints.md` va fi publicat de backend (owner: P5 din
  [backend_module2_tasks.md](backend_module2_tasks.md)) — nu inventați formatul DTO-urilor înainte,
  ca să nu repetăm problema de la §1.2.

---

## Prioritate recomandată (dacă nu știți de unde să începeți)
1. Rezolvați §1 (blocante) — altfel orice construiți acum se rescrie.
2. Modulul 1 (§2) — backend-ul e deja gata, e "doar" UI, și e ce lipsește vizibil din aplicație acum.
3. Modulul 2 (§3) — porniți în paralel cu backend-ul lui (vezi `backend_module2_tasks.md`), dar nu
   înainte ca DTO-urile lui să fie stabile, ca să nu repetați situația de la `module1_endpoints.md`.
