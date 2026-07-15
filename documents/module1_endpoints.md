# Modulul 1 — Administrare Resurse Clinică — Contract DTO pentru Frontend

> Acest fișier descrie shape-ul exact al request/response body-urilor pe care le va expune API-ul.
> Backend-ul (entities/service/mapper) e încă în lucru, dar **acest contract e stabil** — puteți începe integrarea/mock-urile pe baza lui.

---

## 1. Doctor — `/api/doctors`

### `DoctorCreateRequest` (POST body)
```ts
{
  userId: string;                              // UUID
  specialty: string;                            // required
  contactInfo?: string;
  standardConsultationDurationMinutes: number;   // > 0
  weeklySchedule: WorkingHoursDto[];             // required, min 1 element
}
```

### `DoctorUpdateRequest` (PUT body)
```ts
{
  specialty: string;                             // required
  contactInfo?: string;
  standardConsultationDurationMinutes: number;    // > 0
  weeklySchedule?: WorkingHoursDto[];
}
```

### `DoctorResponse`
```ts
{
  id: string;                 // UUID
  fullName: string;
  specialty: string;
  contactInfo?: string;
  standardConsultationDurationMinutes: number;
  weeklySchedule: WorkingHoursDto[];
}
```

### `WorkingHoursDto` (folosit în request și response)
```ts
{
  dayOfWeek: "MONDAY" | "TUESDAY" | "WEDNESDAY" | "THURSDAY" | "FRIDAY" | "SATURDAY" | "SUNDAY";
  startTime: string;   // "HH:mm:ss", ex: "08:00:00"
  endTime: string;      // "HH:mm:ss", ex: "14:00:00"
}
```

### Endpoints
| Method | Path | Body | Response |
|--------|------|------|----------|
| POST | `/api/doctors` | `DoctorCreateRequest` | `DoctorResponse` (201) |
| PUT | `/api/doctors/{id}` | `DoctorUpdateRequest` | `DoctorResponse` (200) |
| GET | `/api/doctors/{id}` | — | `DoctorResponse` (200) |
| GET | `/api/doctors` | — | `DoctorResponse[]` (200) |
| DELETE | `/api/doctors/{id}` | — | 204, no body |
| GET | `/api/doctors/{id}/working-hours` | — | `WorkingHoursDto[]` (200) |

---

## 2. Room — `/api/rooms`

### `RoomRequest` (POST / PUT body)
```ts
{
  name: string;         // required, unic
  description?: string;
}
```

### `RoomResponse`
```ts
{
  id: string;            // UUID
  name: string;
  description?: string;
  active: boolean;
}
```

### Endpoints
| Method | Path | Body | Response |
|--------|------|------|----------|
| POST | `/api/rooms` | `RoomRequest` | `RoomResponse` (201) |
| PUT | `/api/rooms/{id}` | `RoomRequest` | `RoomResponse` (200) |
| GET | `/api/rooms/{id}` | — | `RoomResponse` (200) |
| GET | `/api/rooms` | — | `RoomResponse[]` (200) |
| DELETE | `/api/rooms/{id}` | — | 204, no body |

---

## 3. Equipment — `/api/equipment`

### `EquipmentRequest` (POST / PUT body)
```ts
{
  name: string;   // required, unic
}
```

### `EquipmentResponse`
```ts
{
  id: string;      // UUID
  name: string;
  active: boolean;
}
```

### Endpoints
| Method | Path | Body | Response |
|--------|------|------|----------|
| POST | `/api/equipment` | `EquipmentRequest` | `EquipmentResponse` (201) |
| PUT | `/api/equipment/{id}` | `EquipmentRequest` | `EquipmentResponse` (200) |
| GET | `/api/equipment/{id}` | — | `EquipmentResponse` (200) |
| GET | `/api/equipment` | — | `EquipmentResponse[]` (200) |
| DELETE | `/api/equipment/{id}` | — | 204, no body |

---

## 4. Clinical Service — `/api/services`

### `ClinicalServiceRequest` (POST / PUT body)
```ts
{
  name: string;                        // required
  price: number;                       // > 0, decimal (BigDecimal pe backend)
  defaultDurationMinutes: number;      // > 0
  requiredEquipmentIds: string[];      // UUID[], min 1 element — ID-urile echipamentelor din /api/equipment
}
```

### `ClinicalServiceResponse`
```ts
{
  id: string;                          // UUID
  name: string;
  price: number;
  defaultDurationMinutes: number;
  requiredEquipment: EquipmentResponse[];   // obiecte complete, nu doar ID-uri
}
```

### Endpoints
| Method | Path | Body | Response |
|--------|------|------|----------|
| POST | `/api/services` | `ClinicalServiceRequest` | `ClinicalServiceResponse` (201) |
| PUT | `/api/services/{id}` | `ClinicalServiceRequest` | `ClinicalServiceResponse` (200) |
| GET | `/api/services/{id}` | — | `ClinicalServiceResponse` (200) |
| GET | `/api/services` | — | `ClinicalServiceResponse[]` (200) |
| DELETE | `/api/services/{id}` | — | 204, no body |

---

## Erori — format comun (`GlobalExceptionHandler`)

| Cod HTTP | Când apare | Exemplu |
|----------|-----------|---------|
| 400 | Validare eșuată pe body (`@NotBlank`, `@Positive`, etc.) | lipsă `name`, `price` negativ |
| 404 | Resursă inexistentă (doctor/room/equipment/service, sau `userId`/`requiredEquipmentIds` invalide) | GET/PUT/DELETE pe ID inexistent |
| 409 | Conflict — nume duplicat (Room, Equipment) | `name` deja folosit |

> Notă: momentan nu există autentificare/roluri pe aceste endpoint-uri — se va adăuga ulterior JWT + roluri (`ADMIN`, `RECEPTIONIST`, etc.), dar shape-ul body-urilor de mai sus rămâne neschimbat.

---

**Status:** contract stabil, poate fi folosit pentru mock-uri / integrare frontend fără să aștepte implementarea backend.