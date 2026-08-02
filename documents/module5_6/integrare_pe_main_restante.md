# Modulele 5 + 6 — ce a intrat pe `main` și ce a mai rămas

> Stare la 02.08.2026. Toate cele 7 PR-uri deschise (#154-#160) sunt merge-uite pe `main`.
> Backend-ul compilează, cele 83 de teste trec, frontend-ul se construiește (`vite build`).
>
> Documentul ăsta e lista de restanțe **de rezolvat împreună**, nu o listă de reproșuri. E ordonată
> după cât de tare blochează demo-ul, nu după cine a scris codul.

---

## 1. Blocante pentru demo — trebuie rezolvate

### 1.1. Notificările sunt pe **email**, nu pe **SMS** (Lotul A, PR #160)

Decizia 1.1 din `backend_module5_6_tasks.md` era explicită: **doar SMS, prin Twilio**, fără
`spring-boot-starter-mail`, fără template-uri de email. Ce a intrat e exact opusul:
`EmailSender` / `JavaMailEmailSender` / `LoggingEmailSender` / `EmailContent`,
`EmailPermanentException` / `EmailTransientException`, `Notification.recipientEmail` + `subject`,
`app.email.enabled` în loc de `app.sms.enabled`.

Arhitectural, restul e **corect și conform**: outbox în aceeași tranzacție, dispatcher `@Scheduled`
cu `fixedDelay = 15s`, backoff `{1, 5, 15, 60, 360}` minute, `try/catch` per rând, kill switch cu
sender de log implicit, token `SecureRandom` 12 bytes Base64 URL-safe. Doar **canalul** diferă.

Trei variante, alegeți una în grup:

- **(a) Se acceptă emailul ca abatere conștientă.** Cel mai ieftin. Se trece pe lista de "abateri de
  la caiet" pentru prezentare, lângă cea deja asumată (fără instrucțiuni speciale în notificare).
  Atunci trebuie curățat restul (vezi 1.2, 1.3) și scos Twilio din `pom.xml`.
- **(b) Se comută pe SMS.** Interfața `EmailSender` devine `SmsSender`, `EmailContent` dispare
  (SMS-ul n-are subiect), `recipientEmail` → `recipientPhone`, plus
  `util/PhoneNumberNormalizer.java` (§1.5 — **nu există deloc acum**). Dependința Twilio e deja în
  `pom.xml`. E o zi de lucru, nu mai mult.
- **(c) Ambele canale**, cu `NotificationType` decizând senderul. Cel mai mult de muncă și nimeni
  nu a cerut-o. Nu o recomand.

**Până nu se decide asta, 1.2 și 1.3 rămân blocate** — depind de ce câmp poartă destinatarul.

### 1.2. Pagina publică nu funcționează: verb HTTP greșit + link greșit

Două nepotriviri independente, ambele opresc complet fluxul F-502.2:

| Unde | Frontend (PR #155) | Backend (PR #160) | Efect |
|---|---|---|---|
| Verb | `publicApiClient.patch(...)` | `@PostMapping` | **405 Method Not Allowed** la confirmare și la anulare |
| Link | ruta e `/c/:token` | `NotificationMessageFactory` scrie `{baseUrl}/confirm?token=...` | linkul din notificare duce pe o rută inexistentă |

Fix-ul e mic de ambele părți: `patch` → `post` în `api/publicAppointment.js`, și
`baseUrl + "/c/" + confirmationToken` în `NotificationMessageFactory`. **Nu le-am aplicat** pentru
că a doua atinge textul mesajului, care depinde de decizia de la 1.1 — dacă se trece pe SMS,
`NotificationMessageFactory` se rescrie oricum.

### 1.3. `GET /api/notifications` — coloana `Destinatar` rămâne goală

`NotificationsPage.jsx` citește `n.recipientPhone`; `NotificationResponse` trimite `recipientEmail`
(mascat ca `i***@test.com`). Se rezolvă odată cu 1.1 — dacă rămâne emailul, se redenumește în
frontend; dacă se trece pe SMS, se redenumește în backend și masca devine `+4072***1111` ca în §A8.

### 1.4. `NotificationAdminController` nu are `@PreAuthorize` — **problemă de securitate**

`@RequestMapping("/api/notifications")` e sub regula generală "orice utilizator autentificat", deci
**un DOCTOR sau un RECEPTION poate lista coada de notificări a întregii clinici** (inclusiv `body`-ul
mesajelor și telefoanele/emailurile pacienților) și poate apăsa retry. §A8 cere `ADMIN`-only.

Fix: `@PreAuthorize("hasRole('ADMIN')")` la nivel de clasă, exact ca pe `ReportController`, plus un
test în tiparul lui `ReportControllerSecurityTest`. **Ăsta merită făcut primul** — e o linie și e
singurul lucru din listă care e o scurgere de date reală, nu doar o funcție lipsă.

### 1.5. Migrația `V9__.sql` nu există

Ultima migrație e tot `V8__.sql`. Tabela `notifications` apare doar pentru că profilul `dev` are
`ddl-auto: update` — pe o bază curată cu Flyway (adică oriunde în afară de laptopurile voastre)
aplicația pornește fără tabelă și tot Modulul 5 cade.

Lipsesc, conform §4.2:
- `CREATE TABLE notifications (...)` + FK spre `appointments` + `UNIQUE` pe `confirmation_token`
- index pe `(status, next_attempt_at)` — query-ul rulat de dispatcher la fiecare 15 secunde
- index pe `appointments (start_time)` — toate cele trei rapoarte filtrează pe el

Atenție la capcana din §4.2: dacă aveți deja `medsched_dev` pornită cu `ddl-auto: update`, Hibernate
a creat deja tabela pe lângă Flyway și `V9` va eșua cu "relation already exists".

---

## 2. Funcționalități din caiet care nu au fost livrate

### 2.1. Export PDF / Excel — backend complet, frontend deloc (F-602)

`ReportPdfExporter` (389 linii) și `ReportExcelExporter` (325 linii) sunt scrise, testate și expuse
pe `GET /api/reports/{type}/export?from&to&format=pdf|xlsx`. **Nimic din frontend nu le apelează.**

`components/report/ExportButtons.jsx` e un schelet de 15 linii: două butoane care primesc
`onExportCSV` / `onExportPDF`, iar ambele pagini care îl folosesc răspund cu un toast
"în curs de implementare". Lipsesc toate cele trei capcane documentate în §P1.1: citirea erorii din
`Blob`, construirea numelui de fișier în frontend (`Content-Disposition` nu e expus prin CORS) și
`URL.revokeObjectURL`. Lipsește și starea `exporting` per format.

De reținut: butonul se numește **Export Excel**, nu CSV — backend-ul produce `.xlsx`, iar cerința
explicită din F-602 e interpretarea nativă a numerelor, ceea ce CSV n-ar da.

### 2.2. Raportul de ocupare rulează pe date inventate (F-601.1)

`OccupancyReportPage.jsx` are `MOCK_DOCTORS_OCCUPANCY` / `MOCK_ROOMS_OCCUPANCY` hardcodate și un
`fetchOccupancyReport` care rezolvă un `setTimeout`. Comentariul din fișier spune "nu există endpoint
`/rapoarte/ocupare`" — **există**, `GET /api/reports/occupancy`, și întoarce exact shape-ul din §B1.
Trebuie doar înlocuit mock-ul cu `reportsApi.getOccupancyReport(from, to)` și mapate câmpurile
(`bookedMinutes` / `availableMinutes` / `occupancyRate`, nu `occupiedMinutes` / `totalMinutes` /
`percentage`).

Pagina nu folosește nici `ReportTabs`, nici `ReportFilters`, nici `ExportButtons` — are filtre
proprii. Merită aliniată la infrastructura P1, altfel cele trei rapoarte arată ca trei aplicații.

### 2.3. Secțiunea "Notificări SMS" din calendar e un placeholder (P5.2)

În modalul de detalii, blocul afișează text fix — `Status: În așteptare`,
`Tip notificare: Confirmare programare` — și telefonul pacientului din `eventDetail`. Nu apelează
`GET /api/notifications?appointmentId=...` (filtrul **există** în backend, e implementat).
E vizibilă pentru toate rolurile, deși §P5.2 cere `ADMIN` only.

### 2.4. `documents/module5_6/module5_6_endpoints.md` nu a fost publicat

Cerut la §4.4 ca sursă de adevăr pentru contract. Absența lui explică direct 1.2, 1.3 și 2.2 — toate
sunt nepotriviri de nume de câmp/verb pe care un tabel de endpoint-uri le-ar fi prins înainte de PR.
**Merită scris acum, retroactiv**, din codul care e deja pe `main`.

### 2.5. `PhoneNumberNormalizer` nu există

§A2, obligatoriu. Relevant doar dacă se alege varianta SMS la 1.1.

### 2.6. B7 (`priceAtBooking`) nu s-a făcut

Era marcat opțional, deci e în regulă. Dar înseamnă că **limitarea trebuie scrisă în README și spusă
la prezentare**: raportul de vânzări citește `service.price` de azi, deci dacă adminul schimbă un
preț, cifrele lunilor trecute se schimbă retroactiv.

---

## 3. Ce am rezolvat eu la integrare (ca să știți ce s-a schimbat față de PR-urile voastre)

1. **`CalendarPage.jsx` din PR #159 era nefuncțional** — fișierul de pe branch avea un `} finally {`
   fără `try`, un `closeModal` duplicat și **îi lipsea complet blocul de `return`** al componentei
   (361 de linii, se termina după `renderActionButtons`). Branch-ul nu se construia. La merge am
   păstrat versiunea din PR #158, care e versiunea funcțională de pe `main` plus secțiunea de
   notificări. **Consecință: modificările lui Ossian pe calendar (filtrarea pe medici) nu sunt
   active.** `components/Calendar/DoctorFilterMenu.jsx` și `doctorColors.js` sunt pe `main`, dar
   nimeni nu le importă — trebuie reintegrate într-un PR nou, pornit din `main` curat.
2. **`Navbar.jsx` — hook apelat condiționat.** `if (pathname.startsWith('/c/')) return null;` era
   pus înaintea lui `useState`, deci pe ruta publică se apelau mai puține hook-uri decât pe restul
   rutelor și React arunca "rendered fewer hooks than expected" la navigare. Am mutat ieșirea după
   toate hook-urile.
3. **`api/reports.js` trimitea `startDate` / `endDate`**, iar `ReportController` leagă `from` / `to`.
   Toate cele trei rapoarte răspundeau **400**. Redenumit în frontend.
4. **`SalesReportPage` destructura `const { toast } = useToast()`**, dar `ToastContext` expune
   `showSuccess` / `showError`. `toast` era `undefined`, deci pagina crăpa cu "toast is not a
   function" la prima cerere — și pe succes, și pe eroare. Trecut pe `showSuccess`/`showError`.
5. **`application-dev.yml` avea `password: ${ACCOUNT_PASSWORD}` fără valoare implicită.** Oricine
   nu avea variabila setată nu mai putea porni aplicația (placeholder nerezolvabil). Am pus
   `${ACCOUNT_PASSWORD:}` și am trecut host/port/username tot pe env cu valorile voastre ca default.
6. **Navbar avea două linkuri de rapoarte** (`Raport Ocupare` de la #159 și `Rapoarte` de la #154).
   Am păstrat unul singur, conform §P1.1 — navigarea între rapoarte se face din `ReportTabs`.
7. **`pom.xml` / `application.yml`** — reunite manual: Twilio + POI + OpenPDF (#157) și
   `spring-boot-starter-mail` (#160) coexistă; blocul `app.reports` era adăugat de amândoi, l-am
   păstrat o singură dată.

> Twilio a rămas în `pom.xml` deși nimic nu-l folosește acum. L-am lăsat intenționat, ca varianta (b)
> de la 1.1 să nu mai aibă nevoie de o modificare de build. Dacă se alege varianta (a), scoateți-l.

---

## 4. Ordinea sugerată de atac

1. `@PreAuthorize` pe `NotificationAdminController` (1.4) — o linie, e scurgere de date.
2. Decizia email vs SMS (1.1) — deblochează 1.2, 1.3, 2.3, 2.5.
3. `V9__.sql` (1.5) — fără ea nu există deploy curat.
4. `module5_6_endpoints.md` (2.4) — înainte ca cineva să atingă frontend-ul.
5. Export PDF/Excel în frontend (2.1) — e cea mai vizibilă lipsă la demo, backend-ul e gata.
6. Ocupare pe date reale (2.2).
7. Reintegrarea calendarului lui Ossian (3.1) și secțiunea de notificări din modal (2.3).
