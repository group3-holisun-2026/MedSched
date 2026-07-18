# Modulul 2 — Gestiune Pacienți și Fișe Medicale — Contract DTO pentru Frontend

> Acest fișier descrie shape-ul exact al request/response body-urilor expuse de API. Entity/repository/service
> pentru `Patient`, `ConsultationRecord` și `AuditLog` sunt încă în lucru (P1/P2/P3), dar **controller-ele și
> shape-ul DTO-urilor de mai jos sunt stabile** — puteți începe integrarea/mock-urile pe baza lui, la fel ca la
> Modulul 1.

---

## 1. Patient — `/api/patients`

**Acces:** orice staff autentificat — `ADMIN`, `DOCTOR`, `RECEPTION`.

### `PatientQuickCreateRequest` (POST body)
```ts
{
  firstName: string;   // required
  lastName: string;    // required
  phone: string;        // required
}
```
Creare rapidă de recepție (la telefon/ghișeu) — restul câmpurilor rămân `null`, iar `profileComplete` iese
mereu `false`.

### `PatientRequest` (PUT body)
```ts
{
  firstName: string;              // required
  lastName: string;                // required
  phone: string;                    // required
  cnp?: string;                     // exact 13 cifre daca e trimis
  dateOfBirth?: string;             // "YYYY-MM-DD", trebuie sa fie in trecut
  email?: string;
  allergies?: string;
  medicalHistory?: string;
}
```
Folosit atât pentru editare normală cât și pentru completarea profilului (recepția revine și adaugă
CNP/data nașterii/etc.). `profileComplete` se recalculează automat: devine `true` de îndată ce `cnp` și
`dateOfBirth` sunt ambele nenule în urma acelui `update`.

### `PatientResponse`
```ts
{
  id: string;                 // UUID
  firstName: string;
  lastName: string;
  phone: string;
  cnp?: string;                // decriptat, doar pentru utilizatori autorizati
  dateOfBirth?: string;        // "YYYY-MM-DD"
  email?: string;
  allergies?: string;
  medicalHistory?: string;
  profileComplete: boolean;
  createdAt: string;            // ISO datetime
}
```

### Endpoints
| Method | Path | Body | Response | Acces |
|--------|------|------|----------|-------|
| GET | `/api/patients?search=` | — | `PatientResponse[]` | ADMIN, DOCTOR, RECEPTION |
| GET | `/api/patients/{id}` | — | `PatientResponse` | ADMIN, DOCTOR, RECEPTION |
| POST | `/api/patients` | `PatientQuickCreateRequest` | `PatientResponse` (201) | ADMIN, DOCTOR, RECEPTION |
| PUT | `/api/patients/{id}` | `PatientRequest` | `PatientResponse` (200) | ADMIN, DOCTOR, RECEPTION |
| GET | `/api/patients/incomplete?search=&sort=lastName,asc` | — | `Page<PatientResponse>` | ADMIN, DOCTOR, RECEPTION |

Ultimul endpoint alimentează widget-ul de pe dashboard ("pacienți de completat"). `sort` e parametru
standard Spring Data (`<coloană>,<asc|desc>`, ex. `lastName,asc` sau `createdAt,desc`), implicit
`createdAt,asc`. Răspunsul `Page<PatientResponse>` are shape-ul standard Spring Data:
```ts
{
  content: PatientResponse[];
  totalElements: number;
  totalPages: number;
  number: number;   // pagina curenta, 0-indexed
  size: number;
}
```

Nu există `DELETE` — pacienții nu se șterg (păstrare istoric legal).

---

## 2. Consultation Record — `/api/appointments/{appointmentId}/record`

**Acces:** doar `DOCTOR` și `ADMIN` — `RECEPTION` primește **403** (cerință de securitate F-202, conținutul
fișei medicale e protejat împotriva vizualizării neautorizate).

### `ConsultationRecordRequest` (POST / PUT body)
```ts
{
  presentationMotive?: string;   // Motivele prezentarii
  anamnesis?: string;
  clinicalExam?: string;         // Examen clinic
  diagnosis?: string;            // codificat sau text liber
  prescription?: string;         // Recomandari / Reteta eliberata
}
```

### `ConsultationRecordResponse`
```ts
{
  id: string;               // UUID
  appointmentId: string;    // UUID
  presentationMotive?: string;
  anamnesis?: string;
  clinicalExam?: string;
  diagnosis?: string;
  prescription?: string;
  locked: boolean;           // true dupa ce programarea a ajuns COMPLETED (Modulul 3/4)
  createdAt: string;         // ISO datetime
  updatedAt: string;         // ISO datetime
}
```

### Endpoints
| Method | Path | Body | Response |
|--------|------|------|----------|
| GET | `/api/appointments/{appointmentId}/record` | — | `ConsultationRecordResponse` (200) |
| POST | `/api/appointments/{appointmentId}/record` | `ConsultationRecordRequest` | `ConsultationRecordResponse` (201) |
| PUT | `/api/appointments/{appointmentId}/record` | `ConsultationRecordRequest` | `ConsultationRecordResponse` (200) |

`PUT` întoarce **409** dacă fișa e deja `locked`. `POST` întoarce **409** dacă există deja o fișă pentru
acel `appointmentId` (relație 1-1).

---

## 3. Audit Log — `/api/audit-log`

**Acces:** doar `ADMIN`. Read-only (repository-ul e append-only, nu există endpoint de scriere directă —
log-urile se creează automat de `AuditLoggingAspect` pe metodele adnotate `@Audited`).

### `AuditLogResponse`
```ts
{
  id: string;              // UUID
  userId: string;          // UUID
  action: "CREATE" | "READ" | "UPDATE" | "DELETE";
  entityName: string;
  entityId: string;         // UUID
  timestamp: string;        // ISO datetime
}
```

### Endpoints
| Method | Path | Query params | Response | Acces |
|--------|------|---------------|----------|-------|
| GET | `/api/audit-log` | `user` (UUID, required), `from` (ISO datetime, required), `to` (ISO datetime, required) | `AuditLogResponse[]` | ADMIN |

---

## Erori — format comun (`GlobalExceptionHandler`)

| Cod HTTP | Când apare | Exemplu |
|----------|-----------|---------|
| 400 | Validare eșuată pe body (`@NotBlank`, `@Pattern`, `@Past`, etc.) | CNP cu altă lungime decât 13 cifre |
| 403 | Rol insuficient (`@PreAuthorize`) | `RECEPTION` pe `/api/appointments/{id}/record` |
| 404 | Resursă inexistentă | GET pe `patientId`/`appointmentId` inexistent |
| 409 | Conflict — CNP duplicat, fișă deja existentă, sau editare pe fișă `locked` | al doilea `POST` pe același `appointmentId` |

> Notă: toate endpoint-urile de mai sus necesită JWT valid (`Authorization: Bearer <token>`), la fel ca
> restul API-ului — vezi `SecurityConfig`.

---

**Status:** contract stabil pentru controller-e (P5). Entity/repository/service reale pentru
`Patient` (P1), `ConsultationRecord` (P2) și `AuditLog`/`@Audited` (P3) sunt încă în lucru — până atunci,
endpoint-urile de mai sus întorc **500** cu mesaj `"... nu e inca implementat de P{1,2,3}"` (placeholder-e
temporare, vezi `backend/src/main/java/com/holisun/backend/service/*ServiceImpl.java`).
