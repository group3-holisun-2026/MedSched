# Modulul 4 — Gestiune Status și Validare Flux — Contract de Endpoint-uri

> Sursa de adevăr pentru frontend pe shape-ul exact de request/response al tranzițiilor de status
> (F-401) și al blocării fișei (F-402). Regulile de business din spatele lor sunt în
> [backend_module4_tasks.md](backend_module4_tasks.md), secțiunile 1.1–1.3.
>
> Toate endpoint-urile de mai jos sunt implementate în `AppointmentController` și verificate de
> `AppointmentStatusSecurityTest` / `AppointmentStateMachineTest`.

---

## 1. Tranziții de status — `/api/appointments/{id}`

Toate sunt `PATCH`, **fără body**, și întorc `AppointmentResponse` (200) — același DTO ca
`GET /api/appointments/{id}`.

| Method | Path | Tranziție | Roluri |
|---|---|---|---|
| PATCH | `/api/appointments/{id}/confirm` | `SCHEDULED → CONFIRMED` | ADMIN, RECEPTION |
| PATCH | `/api/appointments/{id}/check-in` | `CONFIRMED → IN_PROGRESS` | ADMIN, RECEPTION, DOCTOR (doar propria programare) |
| PATCH | `/api/appointments/{id}/no-show` | `CONFIRMED → NO_SHOW` | ADMIN, RECEPTION |
| PATCH | `/api/appointments/{id}/complete` | `IN_PROGRESS → COMPLETED` | ADMIN, DOCTOR (doar propria programare) |
| PATCH | `/api/appointments/{id}/cancel` | `SCHEDULED`/`CONFIRMED` → `CANCELLED` | ADMIN, RECEPTION |

### Matricea completă de tranziții permise (secțiunea 1.1)

```
SCHEDULED   -> CONFIRMED, CANCELLED
CONFIRMED   -> IN_PROGRESS, NO_SHOW, CANCELLED
IN_PROGRESS -> COMPLETED
COMPLETED   -> (nimic — stare terminală)
NO_SHOW     -> (nimic — stare terminală)
CANCELLED   -> (nimic — stare terminală)
```

Orice altă pereche este respinsă cu **409**.

### Coduri de răspuns

| Cod | Când |
|---|---|
| 200 | Tranziția a reușit; body-ul e `AppointmentResponse` actualizat |
| 403 | Rol fără drept pe acțiune, **sau** `DOCTOR` care nu e medicul alocat (pe `/check-in` și `/complete`) |
| 404 | Programarea nu există, sau contul curent nu e asociat unui medic |
| 409 | Tranziție invalidă din starea curentă (ex. `/confirm` pe o programare deja `CANCELLED`) |

### Shape-ul erorii

Erorile de tranziție (409) și cele de acces (403/404) sunt aruncate ca `ResponseStatusException` și
serializate de `GlobalExceptionHandler` în `ErrorResponse`:

```ts
{
  timestamp: string;   // ISO-8601
  status: number;      // 409
  error: string;       // "Conflict"
  message: string;     // "Tranziția din COMPLETED în CONFIRMED nu este permisă."
  path: string;        // "/api/appointments/{id}/confirm"
}
```

> Atenție, diferență față de Modulul 3: `ResourceConflictException` (folosit la
> creare/reprogramare pentru coliziuni de resurse) e mapat separat la un **string brut**, nu la
> `ErrorResponse`. Pentru endpoint-urile din acest modul citiți `error.response.data.message`;
> pentru conflictele de resurse din `AppointmentForm` citiți `error.response.data` direct.

---

## 2. `AppointmentResponse` — câmp nou `completedAt`

```ts
{
  id: string;                    // UUID
  patient: PatientSummary;
  doctor: DoctorSummary;
  room: RoomSummary;
  service: ServiceSummary;
  equipment: EquipmentSummary | null;
  startTime: string;             // ISO-8601, fără timezone
  endTime: string;
  status: "SCHEDULED" | "CONFIRMED" | "IN_PROGRESS" | "COMPLETED" | "NO_SHOW" | "CANCELLED";
  notes: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;    // NOU — setat o singură dată, la tranziția în COMPLETED
}
```

`completedAt` e `null` pentru orice status diferit de `COMPLETED`. Fereastra de grație se
calculează local: fișa rămâne editabilă până la `completedAt + 30 minute`.

## 3. `DoctorSummary` — câmp nou `userId`

```ts
{
  id: string;        // UUID-ul medicului (Doctor.id)
  userId: string;    // NOU — UUID-ul contului (User.id)
  username: string;
  speciality: string;
}
```

`userId` există ca frontend-ul să poată decide dacă programarea afișată e „a mea" pentru un user cu
rol `DOCTOR`: comparați `appointment.doctor.userId` cu `user.id` din `AuthContext`. Nu vă bazați doar
pe ascunderea butonului — backend-ul respinge oricum cu 403.

---

## 4. Fișa de consultație — `/api/appointments/{appointmentId}/record`

Path-urile rămân cele din Modulul 2; se schimbă doar regulile de acces și de blocare.

| Method | Path | Acces |
|---|---|---|
| GET | `/api/appointments/{appointmentId}/record` | DOCTOR, ADMIN |
| POST | `/api/appointments/{appointmentId}/record` | ADMIN, DOCTOR **doar pe propria programare** |
| PUT | `/api/appointments/{appointmentId}/record` | ADMIN, DOCTOR **doar pe propria programare** |

- **403** dacă rolul e `DOCTOR` și `appointment.doctor.id` nu e medicul curent (nou în Modulul 4 —
  înainte orice `DOCTOR` putea edita orice fișă). `ADMIN` rămâne exceptat.
- **Blocarea (F-402) nu e sincronă cu `/complete`.** `ConsultationRecordLockScheduler` rulează o dată
  pe minut și cheamă `lock()` pentru programările `COMPLETED` cu `completedAt <= now() - 30 min` a
  căror fișă nu e deja blocată. Până atunci `record.locked` rămâne `false` și fișa e editabilă.
- Odată `locked = true`, `PUT` nu mai trece — afișați mesajul de fișă blocată, nu bannerul de grație.
