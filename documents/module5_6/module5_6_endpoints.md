# Modulele 5 + 6 — Contract de endpoint-uri

> **Sursa de adevar pentru frontend.** Scris retroactiv, dupa integrarea PR-urilor #154-#160, si
> verificat direct pe aplicatia pornita (nu copiat din planul de sprint). Absenta acestui document
> in timpul sprintului e cauza directa a nepotrivirilor de nume de campuri care au trebuit reparate
> la integrare — daca schimbati un camp, schimbati-l intai aici.
>
> **Canalul de notificare e email**, nu SMS. Twilio a fost scos din scop: un cont SMS ar fi fost
> peste nevoile demo-ului, iar F-501/F-502 sunt acoperite integral pe email.

Toate rutele `/api/**` cer `Authorization: Bearer <accessToken>`. Rutele `/public/**` sunt anonime.
Erorile trec prin `GlobalExceptionHandler`, deci frontend-ul citeste `error.response.data.message`.

---

## 1. Notificari (Modulul 5) — `ADMIN` only

`@PreAuthorize("hasRole('ADMIN')")` e pe clasa: raspunsul contine corpul mesajelor si adresele
pacientilor. `DOCTOR` si `RECEPTION` primesc **403**.

| Metoda | Path | Query | Raspuns |
|---|---|---|---|
| GET | `/api/notifications` | `status?`, `appointmentId?`, `page=0`, `size=20` | `Page<NotificationResponse>` |
| POST | `/api/notifications/{id}/retry` | — | `204`; `409` daca nu e `FAILED`; `404` daca nu exista |

```ts
type NotificationTrigger = 'CONFIRMATION' | 'REMINDER_24H' | 'RESCHEDULED' | 'CANCELLED';
type NotificationStatus  = 'PENDING' | 'SENT' | 'FAILED' | 'CANCELLED';

type NotificationResponse = {
  id: string;
  appointmentId: string;
  trigger: NotificationTrigger;
  status: NotificationStatus;
  recipientEmail: string;   // deja mascat: "a***@example.com" — nu incercati sa-l "reparati"
  body: string;
  attempts: number;
  lastError: string | null;
  nextAttemptAt: string | null;  // LocalDateTime
  sentAt: string | null;
  createdAt: string;
};
```

Raspunsul e un `Page` Spring: `{ content: [...], totalElements, totalPages, number, size }`.

### Cand apar randurile (F-502)

| Trigger | Inserat in | `nextAttemptAt` initial |
|---|---|---|
| `CONFIRMATION` | `AppointmentService.create()` | `now()` |
| `REMINDER_24H` | `create()` si `update()` | `startTime - 24h` |
| `RESCHEDULED` | `update()` | `now()` |
| `CANCELLED` | `cancel()` | `now()` |

- Programare la mai putin de 24h distanta → **nu se insereaza** rand de reminder.
- La reprogramare, reminder-ul vechi trece pe `CANCELLED` si se insereaza unul nou, cu token nou.
- La anulare, reminder-ul `PENDING` trece pe `CANCELLED` si **nu** se mai programeaza altul.
- Tranzitiile de status (`confirm`/`check-in`/`no-show`/`complete`) **nu** genereaza notificari.
- Adresa invalida → randul se insereaza cu `status = FAILED`; crearea programarii **nu** pica.

---

## 2. Confirmare/anulare din link (Modulul 5) — public

Linkul din email e `{PUBLIC_BASE_URL}/c/{token}`, iar ruta din frontend e `/c/:token`.
Token: 16 caractere URL-safe (`SecureRandom`, 12 bytes Base64), doar pe `REMINDER_24H`.

| Metoda | Path | Raspuns |
|---|---|---|
| GET | `/public/appointments/{token}` | `200` `PublicAppointmentResponse` · `404` token inexistent · `410` programarea a trecut |
| POST | `/public/appointments/{token}/confirm` | `204` · `404` · `410` · `409` respins de masina de stari |
| POST | `/public/appointments/{token}/cancel` | `204` · `404` · `410` · `409` |

> **POST, nu PATCH.** Frontend-ul folosea initial `PATCH` si primea 405 pe ambele actiuni.

```ts
type PublicAppointmentResponse = {
  patientFirstName: string;   // doar prenumele
  startTime: string;
  endTime: string;
  doctorName: string;
  roomName: string;
  status: AppointmentStatus;
  canConfirm: boolean;        // status === 'SCHEDULED'
  canCancel: boolean;         // status === 'SCHEDULED' | 'CONFIRMED'
};
```

Pagina e accesibila fara autentificare, deci raspunsul **nu contine** nume de familie, CNP, telefon,
email, serviciu, notite sau id-uri interne. Nu cereti adaugarea lor.

Token-ul **nu** e single-use: ramane valabil pana la `startTime`, ca un pacient care a confirmat sa
poata anula mai tarziu din acelasi email. Dubla confirmare e respinsa de masina de stari cu 409.

---

## 3. Rapoarte (Modulul 6) — `ADMIN` only

`@PreAuthorize("hasRole('ADMIN')")` pe clasa (NFR-1: medicul nu are acces la date financiare).
`DOCTOR` si `RECEPTION` → **403**.

| Metoda | Path | Query | Raspuns |
|---|---|---|---|
| GET | `/api/reports/occupancy` | `from`, `to` | `OccupancyReportResponse` |
| GET | `/api/reports/no-show` | `from`, `to` | `NoShowReportResponse` |
| GET | `/api/reports/sales` | `from`, `to` | `SalesReportResponse` |
| GET | `/api/reports/{type}/export` | `from`, `to`, `format=pdf\|xlsx` | fisier binar |

**Parametrii se numesc `from` si `to`**, format `yyyy-MM-dd` (`@DateTimeFormat(ISO.DATE)`) — nu
`startDate`/`endDate`. Ambele capete sunt inclusive. Interval maxim 366 de zile, altfel `400`.

```ts
// Ratele sunt fractii 0..1 (0.734), nu 73.4. Frontend-ul inmulteste cu 100 la afisare.
type ResourceOccupancyRow = {
  resourceId: string; name: string;
  bookedMinutes: number; availableMinutes: number; occupancyRate: number;
};
type OccupancyReportResponse = {
  from: string; to: string;
  doctors: ResourceOccupancyRow[]; rooms: ResourceOccupancyRow[];
};

type PatientNoShowRow = { patientId: string; patientName: string; total: number; noShows: number; rate: number };
type WeekdayNoShowRow = { dayOfWeek: 'MONDAY' | ... | 'SUNDAY'; total: number; noShows: number; rate: number };
type NoShowReportResponse = {
  from: string; to: string;
  total: number; noShows: number; rate: number;
  byPatient: PatientNoShowRow[]; byWeekday: WeekdayNoShowRow[];
};

type SalesRow = { id: string; name: string; appointments: number; total: number };
type SalesReportResponse = {
  from: string; to: string; grandTotal: number;
  byService: SalesRow[]; byDoctor: SalesRow[];
};
```

**Atentie la nume** — acestea sunt greselile care au trebuit reparate la integrare:
`total` (nu `totalAppointments`), `rate` (nu `noShowRate`/`overallRate`), `noShows`,
`grandTotal` (nu `totalRevenue`), `name` (nu `serviceName`/`doctorName`),
`total` pe `SalesRow` (nu `revenue`), `bookedMinutes`/`availableMinutes`/`occupancyRate`
(nu `occupiedMinutes`/`totalMinutes`/`percentage`).

### Ce se numara

- **Ocupare**: numarator = minutele programarilor cu status diferit de `CANCELLED`/`NO_SHOW`.
  Numitor: pentru medici, `work_schedules` din interval; pentru cabinete, programul clinicii din
  config (08:00-20:00, Luni-Vineri). Numitor 0 → rata 0; frontend-ul afiseaza `—`.
- **No-show**: `noShows / total`, unde `total` exclude programarile `CANCELLED`.
- **Vanzari**: doar programarile `COMPLETED`, la `appointment.priceAtBooking` — pretul inghetat in
  momentul rezervarii (B7), **nu** `service.price` de azi. O schimbare de tarif nu mai rescrie
  retroactiv lunile deja raportate. Pretul se refotografiaza doar daca o reprogramare schimba
  serviciul; simpla mutare a orei pastreaza tariful acceptat de pacient.
- Filtrarea se face pe `startTime`, nu pe `createdAt`.
- Toate resursele active apar in raport, inclusiv cele cu 0 programari.

### Export

`Content-Type`: `application/pdf` sau
`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.
`Content-Disposition: attachment; filename="raport-vanzari_2026-07-01_2026-07-31.xlsx"`.

Prefixe: `raport-ocupare`, `raport-no-show`, `raport-vanzari`. Format sau tip invalid → `400`.

> **`Content-Disposition` nu e citibil din JS.** `SecurityConfig` seteaza `setAllowedHeaders("*")`
> dar nu `setExposedHeaders`, iar CORS nu expune implicit header-ul. Numele fisierului se
> construieste in frontend (vezi `components/report/ExportButtons.jsx`).
>
> **Erorile vin ca `Blob`** cand ceri `responseType: 'blob'`, deci `error.response.data.message` e
> `undefined`. Corpul trebuie citit cu `await error.response.data.text()` si abia apoi parsat.

In `.xlsx` numerele sunt celule numerice native, procentele sunt fractii cu format `0.0%`, iar
datele sunt valori de tip data cu format `dd.mm.yyyy` (cerinta explicita din F-602).

---

## 4. Calendar si programari — ce au schimbat Modulele 5-6

Rutele sunt de la Modulul 3, dar contractul lor s-a mutat aici pentru ca au fost atinse de B7 si de
filtrarea pe medic (PR #159, reintegrat).

| Metoda | Path | Query | Raspuns |
|---|---|---|---|
| GET | `/api/appointments/calendar` | `from`, `to`, `doctorIds?`, `roomId?` | `CalendarAppointmentResponse[]` |

```ts
type CalendarAppointmentResponse = {
  id: string;
  doctorId: string; doctorName: string;   // doctorId e cheia culorii din doctorColors.js
  roomId: string; roomName: string;
  patientName: string; serviceName: string;
  startTime: string; endTime: string;
  status: AppointmentStatus;
};
```

- `doctorIds` se trimite **repetat** (`?doctorIds=a&doctorIds=b`), nu ca CSV.
- `roomId` are prioritate: daca e prezent, `doctorIds` e ignorat.
- **`doctorIds` lipsa sau lista goala inseamna "toti medicii activi"**, nu "niciunul"
  (`CalendarService.getByDateRangeAndDoctors`). Deci "deselecteaza tot" din filtru **nu** are voie sa
  trimita cererea — frontend-ul afiseaza direct calendarul gol, altfel utilizatorul primea exact
  opusul a ce a cerut.
- Rolul `DOCTOR` e fortat pe propriul calendar in controller; `doctorIds` din query e ignorat pentru
  el, iar `DoctorFilterMenu` nici nu se randeaza.
- `GET /api/doctors` intoarce si medicii dezactivati, deci `DoctorResponse` are acum campul `active`;
  filtrul afiseaza doar medicii activi.

`AppointmentResponse` (de la `GET /api/appointments/{id}`) are in plus `priceAtBooking: number` —
pretul inghetat la rezervare, in RON. Poate diferi de `service.price` de azi; asta e intentia.

---

## 5. Configurare

```yaml
app:
  email:
    enabled: ${EMAIL_ENABLED:false}   # kill switch; false => LoggingEmailSender, fara SMTP real
    from-address: ${EMAIL_FROM_ADDRESS:no-reply@medsched.com}
  notifications:
    public-base-url: ${PUBLIC_BASE_URL:http://localhost:5173}
  reports:
    clinic-open-time: "08:00"
    clinic-close-time: "20:00"
    clinic-working-days: MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY
```

Cu `app.email.enabled=false` (implicit) se injecteaza `LoggingEmailSender`: scrie in log si intoarce
un id fals. Toata echipa poate rula aplicatia si toate testele fara credentiale SMTP. Doar cine face
demo-ul porneste cu `EMAIL_ENABLED=true` si `ACCOUNT_PASSWORD` din variabila de mediu.

**Credentialele nu intra niciodata in git.**
