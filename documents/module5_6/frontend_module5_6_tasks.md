# Frontend — Modulele 5 + 6 (Notificări SMS & Rapoarte + Export) — Împărțire pe persoane

> **Se schimbă convenția față de Modulele 2-4.** Până acum frontend-ul a avut un TODO comun în ordine de
> prioritate. De data asta avem 5 oameni și două module deodată, deci mai jos e o **împărțire nominală
> P1...P5**, fiecare cu fișierele lui, alese special ca să nu se calce doi oameni pe același fișier.
> Zonele unde totuși vă atingeți sunt listate explicit la §6.
>
> Backend-ul e descris în [backend_module5_6_tasks.md](backend_module5_6_tasks.md). Shape-urile din
> secțiunile §2 (DTO-uri notificări) și §3.B1 (DTO-uri rapoarte) de acolo sunt **suficient de stabile ca
> să începeți imediat**; `module5_6_endpoints.md` (publicat de backend la final) rămâne sursa de adevăr
> finală înainte de integrarea propriu-zisă.
>
> **Ce se schimbă conceptual:** notificările sunt **SMS prin Twilio**, nu email. Nu construiți niciun ecran
> de email, niciun câmp de email nou.

---

## 0. Ce există deja (verificat în cod, nu presupus) — citiți înainte de a scrie prima linie

- **Client HTTP: `services/apiClient.js` (axios).** Are deja interceptor de token
  (`localStorage.accessToken`) și refresh automat pe 401 cu coadă de cereri. **Toate apelurile noi trec
  prin el.** Fișierele `api/api.js`, `api/auditLog.js` și `api/consultationRecord.js` folosesc încă
  `fetch` brut cu URL hardcodat — **nu copiați tiparul ăla**, e restanță, nu model.
- **`components/PrivateRoute.jsx`** acceptă `roles={['ADMIN']}` și redirecționează spre `/dashboard` dacă
  rolul nu se potrivește. Există deja și `userLoading`, deci nu clipește.
- **`components/Navbar.jsx` are două ramuri complet separate**: una pentru `DOCTOR` (meniu burger) și una
  orizontală pentru `ADMIN`/`RECEPTION`, cu un bloc `{role === 'ADMIN' && (...)}` unde stau link-urile
  administrative. Link-urile noi de rapoarte intră **în acel bloc**, nu la rădăcină.
- **`<Navbar />` e randat global în `App.jsx`, deasupra `<Routes>`** — deci apare pe **orice** pagină,
  inclusiv pe una publică. Contează pentru P4 (vezi acolo).
- **Toast-uri: `sonner`.** `<Toaster position="top-right" richColors />` e montat în `main.jsx`;
  se folosește `toast.success(...)` / `toast.error(...)` (vezi `CalendarPage.jsx`). **NFR-3 interzice
  explicit** `alert()` / `confirm()` / `prompt()` native — nu le folosiți nicăieri.
  (Există și un `context/ToastContext.jsx` mai vechi, nefolosit de paginile noi — ignorați-l, mergeți pe
  `sonner` ca restul aplicației.)
- **`components/{Button,Card,Input,Modal}.jsx`** sunt pe Tailwind v4. Restul paginilor folosesc stiluri
  inline. E un mix deja existent — **folosiți `Card`/`Button`/`Input` pentru elementele de UI și stiluri
  inline pentru layout**, exact ca `CalendarPage.jsx`. Nu introduceți un al treilea sistem de stilizare.
  Atenție la `Modal.jsx`: randează **întotdeauna** un buton "Anulează" în subsol, peste `children` — dacă
  aveți nevoie de alt set de butoane, puneți-le în `children`, nu modificați `Modal` (îl folosesc alte 3
  pagini).
- **`pages/AuditLog/AuditLogPage.jsx` e cel mai apropiat model existent de "filtre + tabel"** — formular
  cu `<input type="date">` native, stări `loading`/`error`/`searched`. Uitați-vă la el înainte să
  inventați altceva; e mic (69 de linii).
- **Nu există nicio librairie de grafice** în `package.json` (`axios`, `date-fns`, `react`,
  `react-big-calendar`, `react-dom`, `react-router-dom`, `sonner` — atât). **Decizie de sprint: nu
  instalăm niciuna.** Ratele de ocupare se desenează cu bare CSS simple (un `div` cu `width: {rate}%`) —
  arată bine, e 10 linii, și nu adăugăm 200KB de bundle pentru trei tabele. Dacă cineva vrea grafice
  adevărate, e task separat, după ce toate paginile obligatorii sunt gata.
- **`date-fns` v4 e instalat** și se importă în stilul din `CalendarPage.jsx`
  (`import format from "date-fns/format"`). Copiați acel stil de import, nu altul.
- **`pages/DashboardPage.jsx` e încă un placeholder** de 19 linii. Rapoartele **nu** intră acolo — merg pe
  rute proprii (vezi P1).
- **Modalul de detalii din `CalendarPage.jsx`** (`modalMode === "details"`) are deja panoul complet de
  acțiuni de status din Modulul 4 și `eventDetail` (răspunsul complet `AppointmentResponse`). Acolo intră
  informația despre SMS-uri (P5) — nu într-o pagină nouă.

---

## P1 — Infrastructura de rapoarte + Raportul de Vânzări `[BLOCANT — se face primul]`

**P2 și P3 nu pot începe fără tine.** Tot ce e la §P1.1 trebuie să existe pe branch în **prima zi**, chiar
și cu tabele goale. Pagina de vânzări de la §P1.3 e cea mai simplă dintre cele trei rapoarte și devine
**modelul pe care P2 și P3 îl copiază** — scrie-o curat.

### P1.1. Infrastructura comună

- [ ] `api/reports.js` — prin `apiClient`, nu `fetch`:
  ```js
  export const reportApi = {
    occupancy: (params) => apiClient.get('/reports/occupancy', { params }).then(r => r.data),
    noShow:    (params) => apiClient.get('/reports/no-show',   { params }).then(r => r.data),
    sales:     (params) => apiClient.get('/reports/sales',     { params }).then(r => r.data),
    export:    (type, params) =>
      apiClient.get(`/reports/${type}/export`, { params, responseType: 'blob' }),
  };
  ```
  `params` = `{ from: 'yyyy-MM-dd', to: 'yyyy-MM-dd' }` (date, nu datetime — backend-ul le leagă cu
  `@DateTimeFormat(ISO.DATE)`).
- [ ] `components/report/ReportFilters.jsx` — două `<input type="date">` (`de la` / `până la`) + buton
  `Generează`. Reguli:
  - valori implicite: **luna curentă** (prima zi → azi) — adminul deschide pagina și vede ceva imediat,
    nu un ecran gol cu două câmpuri;
  - validare în UI înainte de request: `from <= to`, altfel `toast.error("Data de început trebuie să fie
    înaintea celei de sfârșit.")` și nu trimite;
  - `disabled` pe buton cât timp raportul se încarcă.
- [ ] `components/report/ExportButtons.jsx` — două butoane (`Export PDF`, `Export Excel`) + logica de
  descărcare. **Trei capcane reale, verificate în codul actual:**
  1. **Eroarea vine ca Blob, nu ca JSON.** Cu `responseType: 'blob'`, dacă backend-ul răspunde 403/400,
     `error.response.data` e tot un `Blob`, deci `error.response.data.message` e `undefined`. Citirea
     corectă:
     ```js
     const text = await error.response.data.text();
     const message = JSON.parse(text)?.message ?? 'Exportul a esuat.';
     ```
     Puneți asta într-un `try/catch` propriu — dacă body-ul nu e JSON, cădeți pe mesajul generic.
  2. **`Content-Disposition` NU e citibil din browser.** `SecurityConfig` din backend setează
     `setAllowedHeaders("*")` dar **nu** `setExposedHeaders`, iar CORS nu expune implicit header-ul ăsta
     către JS. Deci `response.headers['content-disposition']` va fi `undefined`. **Construiți numele
     fișierului în frontend** (`raport-vanzari_2026-07-01_2026-07-31.xlsx`) — zero modificări de backend.
     (Alternativa ar fi să cereți backend-ului `setExposedHeaders(List.of("Content-Disposition"))`; nu
     merită, e o schimbare în plus într-un fișier de securitate pentru un beneficiu cosmetic.)
  3. Curățați `URL.revokeObjectURL(url)` după `click()`, altfel blob-urile rămân în memorie la fiecare
     export.
  - Stare `exporting` per format (nu un singur flag pentru amândouă butoanele), `disabled` + text
    "Se genereaza..." cât timp durează. Fișierele PDF/Excel se generează pe backend și pot dura câteva
    secunde pe intervale mari.
- [ ] `App.jsx` — trei rute noi, toate `<PrivateRoute roles={['ADMIN']}>`:
  `/rapoarte/vanzari`, `/rapoarte/ocupare`, `/rapoarte/no-show`.
  Adaugă-le pe toate trei tu, chiar dacă paginile lui P2/P3 încă nu există — pune temporar un component
  gol. Altfel P2 și P3 ajung să editeze `App.jsx` în paralel și iese conflict.
- [ ] `components/Navbar.jsx` — link-uri noi **în blocul `{role === 'ADMIN' && (...)}`**. Trei link-uri
  separate ar încărca prea mult bara; fă un singur link `Rapoarte` către `/rapoarte/vanzari` plus o bandă
  de sub-navigare (tab-uri) în interiorul paginilor de raport (vezi mai jos).
- [ ] `components/report/ReportTabs.jsx` — trei `<NavLink>`-uri (`Vânzări` · `Ocupare resurse` ·
  `Neprezentări`) cu stil de "activ", randate în capul fiecărei pagini de raport. Așa navigarea între
  rapoarte e un click, iar navbar-ul rămâne curat.

### P1.2. Reguli comune de afișare (respectate de P1, P2, P3 la fel)

- **Ratele vin de la backend ca fracție `0..1`** (ex. `0.734`), nu ca 73.4. Afișare: `(rate * 100).toFixed(1) + '%'`.
  Dacă `availableMinutes === 0`, afișați `—`, nu `0.0%` și în niciun caz `NaN%`.
- **Sumele**: `new Intl.NumberFormat('ro-RO', { style: 'currency', currency: 'RON' }).format(value)`.
- **Tabelele au nevoie de `overflow-x: auto` pe un wrapper** — NFR-3 cere ca interfața să meargă și pe
  tabletă, iar un tabel de 6 coloane sparge layout-ul la 768px altfel.
- **Trei stări obligatorii pe fiecare pagină**: `loading` ("Se genereaza raportul..."), `error` (mesaj din
  `error.response.data.message`), `empty` ("Nicio programare in intervalul selectat.") — nu lăsați un
  tabel gol fără explicație.

### P1.3. Pagina `pages/Reports/SalesReportPage.jsx` (`/rapoarte/vanzari`) — F-601.3

- [ ] `ReportTabs` + `ReportFilters` + `ExportButtons type="sales"` în cap.
- [ ] Card cu **total încasări** pe interval (`grandTotal`), mare și vizibil — e cifra pentru care adminul
  deschide pagina.
- [ ] Două tabele: **pe serviciu** (`byService`) și **pe medic** (`byDoctor`), fiecare cu coloanele
  `Nume` · `Nr. programări` · `Total`. Sortate descrescător după total (backend-ul le trimite deja
  sortate — nu re-sortați în JS decât dacă adăugați sortare la click, care e opțională).
- [ ] Sub tabel, o notă mică: *"Se contorizează doar consultațiile finalizate."* — altfel adminul se
  întreabă de ce nu se potrivește cu numărul de programări din calendar.

---

## P2 — Raport Ocupare Resurse (`/rapoarte/ocupare`) — F-601.1

**Poți începe cu date mock imediat** (fixture JSON după shape-ul din `backend_module5_6_tasks.md` §B1),
fără să aștepți backend-ul. Depinzi doar de `ReportFilters`/`ExportButtons` de la P1 — dacă nu sunt gata,
scrie pagina cu două `<input type="date">` proprii și le înlocuiești când apar.

- [ ] `pages/Reports/OccupancyReportPage.jsx` — `ReportTabs` + `ReportFilters` +
  `ExportButtons type="occupancy"`.
- [ ] **Două secțiuni: Medici și Cabinete**, fiecare un tabel cu:
  `Nume` · `Minute ocupate` · `Minute disponibile` · `Grad de ocupare`.
- [ ] Coloana `Grad de ocupare` = procentul **plus o bară CSS** (fără librărie de grafice — vezi §0):
  ```jsx
  <div style={{ background: '#eee', borderRadius: 4, height: 10, width: 120 }}>
    <div style={{ width: `${Math.min(rate * 100, 100)}%`, height: '100%', borderRadius: 4,
                  background: rate > 0.85 ? '#c0392b' : rate > 0.6 ? '#e0a800' : '#2e8b57' }} />
  </div>
  ```
  Culorile sunt exact cele din `STATUS_COLORS` (`CalendarPage.jsx`) — refolosiți paleta aplicației, nu
  inventați alta.
- [ ] Afișați **minutele ca ore + minute** (`7h 30m`), nu `450` — nimeni nu citește un raport în minute.
  Păstrați minutele brute doar în export (le formatează backend-ul).
- [ ] Rândurile cu `availableMinutes === 0` (medic fără orar definit în interval): `—` la procent și un
  tooltip/notă *"Medicul nu are program definit în acest interval."* Nu le ascundeți — faptul că un medic
  n-are orar configurat e exact genul de problemă pe care adminul vrea s-o vadă.
- [ ] Medicii/cabinetele cu 0 programări apar cu `0h 0m` și `0.0%` — backend-ul le trimite intenționat, nu
  le filtrați în UI.
- [ ] Sortare descrescătoare după grad de ocupare, cu posibilitatea de a comuta pe nume (opțional, doar
  dacă termini restul).

---

## P3 — Raport Neprezentări (`/rapoarte/no-show`) — F-601.2

La fel ca P2: poți începe cu mock-uri, nu aștepta backend-ul.

- [ ] `pages/Reports/NoShowReportPage.jsx` — `ReportTabs` + `ReportFilters` +
  `ExportButtons type="no-show"`.
- [ ] **Trei carduri de sumar** în cap: total programări, total neprezentări, rata generală (procent mare).
- [ ] **Tabel 1 — pe pacient** (`byPatient`): `Pacient` · `Programări` · `Neprezentări` · `Rată`.
  - Evidențiați cu roșu (`#c0392b`) rândurile cu rată > 30% **și** cel puțin 3 programări — un pacient cu
    1 programare și 1 neprezentare are 100% rată dar nu e un "pacient problemă", iar dacă îl arătați
    colorat, raportul devine inutil.
  - Filtru rapid deasupra tabelului: `[ ] Doar pacienți cu ≥ 2 neprezentări` (checkbox, filtrare locală în
    JS — nu un request nou).
- [ ] **Tabel 2 — pe zi a săptămânii** (`byWeekday`): `Zi` · `Programări` · `Neprezentări` · `Rată`, cu
  aceleași bare CSS ca la P2 (coordonează cu P2 — dacă face un component `<RateBar />`, refolosiți-l în
  loc să-l duplicați; vorbiți în grup cine îl scrie).
  Traducerea `DayOfWeek` (`MONDAY` → `Luni`) se face cu un obiect de mapare local, în stilul lui
  `STATUS_LABELS` din `CalendarPage.jsx`.
- [ ] Notă sub tabel: *"Programările anulate din timp nu sunt contorizate."*

---

## P4 — Pagina publică de confirmare/anulare din SMS (`/c/:token`) — F-502.2

Asta e singura pagină din aplicație **fără autentificare**, deschisă de pacient **de pe telefon**, dintr-un
link primit prin SMS. Nu depinzi de nimeni — poți începe din prima zi.

- [ ] `api/publicAppointment.js` — trei apeluri către `/public/appointments/...`. **Atenție:**
  `apiClient` are `baseURL: 'http://localhost:8080/api'`, iar endpoint-urile astea sunt sub `/public`,
  **nu** sub `/api`. Deci `apiClient` cu path relativ **nu merge aici**. Două soluții, alegeți una și
  scrieți-o în comentariu:
  - o instanță axios separată, minimală, cu `baseURL: 'http://localhost:8080/public'` și **fără**
    interceptorul de token (pacientul nu are token; un 401 nu trebuie să declanșeze `forceLogout()` și
    redirect spre `/login`);
  - sau `apiClient.get('http://localhost:8080/public/...')` cu URL absolut (axios ignoră `baseURL` la URL
    absolut) — dar rămâne interceptorul de 401, deci prima variantă e mai curată.
- [ ] `pages/Public/AppointmentConfirmPage.jsx`:
  - la montare, `GET /public/appointments/{token}` → afișează data, ora, medicul, cabinetul, prenumele
    pacientului;
  - două butoane mari: **Confirm prezența** / **Anulez programarea**;
  - butoanele sunt vizibile doar dacă `canConfirm` / `canCancel` sunt `true` (backend-ul le trimite);
  - după acțiune reușită → mesaj clar de confirmare pe ecran (**nu doar un toast** — pacientul închide
    pagina imediat și trebuie să rămână cu o confirmare vizibilă);
  - **anularea cere o a doua apăsare de confirmare** ("Sigur anulați?" ca stare în pagină, nu
    `window.confirm()` — NFR-3).
- [ ] **Cele patru stări de eroare, fiecare cu text propriu în română:**

  | Cod | Ce afișezi |
  |---|---|
  | 404 | "Link invalid sau expirat. Contactați clinica la {telefon}." |
  | 410 | "Programarea a trecut deja. Pentru o programare nouă, sunați la {telefon}." |
  | 409 | "Programarea a fost deja {confirmată/anulată}." + reîncarci starea de pe server |
  | rețea | "Nu ne putem conecta. Încercați din nou." + buton de reîncercare |

- [ ] **Ruta e publică — în afara `PrivateRoute`.** În `App.jsx`:
  `<Route path="/c/:token" element={<AppointmentConfirmPage />} />`, la același nivel cu `/login`.
- [ ] **Ascunde navbar-ul pe această pagină.** `<Navbar />` e randat global în `App.jsx`, deasupra
  `<Routes>` — un pacient care deschide link-ul din SMS ar vedea o bară albastră cu "Login", ceea ce e
  derutant și arată neterminat. Rezolvare minimă, într-un singur loc: în `Navbar.jsx`,
  `const { pathname } = useLocation(); if (pathname.startsWith('/c/')) return null;` — o linie, fără să
  restructurezi `App.jsx`.
- [ ] **Mobile-first, obligatoriu.** Ăsta e singurul ecran din aplicație pe care **garantat** îl vede
  cineva pe telefon. Un singur card centrat, `max-width: 480px`, butoane pe toată lățimea, minimum 44px
  înălțime, text de minimum 16px. Verifică la 360px lățime în DevTools înainte de PR.
- [ ] Nu afișa și nu cere **nimic** în plus: fără CNP, fără telefon, fără serviciu, fără diagnostic.
  Backend-ul nici nu le trimite (e o decizie de confidențialitate, vezi §1.7 din doc-ul de backend) — dacă
  ai nevoie de un câmp care lipsește, **întreabă înainte de a cere adăugarea lui**.
- [ ] `PUBLIC_BASE_URL` din backend e `http://localhost:5173` în dev, deci link-ul din SMS va fi
  `http://localhost:5173/c/<token>` — testează cu un token luat direct din tabela `notifications`, nu
  aștepta un SMS real.

---

## P5 — Administrarea notificărilor + integrarea SMS în calendar

Nu depinzi de P1/P2/P3. Depinzi de endpoint-urile A8 din backend (`GET /api/notifications`,
`POST /api/notifications/{id}/retry`) — până apar, lucrează cu mock-uri.

### P5.1. Pagina `pages/Notifications/NotificationsPage.jsx` (`/admin/notificari`) — ADMIN only

- [ ] Rută nouă în `App.jsx` cu `<PrivateRoute roles={['ADMIN']}>` + link în blocul `{role === 'ADMIN'}`
  din `Navbar.jsx` (coordonează cu P1, care atinge aceleași două fișiere — vezi §6).
- [ ] `api/notifications.js` prin `apiClient`: `list({ status, appointmentId, page, size })` și
  `retry(id)`.
- [ ] Tabel: `Data` · `Tip` · `Destinatar` · `Status` · `Încercări` · `Ultima eroare` · acțiune.
  - Traduceri (obiect local, tiparul `STATUS_LABELS` din `CalendarPage.jsx`):
    `CONFIRMATION: "Confirmare"`, `REMINDER_24H: "Reamintire 24h"`, `RESCHEDULED: "Reprogramare"`,
    `CANCELLED: "Anulare"`; `PENDING: "În așteptare"`, `SENT: "Trimis"`, `FAILED: "Eșuat"`,
    `CANCELLED: "Anulat"`.
  - Pastilă colorată pe status: `SENT` verde `#2e8b57`, `PENDING` albastru `#3174ad`, `FAILED` roșu
    `#c0392b`, `CANCELLED` gri `#a0a0a0` — **aceeași paletă ca statusurile de programare**, ca să nu
    învețe utilizatorul două coduri de culoare diferite.
  - `Ultima eroare` trunchiat la ~60 de caractere, cu textul complet în `title` (tooltip nativ) — mesajele
    de la Twilio sunt lungi.
  - Telefonul vine deja mascat de la backend (`+4072***1111`) — **nu încercați să-l "reparați"**, e
    intenționat.
- [ ] Filtru pe status (`<select>`: toate / în așteptare / trimise / eșuate) + paginare simplă
  (`Înapoi` / `Înainte`, backend-ul întoarce `Page`).
- [ ] Buton **Retrimite**, vizibil **doar pe rândurile `FAILED`**. La succes: `toast.success("Notificarea
  a fost repusă în coadă.")` + refetch. La 409: `toast.error` cu mesajul din server.
- [ ] Buton `Reîmprospătează` manual. **Nu adăugați polling automat aici** — e un ecran administrativ
  deschis rar, iar un `setInterval` în plus doar consumă cereri (calendarul are deja polling la 20s din
  Modulul 3, e suficient real-time cât ne trebuie).

### P5.2. Starea SMS-urilor în modalul de detalii al programării

- [ ] În `CalendarPage.jsx`, modalul `modalMode === "details"`, sub panoul de acțiuni existent: o secțiune
  compactă **"Notificări SMS"** cu o linie per notificare (tip + status + oră).
- [ ] **Vizibilă doar pentru `ADMIN`** (endpoint-ul e ADMIN-only) — pentru `RECEPTION`/`DOCTOR` secțiunea
  nu se randează deloc, nu se randează goală.
- [ ] Necesită filtrul `appointmentId` pe `GET /api/notifications` — e trecut în task-ul A8 al
  backend-ului, dar **confirmă cu ei că a intrat** înainte de a scrie fetch-ul. Dacă întârzie, livrează
  P5.1 fără această bucată și adaug-o după; nu bloca PR-ul pe ea.
- [ ] Fetch-ul se face **doar când se deschide modalul de detalii**, nu la fiecare randare a calendarului
  (uită-te cum e făcut deja `eventDetail`/`eventDetailLoading` în `CalendarPage.jsx` și agață-te de același
  efect, nu adăuga un al doilea ciclu de fetch).

### P5.3. Revizia NFR-3 pe paginile noi `[la final, după ce P1-P4 au dat PR-uri]`

Ești ultimul filtru înainte de prezentare. Pe toate paginile noi (rapoarte, notificări, pagina publică):

- [ ] **Tabletă 768px și desktop 1280px** — fiecare tabel are wrapper cu `overflow-x: auto`, niciun text
  nu se suprapune, butoanele rămân apăsabile cu degetul. Persona (Dr. Simona) folosește tabletă în cabinet —
  e cerință, nu polish.
- [ ] **Zero `alert`/`confirm`/`prompt` native** în tot codul nou (`grep -rn "window.confirm\|alert(" frontend/src`).
- [ ] Fiecare buton care declanșează un request are stare `disabled` + text de progres cât timp lucrează
  (tiparul `{cancelling ? "Se anuleaza..." : "Anuleaza"}` din `CalendarPage.jsx`).
- [ ] Fiecare pagină are stările loading / empty / error, nu doar cazul fericit.

---

## 6. Zone unde vă atingeți — anunțați-vă în grup

| Fișier | Cine | Cum evităm conflictul |
|---|---|---|
| `App.jsx` | P1 (3 rute rapoarte), P4 (ruta publică), P5 (ruta notificări) | **P1 adaugă toate rutele deodată, în prima zi**, cu componente goale unde pagina încă nu există. P4 și P5 doar înlocuiesc importul. |
| `components/Navbar.jsx` | P1 (link Rapoarte), P4 (ascunderea pe `/c/`), P5 (link Notificări) | Trei modificări mici în locuri diferite ale fișierului — faceți-le pe rând, nu în paralel. Ordine: P1 → P5 → P4. |
| `CalendarPage.jsx` | doar P5 | Nimeni altcineva nu atinge fișierul ăsta în acest sprint. |
| `<RateBar />` (bara CSS de procent) | P2 și P3 | Îl scrie **P2**, în `components/report/RateBar.jsx`; P3 îl importă. Stabiliți în grup înainte, nu după ce l-au scris amândoi. |

---

## 7. Ordine recomandată pe sprint

1. **Ziua 1:** P1 livrează §P1.1 (api + filtre + export + rute + navbar) — restul depind de el.
   În paralel, **P4 și P5 pornesc imediat** cu mock-uri (nu depind de P1).
2. **Ziua 1-2:** P2 și P3 pornesc pe fixture-uri JSON după shape-urile din `backend_module5_6_tasks.md`
   §B1, fără să aștepte backend-ul funcțional.
3. **Când apare `module5_6_endpoints.md`**, fiecare își verifică shape-urile reale și înlocuiește
   mock-urile. Dacă un câmp diferă, **întrebați backend-ul înainte de a "corecta"** — poate e intenționat.
4. **La final:** P5 face revizia NFR-3 (§P5.3) pe tot ce a intrat, iar P4 verifică pagina publică pe un
   telefon real, nu doar în DevTools.

## 8. Ce NU facem în acest sprint

- Niciun ecran de email (notificările sunt SMS — decizie de sprint).
- Nicio librărie de grafice (bare CSS, vezi §0).
- Fără trimitere manuală de SMS dintr-un ecran de programare — există doar retry pe notificările eșuate.
- Fără WebSocket/real-time pe noile pagini (calendarul rămâne singurul cu polling).
- Fără export din frontend (SheetJS/jsPDF) — PDF-ul și Excel-ul vin gata făcute de la backend, ca cifrele
  din fișier să fie identice cu cele de pe ecran.
