# Modulul 1: Administrare Resurse Clinică

Documentație tehnică pentru implementarea Modulului 1 — cuprinde structura de clase (entități, DTO-uri, mapper-e, servicii, controllere) necesară pentru F-101, F-102, F-103, cu exemple de cod pentru fiecare strat.

---

## Cuprins

- [Cerințe funcționale](#cerințe-funcționale)
- [Arhitectură pe straturi](#arhitectură-pe-straturi)
- [1. Entități (JPA)](#1-entități-jpa)
- [2. Enums](#2-enums)
- [3. Repositories](#3-repositories)
- [4. DTO-uri](#4-dto-uri)
- [5. Mappers (MapStruct)](#5-mappers-mapstruct)
- [6. Service layer (interfețe + implementări)](#6-service-layer-interfețe--implementări)
- [7. Controllers](#7-controllers)
- [8. Validări și excepții](#8-validări-și-excepții)
- [9. Puncte de integrare cu alte module](#9-puncte-de-integrare-cu-alte-module)
- [10. Structură de foldere recomandată](#10-structură-de-foldere-recomandată)

---

## Cerințe funcționale

| Cod | Descriere |
|-----|-----------|
| **F-101** | Management Personal Medical — medici (specialitate, contact, orar săptămânal flexibil, durata standard a unei consultații) |
| **F-102** | Management Spații și Echipamente — cabinete fizice, echipamente medicale de inventar (partajate) |
| **F-103** | Catalog de Servicii — servicii oferite (preț, durată implicită, resurse hardware obligatorii) |

---

## Arhitectură pe straturi

```
Controller (@RestController)
      │  primește request-uri HTTP, validează DTO-uri, nu conține logică de business
      ▼
Service (interfață + impl @Service)
      │  logică de business, validări, tranzacții
      ▼
Repository (@JpaRepository)
      │  acces la date
      ▼
Entity (@Entity)
```

Fiecare service are o **interfață separată** de implementare, astfel încât alte module (ex. Programări) să poată depinde de contract, nu de implementare concretă — permite dezvoltare paralelă și testare cu mock-uri.

---

## 1. Entități (JPA)

`backend/entity/`

```java
@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String specialty;

    @Column(name = "contact_info")
    private String contactInfo;

    @Column(name = "standard_duration_minutes", nullable = false)
    private int standardConsultationDurationMinutes;

    @OneToMany(mappedBy = "doctor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkingHours> weeklySchedule = new ArrayList<>();

    // getters / setters
}
```

```java
@Entity
@Table(name = "working_hours")
public class WorkingHours {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    // getters / setters
}
```

```java
@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(nullable = false)
    private boolean active = true;

    // getters / setters
}
```

```java
@Entity
@Table(name = "equipment")
public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    // getters / setters
}
```

```java
@Entity
@Table(name = "clinical_services")
public class ClinicalService {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "default_duration_minutes", nullable = false)
    private int defaultDurationMinutes;

    @ManyToMany
    @JoinTable(
        name = "service_equipment",
        joinColumns = @JoinColumn(name = "service_id"),
        inverseJoinColumns = @JoinColumn(name = "equipment_id")
    )
    private List<Equipment> requiredEquipment = new ArrayList<>();

    // getters / setters
}
```

---

## 2. Enums

`backend/enums/`

Pentru zi din săptămână se folosește direct `java.time.DayOfWeek` — nu e nevoie de enum propriu. Dacă vrei categorii de echipamente (opțional, nu e cerut explicit de F-102):

```java
public enum EquipmentType {
    IMAGING,
    DIAGNOSTIC,
    MONITORING,
    OTHER
}
```

---

## 3. Repositories

`backend/repository/`

```java
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
    List<Doctor> findBySpecialtyIgnoreCase(String specialty);
    Optional<Doctor> findByUserId(UUID userId);
}
```

```java
public interface RoomRepository extends JpaRepository<Room, UUID> {
    boolean existsByNameIgnoreCase(String name);
}
```

```java
public interface EquipmentRepository extends JpaRepository<Equipment, UUID> {
    boolean existsByNameIgnoreCase(String name);
}
```

```java
public interface ClinicalServiceRepository extends JpaRepository<ClinicalService, UUID> {
    List<ClinicalService> findByNameContainingIgnoreCase(String name);
}
```

---

## 4. DTO-uri

`backend/dto/`

DTO-urile separă complet forma datelor expuse către client de structura internă a entităților. Regula: **request** pentru input (create/update), **response** pentru output.

```java
public record WorkingHoursDto(
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime
) {}
```

```java
public record DoctorCreateRequest(
    @NotNull UUID userId,
    @NotBlank String specialty,
    String contactInfo,
    @Positive int standardConsultationDurationMinutes,
    @NotEmpty List<WorkingHoursDto> weeklySchedule
) {}
```

```java
public record DoctorUpdateRequest(
    @NotBlank String specialty,
    String contactInfo,
    @Positive int standardConsultationDurationMinutes,
    List<WorkingHoursDto> weeklySchedule
) {}
```

```java
public record DoctorResponse(
    UUID id,
    String fullName,
    String specialty,
    String contactInfo,
    int standardConsultationDurationMinutes,
    List<WorkingHoursDto> weeklySchedule
) {}
```

```java
public record RoomRequest(
    @NotBlank String name,
    String description
) {}

public record RoomResponse(
    UUID id,
    String name,
    String description,
    boolean active
) {}
```

```java
public record EquipmentRequest(
    @NotBlank String name
) {}

public record EquipmentResponse(
    UUID id,
    String name,
    boolean active
) {}
```

```java
public record ClinicalServiceRequest(
    @NotBlank String name,
    @Positive BigDecimal price,
    @Positive int defaultDurationMinutes,
    @NotEmpty List<UUID> requiredEquipmentIds
) {}

public record ClinicalServiceResponse(
    UUID id,
    String name,
    BigDecimal price,
    int defaultDurationMinutes,
    List<EquipmentResponse> requiredEquipment
) {}
```

> Folosind `record`-uri, DTO-urile devin imutabile, fără boilerplate de getteri/setteri/`equals`/`hashCode`.

---

## 5. Mappers (MapStruct)

`backend/mapper/`

MapStruct generează implementarea la compile-time — se scrie doar interfața.

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.6.3</version>
</dependency>
```

```java
@Mapper(componentModel = "spring")
public interface DoctorMapper {

    @Mapping(target = "fullName", source = "user.fullName")
    DoctorResponse toResponse(Doctor doctor);

    List<WorkingHoursDto> toWorkingHoursDtoList(List<WorkingHours> workingHours);

    @Mapping(target = "doctor", ignore = true)
    WorkingHours toEntity(WorkingHoursDto dto);
}
```

```java
@Mapper(componentModel = "spring")
public interface RoomMapper {
    Room toEntity(RoomRequest dto);
    RoomResponse toResponse(Room room);
}
```

```java
@Mapper(componentModel = "spring")
public interface EquipmentMapper {
    Equipment toEntity(EquipmentRequest dto);
    EquipmentResponse toResponse(Equipment equipment);
    List<EquipmentResponse> toResponseList(List<Equipment> equipmentList);
}
```

```java
@Mapper(componentModel = "spring", uses = EquipmentMapper.class)
public interface ClinicalServiceMapper {

    @Mapping(target = "requiredEquipment", ignore = true) // setat manual în service (după fetch din DB)
    ClinicalService toEntity(ClinicalServiceRequest dto);

    ClinicalServiceResponse toResponse(ClinicalService service);
}
```

> De ce `requiredEquipment` e ignorat la mapping: DTO-ul primește doar `List<UUID>`, iar entitatea are nevoie de obiecte `Equipment` reale — conversia se face în service, după ce echipamentele sunt încărcate din `EquipmentRepository`.

---

## 6. Service layer (interfețe + implementări)

`backend/service/` (interfețe) și `backend/service/impl/` (implementări `@Service`)

```java
public interface DoctorService {
    DoctorResponse create(DoctorCreateRequest dto);
    DoctorResponse update(UUID id, DoctorUpdateRequest dto);
    DoctorResponse getById(UUID id);
    List<DoctorResponse> getAll();
    void delete(UUID id);
    List<WorkingHoursDto> getWorkingHours(UUID doctorId);
}
```

```java
@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final DoctorMapper doctorMapper;

    @Override
    @Transactional
    public DoctorResponse create(DoctorCreateRequest dto) {
        User user = userRepository.findById(dto.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + dto.userId()));

        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setSpecialty(dto.specialty());
        doctor.setContactInfo(dto.contactInfo());
        doctor.setStandardConsultationDurationMinutes(dto.standardConsultationDurationMinutes());

        List<WorkingHours> schedule = dto.weeklySchedule().stream()
            .map(doctorMapper::toEntity)
            .peek(wh -> wh.setDoctor(doctor))
            .toList();
        doctor.setWeeklySchedule(schedule);

        Doctor saved = doctorRepository.save(doctor);
        return doctorMapper.toResponse(saved);
    }

    @Override
    public DoctorResponse getById(UUID id) {
        Doctor doctor = doctorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + id));
        return doctorMapper.toResponse(doctor);
    }

    @Override
    public List<DoctorResponse> getAll() {
        return doctorRepository.findAll().stream()
            .map(doctorMapper::toResponse)
            .toList();
    }

    @Override
    public List<WorkingHoursDto> getWorkingHours(UUID doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
            .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));
        return doctorMapper.toWorkingHoursDtoList(doctor.getWeeklySchedule());
    }

    // update() și delete() urmează același pattern
}
```

```java
public interface ClinicalServiceService {
    ClinicalServiceResponse create(ClinicalServiceRequest dto);
    ClinicalServiceResponse update(UUID id, ClinicalServiceRequest dto);
    ClinicalServiceResponse getById(UUID id);
    List<ClinicalServiceResponse> getAll();
    void delete(UUID id);
}
```

```java
@Service
@RequiredArgsConstructor
public class ClinicalServiceServiceImpl implements ClinicalServiceService {

    private final ClinicalServiceRepository serviceRepository;
    private final EquipmentRepository equipmentRepository;
    private final ClinicalServiceMapper mapper;

    @Override
    @Transactional
    public ClinicalServiceResponse create(ClinicalServiceRequest dto) {
        List<Equipment> equipment = equipmentRepository.findAllById(dto.requiredEquipmentIds());
        if (equipment.size() != dto.requiredEquipmentIds().size()) {
            throw new ResourceNotFoundException("One or more equipment IDs are invalid");
        }

        ClinicalService entity = mapper.toEntity(dto);
        entity.setRequiredEquipment(equipment);

        return mapper.toResponse(serviceRepository.save(entity));
    }

    @Override
    public ClinicalServiceResponse getById(UUID id) {
        ClinicalService entity = serviceRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Service not found: " + id));
        return mapper.toResponse(entity);
    }

    @Override
    public List<ClinicalServiceResponse> getAll() {
        return serviceRepository.findAll().stream().map(mapper::toResponse).toList();
    }

    // update() / delete() urmează același pattern
}
```

`RoomService` și `EquipmentService` urmează exact același pattern CRUD simplu (create / update / getAll / delete), doar fără logica suplimentară de rezolvare a relațiilor.

---

## 7. Controllers

`backend/controller/`

```java
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping
    public ResponseEntity<DoctorResponse> create(@Valid @RequestBody DoctorCreateRequest dto) {
        DoctorResponse response = doctorService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DoctorResponse> update(
        @PathVariable UUID id,
        @Valid @RequestBody DoctorUpdateRequest dto
    ) {
        return ResponseEntity.ok(doctorService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(doctorService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<DoctorResponse>> getAll() {
        return ResponseEntity.ok(doctorService.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        doctorService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

`RoomController`, `EquipmentController` și `ClinicalServiceController` urmează același pattern REST (`/api/rooms`, `/api/equipment`, `/api/services`), cu request/response DTO-urile corespunzătoare.

---

## 8. Validări și excepții

Reutilizate din `GlobalExceptionHandler` existent (`@RestControllerAdvice`):

| Excepție | Cod HTTP | Caz de utilizare în Modulul 1 |
|----------|----------|-------------------------------|
| `ResourceNotFoundException` | 404 | doctor/cabinet/echipament/serviciu inexistent |
| `ConflictException` | 409 | nume duplicat (cabinet, echipament), `equipmentIds` invalide |
| `MethodArgumentNotValidException` | 400 | validare Bean Validation eșuată pe DTO (`@NotBlank`, `@Positive`) |

Exemplu de validare suplimentară în service (nu doar la nivel de DTO):

```java
if (roomRepository.existsByNameIgnoreCase(dto.name())) {
    throw new ConflictException("Room with name '" + dto.name() + "' already exists");
}
```

---

## 9. Puncte de integrare cu alte module

Modulul 2 (Programări) va depinde de aceste **interfețe**, nu de implementări:

```java
// Verificare disponibilitate doctor la programare
List<WorkingHoursDto> schedule = doctorService.getWorkingHours(doctorId);

// Aflare durată + resurse necesare pentru un serviciu
ClinicalServiceResponse service = clinicalServiceService.getById(serviceId);
int durationMinutes = service.defaultDurationMinutes();
List<EquipmentResponse> requiredEquipment = service.requiredEquipment();
```

Această separare pe interfețe permite echipelor să lucreze **în paralel**: Modulul 2 poate fi dezvoltat pe baza contractului `DoctorService` / `ClinicalServiceService` chiar înainte ca implementarea completă din Modulul 1 să fie gata (folosind mock-uri în teste).

---

## 10. Structură de foldere recomandată

```
backend/
├── controller/
│   ├── DoctorController.java
│   ├── RoomController.java
│   ├── EquipmentController.java
│   └── ClinicalServiceController.java
├── service/
│   ├── DoctorService.java
│   ├── RoomService.java
│   ├── EquipmentService.java
│   ├── ClinicalServiceService.java
│   └── impl/
│       ├── DoctorServiceImpl.java
│       ├── RoomServiceImpl.java
│       ├── EquipmentServiceImpl.java
│       └── ClinicalServiceServiceImpl.java
├── repository/
│   ├── DoctorRepository.java
│   ├── WorkingHoursRepository.java
│   ├── RoomRepository.java
│   ├── EquipmentRepository.java
│   └── ClinicalServiceRepository.java
├── entity/
│   ├── Doctor.java
│   ├── WorkingHours.java
│   ├── Room.java
│   ├── Equipment.java
│   └── ClinicalService.java
├── dto/
│   ├── DoctorCreateRequest.java
│   ├── DoctorUpdateRequest.java
│   ├── DoctorResponse.java
│   ├── WorkingHoursDto.java
│   ├── RoomRequest.java
│   ├── RoomResponse.java
│   ├── EquipmentRequest.java
│   ├── EquipmentResponse.java
│   ├── ClinicalServiceRequest.java
│   └── ClinicalServiceResponse.java
├── mapper/
│   ├── DoctorMapper.java
│   ├── RoomMapper.java
│   ├── EquipmentMapper.java
│   └── ClinicalServiceMapper.java
└── exception/
    ├── ResourceNotFoundException.java
    ├── ConflictException.java
    └── GlobalExceptionHandler.java
```