# Frontend — TODO Modulul 3 (Motorul de Programări și Calendarul Interactiv)

> Stare la 2026-07-19. La fel ca la Modulul 2: nu e o împărțire pe persoane, e un TODO în ordine de
> prioritate — oricine liber ia următorul item nebifat. Backend-ul pentru acest modul e descris în
> [backend_module3_tasks.md](backend_module3_tasks.md) — contractul de DTO-uri de acolo (secțiunea 0) e
> stabil, dar `module3_endpoints.md` (publicat de P5 la final, ca la Modulul 2) e sursa de adevăr finală
> înainte să scrieți formulare pe baza shape-ului exact.

## 0. Ce există deja (ca să nu reinventăm)

- Zero cod de calendar/programări pe frontend — nu există `pages/Appointment*`, `pages/Calendar*`, nici
  vreun import de librărie de calendar. `package.json` nu are niciun pachet de dată/calendar/drag-and-drop
  (`axios`, `react`, `react-router-dom`, `tailwindcss` — atât). Pornim de la zero pe acest modul.
- `pages/DashboardPage.jsx` e în continuare un placeholder literal (`<h1>Dashboard Principal</h1><p>Aici
  medicii vor vedea calendarul cu programări.</p>` + widget-ul de pacienți incompleți din Modulul 2) —
  **acesta e locul unde intră calendarul**, nu o pagină nouă separată de dashboard (deși poate fi și o
  rută dedicată `/calendar` legată din navbar, decideți la implementare — vezi §2).
- Navigarea e încă un `Navbar()` inline în `App.jsx` (nu există `components/Navbar.jsx` separat), cu
  `<Link>`-uri simple, fără hamburger/dropdown de niciun fel, stiluri inline (nu Tailwind). Rolurile
  `DOCTOR`/`RECEPTION` primesc azi exact același meniu minimal (doar `ADMIN` are linkuri suplimentare) —
  Modulul 3 e primul care chiar are nevoie de linkuri diferite pentru `DOCTOR` vs `RECEPTION` (calendar
  propriu vs. calendar cu filtru multi-medic), deci merită să scoateți `Navbar` într-un component propriu
  acum, nu să mai înghesuiți logică în `App.jsx`.
- `pages/ClinicalService/*` (3 fișiere) sunt încă goale (0 bytes), nerutate — leftover din Modulul 1.
  Backend-ul (`/api/services`, `ClinicalServiceController`) e complet funcțional. Formularul de programare
  de mai jos are nevoie de o listă de servicii (dropdown), deci aveți nevoie **minim** de un
  `api/services.js` + un fetch simplu — **nu** trebuie neapărat să terminați pagina de administrare CRUD a
  serviciilor ca să porniți Modulul 3 (aia rămâne o restanță separată de Modulul 1, poate fi luată de
  oricine în paralel, dar nu e blocantă aici).
- Client HTTP: bug-ul de chei diferite din Modulul 2 e rezolvat — `apiClient.js` (axios) și
  `AuthContext.jsx` folosesc consecvent `"accessToken"`/`"refreshToken"` din `localStorage`. Toate
  request-urile noi (`api/appointments.js`) trebuie să folosească `apiClient.js` (axios), la fel ca
  `api/doctors.js`/`api/patients.js`/etc. — nu porniți un al treilea client HTTP.

---

## 1. Alegere librărie de calendar (decizie de luat înainte de orice pagină)

Nu există nimic instalat. Recomandare: **`react-big-calendar`** (MIT, gratuit integral) în locul
FullCalendar — motivul concret: F-301 cere explicit vizualizare "pe medic sau pe cabinet" (grupare pe
resurse), iar la FullCalendar vederile de tip resource-timeline sunt în pluginul Scheduler cu licență
separată; `react-big-calendar` are suport de resurse (`resourceMap`/`resourceIdAccessor`) direct în
pachetul de bază, plus un addon `withDragAndDrop` (util mai târziu la Modulul 4, F-401) tot MIT. Dacă
echipa preferă alt pachet, discutați înainte de a scrie prima pagină — schimbarea ulterioară a librăriei
de calendar e costisitoare.

- [ ] `npm install react-big-calendar date-fns` (react-big-calendar cere un adapter de date — `date-fns`
  e cel mai ușor de integrat, dar `moment`/`dayjs` merg la fel de bine dacă echipa are deja preferință).
- [ ] Import CSS-ul de bază al librăriei (`react-big-calendar/lib/css/react-big-calendar.css`) — va trebui
  suprascris parțial cu Tailwind/clase proprii ca să semene cu restul aplicației, nu lăsați stilul default.

---

## 2. Pagina de Calendar (F-301) — piesa centrală a modulului

- [ ] **Rută nouă** `/calendar`, protejată prin `PrivateRoute` pentru `ADMIN`, `DOCTOR`, `RECEPTION` (toți
  trei au acces, dar conținutul diferă per rol, vezi mai jos). Adăugată în `App.jsx`. Decideți dacă
  `/dashboard` redirecționează spre `/calendar` sau dacă rămân separate cu link în navbar — oricare e ok,
  dar nu lăsați `/dashboard` să rămână placeholder gol după acest modul.
- [ ] `pages/Calendar/CalendarPage.jsx` — componenta principală:
  - Vizualizare zi/săptămână (`view="day"`/`"week"` din `react-big-calendar`, comutabile din UI) — luna nu
    e cerută explicit de F-301, opțional dacă rămâne timp.
  - `onSelectSlot` (click pe interval liber) → deschide `AppointmentForm` (vezi §3) pre-completat cu
    data/ora clickuită, conform cerinței F-301 ("adăugarea unei programări printr-un simplu click pe un
    interval liber") și NFR-3 (creare programare în sub 30 secunde).
  - `onSelectEvent` (click pe o programare existentă) → deschide același `AppointmentForm` în mod editare
    (reprogramare) sau un panou de detalii cu opțiune de anulare — decideți la implementare, dar nu
    folosiți `window.confirm()`/`alert()` native pentru anulare, NFR-3 le interzice explicit — folosiți
    `Modal`-ul existent (`components/Modal.jsx`) + `ToastContainer`/`useToast()` (există, montat, dar
    aproape neutilizat încă — acesta e locul potrivit să-l folosiți în sfârșit) pentru confirmare/eroare.
  - Fetch-ul de date către `GET /api/appointments/calendar?from=&to=&doctorIds=&roomId=` — parametrii
    exacți depind de rol (vezi §4, filtrul hamburger) și de vederea aleasă (zi/săptămână determină
    `from`/`to`).
  - **Polling, nu WebSocket**: backend-ul nu are infrastructură real-time în acest modul (vezi
    `backend_module3_tasks.md`, nota din header). Refolosiți un interval simplu (`setInterval`/hook
    `useInterval`, ex. re-fetch la 15-30s cât timp pagina e activă) ca să respectați NFR-2 ("aproape
    instant, fără reload manual") fără server push. Curățați intervalul la unmount.
  - Culoare/etichetă vizuală per `status` din `AppointmentStatus` (SCHEDULED/CONFIRMED/IN_PROGRESS/
    COMPLETED/NO_SHOW/CANCELLED) — chiar dacă tranzițiile de status vin abia în Modulul 4, backend-ul
    întoarce deja `status = SCHEDULED` la creare, deci UI-ul trebuie să știe să-l randeze de pe acum.
- [ ] `api/appointments.js` (nou, stil axios ca restul, vezi `api/patients.js` ca referință) —
  `createAppointment`, `updateAppointment`, `cancelAppointment`, `getAppointmentById`,
  `getCalendarAppointments({from, to, doctorIds, roomId})`.

---

## 3. Formular de programare (`AppointmentForm`)

- [ ] `pages/Calendar/AppointmentForm.jsx` (sau `components/Appointment/AppointmentForm.jsx` dacă e
  reutilizat și în afara paginii de calendar) — folosit atât pentru creare cât și pentru reprogramare:
  - Câmpuri: pacient (select cu search — reutilizați fetch-ul din `api/patients.js`, cu opțiune "adaugă
    pacient nou" care deschide modalul rapid din Modulul 2 dacă nu există deja, ca recepția să nu trebuiască
    să navigheze departe de calendar — vezi NFR-3, max 2 ferestre succesive), medic (select din
    `api/doctors.js`), cabinet (select din `api/rooms.js`), serviciu (select din noul `api/services.js`),
    dată+oră start, notițe/instrucțiuni speciale (textarea opțională).
  - **Nu există câmp de "echipament"** — se alocă automat pe backend (`EquipmentAllocationService`), nu-l
    puneți în formular, altfel duplicați o decizie deja luată pe backend.
  - **Nu există câmp de "oră sfârșit"** — se calculează pe backend din durata serviciului. Afișați-l
    read-only după ce serviciul e ales (calculat local doar pentru preview: `start + service.durata`, dar
    trimiteți la server doar `startTime`).
  - La submit, dacă backend-ul întoarce 409 cu `conflictingResource` (DOCTOR/ROOM/EQUIPMENT) + mesaj —
    afișați mesajul **exact** primit de la server (F-302 cere explicit ca eroarea să numească resursa în
    conflict, ex. "Cabinetul 3 este deja ocupat în acest interval") via toast/inline error, nu un mesaj
    generic de tipul "eroare la salvare".
  - Validare minimă client-side înainte de submit (câmpuri obligatorii goale) — restul validării de
    disponibilitate se face pe server, nu duplicați algoritmul de coliziuni în frontend.

---

## 4. Filtru "hamburger" cu medici — vizualizare multi-calendar pentru recepție

Cerință de business nouă (cerută explicit de PM pentru acest modul): recepția lucrează cu 8 cabinete și
mai mulți medici — pe calendarul comun, trebuie să poată alege rapid **pe care medici** vrea să-i vadă
simultan, ca să nu se piardă într-o grilă supraaglomerată cu toți medicii din clinică deodată.

- [ ] `components/Calendar/DoctorFilterMenu.jsx` — un buton hamburger (☰) plasat lângă header-ul
  `CalendarPage`, care deschide un panou lateral/dropdown (folosiți `Modal.jsx` sau un panel simplu
  poziționat absolut — nu blocați tot ecranul, recepția trebuie să poată vedea calendarul în timp ce
  bifează) cu:
  - Lista tuturor medicilor activi (`GET /api/doctors`, filtrat client-side pe `active === true` dacă
    endpoint-ul nu filtrează deja), fiecare cu un checkbox + numele + specialitatea.
  - Acțiuni rapide "Selectează tot" / "Deselectează tot".
  - Selecția se aplică live (fără buton separat de "Aplică") sau cu un buton explicit "Aplică filtrul" —
    alegeți în funcție de cât de scump e fetch-ul; dacă re-fetch la fiecare bifare simplă e prea des,
    puneți debounce (~300-500ms) sau un buton explicit.
  - **Culoare stabilă per medic** — atribuiți fiecărui `doctorId` o culoare dintr-o paletă fixă (funcție
    de hash simplă pe `doctorId`, sau o mapare index→culoare din lista de medici activi), folosită atât pe
    evenimentele din calendar cât și pe bifele din meniul hamburger, ca recepția să poată asocia vizual
    rapid "acest bloc verde = Dr. X" fără să deschidă fiecare eveniment.
  - Selecția inițială: toți medicii bifați (comportament implicit sigur — nimic ascuns la prima
    deschidere). Opțional: persistați selecția în `localStorage` per utilizator, ca recepția să nu
    rebifeze de la zero la fiecare login (nu e obligatoriu pentru MVP).
- [ ] **Vizibilitate condiționată pe rol** — hamburger-ul cu selectorul de medici apare **doar** pentru
  `RECEPTION` și `ADMIN`. Pentru `DOCTOR`, calendarul afișează direct și exclusiv programările proprii
  (backend-ul oricum forțează asta server-side indiferent ce trimite frontend-ul — vezi
  `backend_module3_tasks.md` §5 — dar UI-ul nu trebuie nici măcar să sugereze opțiunea, la fel cum ecranul
  de fișă de consultație e ascuns complet pentru `RECEPTION` la Modulul 2).
- [ ] Selecția din `DoctorFilterMenu` alimentează parametrul `doctorIds` trimis către
  `GET /api/appointments/calendar` (vezi `api/appointments.js`, §2) — coordonați formatul exact (listă de
  UUID-uri ca query param repetat sau CSV) cu `module3_endpoints.md` odată publicat de backend, nu
  presupuneți formatul înainte.
- [ ] Filtru secundar pe cabinet (dropdown simplu, single-select, nu hamburger — F-301 cere și vederea "pe
  cabinet") poate coexista cu filtrul de medici, dar clarificați cu backend dacă cele două filtre se pot
  combina în același request sau sunt mutual exclusive (`doctorIds` vs `roomId` — vezi regula din
  `backend_module3_tasks.md` §5, controller-ul alege `roomId` peste `doctorIds` dacă ambele sunt trimise).

---

## 5. Navbar — extrage-l din `App.jsx` și adaugă linkurile noi

- [ ] Creează `components/Navbar.jsx` (nu mai există ca fișier separat azi, e inline în `App.jsx`) — mutați
  logica existentă acolo 1:1 mai întâi (fără regresii), apoi adăugați:
  - Link "Calendar" (`/calendar`) — vizibil pentru toate cele 3 roluri (destinația diferă per rol prin
    conținut, nu prin link separat).
  - Pentru `RECEPTION`: nimic suplimentar în navbar propriu-zis — hamburger-ul de medici trăiește **în**
    `CalendarPage`, nu în navbar-ul global (e specific contextului de calendar, nu navigare generală).
  - Corectați, dacă nu s-a făcut deja, verificarea de roluri să folosească exact `ADMIN`/`DOCTOR`/
    `RECEPTION` (fără variantele greșite `ADMINISTRATOR`/`MEDIC`/`RECEPTIONIST` semnalate ca risc în
    `frontend_modul1_modul2.md` §1.4, dacă acel PR a fost integrat între timp cu bug-ul încă prezent,
    verificați).
  - Folosiți clase Tailwind (navbar-ul actual e încă stiluri inline, predatând integrarea Tailwind) — nu
    obligatoriu în acest modul dacă timpul nu permite, dar dacă tot atingeți fișierul, merită curățat.

---

## 6. Loading / empty / error states

- [ ] Calendar gol (nicio programare în intervalul vizualizat) — mesaj clar, nu grilă goală ambiguă.
- [ ] Loading state la schimbarea vederii (zi/săptămână) sau la aplicarea filtrului de medici — spinner/
  skeleton minim, nu "flash" de conținut vechi confuz.
- [ ] Eroare de rețea la fetch calendar (polling eșuat) — nu bombardați utilizatorul cu un toast la
  fiecare interval de polling eșuat; afișați un indicator discret ("ultima actualizare eșuată, reîncercăm")
  și opriți-vă din spam dacă eșuează repetat.

---

## Prioritate recomandată (dacă nu știți de unde să începeți)

1. §1 (alege și instalează librăria de calendar) — blocant pentru orice altceva.
2. §2 (pagina de calendar + fetch de bază, fără filtru încă) — vizibil imediat, validează integrarea cu
   backend-ul (`GET /api/appointments/calendar`).
3. §3 (formular de creare/reprogramare) — funcționalitatea centrală a modulului (F-301 + F-302 din
   perspectiva utilizatorului).
4. §4 (hamburger multi-medic) — cerința nouă de business, dependentă de §2 fiind deja funcțional.
5. §5 (navbar) și §6 (stări de loading/empty/error) — polish, în paralel cu oricare din cele de mai sus.
