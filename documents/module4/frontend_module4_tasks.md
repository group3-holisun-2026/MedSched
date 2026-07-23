# Frontend — TODO Modulul 4 (Gestiune Status și Validare Flux)

> La fel ca la Modulul 3: nu e o împărțire pe persoane, e un TODO în ordine de prioritate — oricine liber
> ia următorul item nebifat. Backend-ul e descris în [backend_module4_tasks.md](backend_module4_tasks.md) —
> contractul de endpoint-uri de acolo (secțiunea 1.2) e stabil, dar `module4_endpoints.md` (publicat de P5
> la final) e sursa de adevăr finală pe shape-ul exact de request/response.

## 0. Ce există deja (verificat în cod, nu presupus) — și doi bug-uri care trebuie reparate primii

- `pages/Calendar/CalendarPage.jsx` are deja un modal "Detalii programare" (`modalMode === "details"`) cu
  butoane `Reprogramează`/`Anulează programarea` — **acesta e locul unde adăugăm butoanele noi de
  tranziție**, nu o pagină separată.
- `pages/Consultation/ConsultationRecordPage.jsx` **deja** face `readOnly={record?.locked || false}` pe
  formular — asta înseamnă că blocarea vizuală a fișei (F-402) **nu are nevoie de nicio schimbare** odată ce
  backend-ul apelează `lock()` la momentul potrivit (Modulul 4 backend, §2). Nu reimplementați asta.
- **Bug blocant, de reparat înaintea oricărui alt task**: `App.jsx` are `import CalendarPage from
  './pages/Calendar/CalendarPage'` declarat **de două ori** (linia 5 și linia 11) și ruta `/calendar`
  definită **de două ori** (liniile 43-50 și 91-98) — un artefact de merge nerezolvat din integrarea a două
  branch-uri de Modul 3. Un import JS duplicat cu același nume e `SyntaxError` la build ("Identifier
  'CalendarPage' has already been declared") — verificați dacă build-ul chiar pică azi pe `main`/branch-ul
  vostru de pornire înainte de orice altceva; dacă da, ștergeți duplicatele (linia 11 și blocul de rută
  91-98) într-un commit separat, minim, înaintea task-urilor de mai jos.
- **Fișier mort, de curățat cât suntem în zonă**: `components/CalendarPage.jsx` (218 linii) e o versiune
  veche/paralelă a paginii de calendar, nefolosită nicăieri (pagina reală e
  `pages/Calendar/CalendarPage.jsx`, importată în `App.jsx`). Ștergeți-l — nu e referențiat de nimic,
  verificat.
- `api/consultationRecord.js` folosește `fetch` brut cu URL hardcodat (`http://localhost:8080/api`), nu
  `apiClient.js` (axios) ca restul fișierelor din `api/` (`appointments.js`, `patients.js` etc.). Nu e
  blocant, dar dacă tot atingeți acest fișier pentru task-ul de la §3 (contor de grație), migrați-l la
  `apiClient.js` în aceeași schimbare — nu introduceți un al treilea tipar HTTP.
- Statusul e afișat azi ca text brut din enum (`Status: {selectedEvent.status}`, ex. literalmente
  "IN_PROGRESS") — nicio etichetă în română, nicio traducere. F-401/F-402 ne pun oricum să construim un
  panou de acțiuni per-status, deci rezolvăm și eticheta în aceeași schimbare (§1).
- Librăria de calendar (`react-big-calendar`) are deja inclus addon-ul `withDragAndDrop` (verificat în
  `node_modules`), dar **nu e folosit**. Notă de corectare față de ce anticipa `frontend_module3_tasks.md`:
  addon-ul e pentru *mutarea* unei programări prin tragere (schimbare de oră/dată), nu pentru schimbarea de
  status. Schimbarea de status (F-401) o construim ca panou de butoane contextuale în modalul de detalii
  existent — mai simplu, mai clar pentru recepție, și nu necesită nicio librărie nouă. Dacă echipa vrea și
  drag-to-reschedule mai târziu, e un task separat, opțional, nu parte din acest modul.

---

## 1. Panou de acțiuni rapide în modalul "Detalii programare" (F-401)

- [ ] `STATUS_LABELS` (obiect nou, lângă `STATUS_COLORS` existent în `CalendarPage.jsx`) — traducere
  română pentru fiecare valoare din `AppointmentStatus`: `SCHEDULED: "Programat"`, `CONFIRMED: "Confirmat"`,
  `IN_PROGRESS: "Consultație activă"`, `COMPLETED: "Finalizat"`, `NO_SHOW: "Neprezentat"`,
  `CANCELLED: "Anulat"`. Înlocuiți `Status: {selectedEvent.status}` cu eticheta tradusă.
- [ ] Butoanele afișate în modalul de detalii depind de **statusul curent** și de **rolul utilizatorului**
  curent (din `useAuth()`, la fel cum face deja `Navbar.jsx`):

  | Status curent | Rol | Butoane |
  |---|---|---|
  | `SCHEDULED` | ADMIN, RECEPTION | Confirmă · Reprogramează · Anulează |
  | `CONFIRMED` | ADMIN, RECEPTION | Pacient sosit (check-in) · Neprezentat · Reprogramează · Anulează |
  | `CONFIRMED` | DOCTOR (propria programare) | Pacient sosit (check-in) |
  | `IN_PROGRESS` | ADMIN, DOCTOR (propria programare) | Deschide fișa de consultație (navighează la `/appointments/{id}/record`) |
  | `COMPLETED` | oricine cu acces la calendar | Vezi fișa (link, read-only) |
  | `NO_SHOW` / `CANCELLED` | — | fără acțiuni, doar afișare |

  Pentru a ști dacă programarea afișată e "a mea" (cazul `DOCTOR`), comparați `user.id`/`user.doctorId` din
  `AuthContext` cu `selectedEvent.raw.doctor.id` — dacă `AuthContext` nu expune încă `doctorId` pentru un
  user cu rol `DOCTOR`, verificați ce întoarce `/api/auth/me` (sau echivalent) înainte de a presupune un
  câmp care nu există; dacă lipsește, backend-ul (P5, `AppointmentResponse`) tot expune `doctor.id` pe
  fiecare programare, deci comparați cu userul curent prin orice identificator comun există deja în
  `AuthContext` — nu adăugați un query nou doar pentru asta dacă informația e deja disponibilă.
  **Nu vă bazați doar pe ascunderea butonului în UI** — backend-ul oricum respinge cu 403 dacă cineva
  apasă un buton la care n-are dreptul (verificat: controller-ul face check-ul de ownership), UI-ul doar
  evită confuzia de a arăta o acțiune care oricum ar eșua.
- [ ] `api/appointments.js` — adaugă `confirm(id)`, `checkIn(id)`, `noShow(id)`, `complete(id)` (PATCH,
  fără body, la fel de simplu ca `cancel` existent). Confirmați path-urile exacte cu
  `module4_endpoints.md` odată publicat.
- [ ] La fiecare acțiune: `toast.success(...)` pe reușită (folosiți `sonner`, la fel ca restul paginii —
  **nu** `window.confirm()`/`alert()` native, NFR-3 le interzice explicit), `toast.error(...)` cu mesajul
  exact din `error.response.data.message` dacă backend-ul îl întoarce (verificați dacă noile endpoint-uri
  întorc `ErrorResponse` cu câmp `message`, nu string brut — pe Modulul 3, `ResourceConflictException` e
  mapat azi la un `ResponseEntity<String>` cu body text brut, nu la shape-ul standard `ErrorResponse`, ceea
  ce face ca `error.response.data?.message` din `AppointmentForm.jsx` să fie mereu `undefined` pentru acel
  caz specific — verificați cu backend-ul dacă erorile noi de tranziție (409) respectă shape-ul standard cu
  `message`, altfel adaptați citirea erorii pentru acel endpoint specific).
- [ ] Refă fetch-ul calendarului (`fetchAppointments()`, deja existent) după orice acțiune reușită, la fel
  cum face deja `handleFormSaved` — nu lăsați evenimentul cu culoarea/statusul vechi până la următorul
  ciclu de polling (20s).

---

## 2. Culoare/etichetă pentru "Consultație activă" pe grila de calendar

- [ ] `STATUS_COLORS` există deja și acoperă toate cele 6 statusuri — verificați doar că paleta rămâne
  distinctă vizual (ex. `IN_PROGRESS` galben `#e0a800` vs `CONFIRMED` verde `#2e8b57` — deja par suficient
  de diferite, dar confirmați pe un ecran real, nu doar citind hex-ul).
- [ ] Legendă mică lângă calendar (culoare + etichetă din `STATUS_LABELS`, §1) — F-301/NFR-3 cer interfață
  "curată, dintr-o privire", iar acum avem 6 culori fără nicio legendă vizibilă azi.

---

## 3. Fișa de consultație — contor de grație (30 minute) și blocare la finalizare

- [ ] `ConsultationRecordPage.jsx` trebuie să știe **statusul și `completedAt`** ale programării, nu doar
  fișa în sine — azi face fetch doar pe `GET /api/appointments/{id}/record`. Adăugați un fetch suplimentar
  pe `GET /api/appointments/{id}` (endpoint deja existent din Modulul 3) ca să obțineți `status` și noul
  câmp `completedAt` (adăugat de backend, vezi `backend_module4_tasks.md` §4).
- [ ] Buton **"Finalizează consultația"**, vizibil doar când `status === "IN_PROGRESS"` și userul e medicul
  alocat (sau `ADMIN`) — apelul salvează întâi fișa curentă (dacă au fost modificări nesalvate) și abia apoi
  cheamă `PATCH /api/appointments/{id}/complete`. După succes, `toast.success` + reîmprospătați starea
  locală (`status` devine `COMPLETED`, `completedAt` se populează) — **fișa NU devine `readOnly` imediat**,
  rămâne editabilă conform `record.locked` (care rămâne `false` încă 30 de minute, e comportament de
  backend, nu de UI).
- [ ] Când `status === "COMPLETED"` și `record.locked === false`, afișați un banner discret (nu blocant, nu
  toast repetitiv): *"Consultația e finalizată. Fișa rămâne editabilă până la `<completedAt + 30 min,
  formatat HH:mm>`."* — calculat local din `completedAt` primit de la server, fără polling suplimentar
  dedicat (un simplu `useEffect` cu recalcul la fiecare minut e suficient, nu inventați un websocket pentru
  un contor de 30 de minute).
- [ ] Când `record.locked === true`, mesajul existent ("Fișa este blocată (programare finalizată) — doar în
  citire.") deja acoperă cazul final — nu-l duplicați cu bannerul de mai sus (afișați unul sau altul, nu
  amândouă).
- [ ] Dacă `PUT`/`POST` pe fișă întoarce 403 (cazul nou de la backend — "doar medicul alocat poate edita
  fișa", vezi `backend_module4_tasks.md` §2), afișați un mesaj clar ("Nu aveți drepturi asupra acestei
  fișe.") în loc să lăsați eroarea necaptată — verificați ce se întâmplă azi în `handleSubmit` din
  `ConsultationRecordPage.jsx` pe eroare (azi pune `err.message` direct în `error`, ceea ce pentru un
  `fetch` eșuat cu 403 va fi mesajul generic "Nu s-a putut actualiza fisa de consultatie" din
  `api/consultationRecord.js` — suficient de clar deja, dar verificați că nu se confundă cu eroarea de
  "fișă blocată").

---

## 4. Loading / empty / error states (polish, în paralel cu restul)

- [ ] Buton de acțiune (confirmă/check-in/etc.) în stare de "se procesează..." (disabled + text schimbat,
  la fel ca `cancelling`/`{cancelling ? "Se anuleaza..." : ...}` deja existent pentru `Anulează`) — nu
  permiteți dublu-click care trimite două request-uri de tranziție simultan.
- [ ] Dacă acțiunea eșuează cu 409 (tranziție invalidă — ex. altcineva a apăsat deja "Anulează" chiar
  înaintea ta, din altă sesiune, iar tu vezi încă starea veche din calendarul nereîmprospătat pe deplin din
  cauza polling-ului de 20s), afișați mesajul de conflict din server și **reîmprospătați calendarul automat**
  ca userul să vadă starea reală curentă, nu doar un toast izolat care lasă UI-ul desincronizat.

---

## Prioritate recomandată

1. §0 — repară bug-ul de build (import/rută dublă în `App.jsx`) înainte de orice. Verifică dacă blochează
   deja pornirea aplicației.
2. §1 (panoul de acțiuni în modalul de detalii) — inima F-401, vizibil imediat, validează integrarea cu
   noile endpoint-uri de backend.
3. §3 (contor de grație + buton Finalizează pe fișă) — inima F-402 din perspectiva medicului.
4. §2 (legendă) și §4 (stări de loading/empty/error) — polish, în paralel cu oricare din cele de mai sus.
