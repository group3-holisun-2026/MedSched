package com.holisun.backend.service;

import com.holisun.backend.dto.AppointmentRequest;
import com.holisun.backend.entity.Doctor;
import com.holisun.backend.entity.Patient;
import com.holisun.backend.entity.Room;
import com.holisun.backend.entity.User;
import com.holisun.backend.entity.WorkSchedule;
import com.holisun.backend.enums.Role;
import com.holisun.backend.repository.DoctorRepository;
import com.holisun.backend.repository.PatientRepository;
import com.holisun.backend.repository.RoomRepository;
import com.holisun.backend.repository.ServiceRepository;
import com.holisun.backend.repository.UserRepository;
import com.holisun.backend.exception.ResourceConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = "spring.profiles.active=test")
@ActiveProfiles("test")
public class AvailabilityConcurrencyIntegrationTest {

    @Autowired private AppointmentService appointmentService;

    // Injectăm toate repository-urile necesare pentru a crea Fixtures
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private UserRepository userRepository; // Necesar pentru că Doctor depinde de User
    @Autowired private DataSource dataSource;

    /*
     * Profilul "test" merge cu ddl-auto=create-drop si flyway.enabled=false (vezi
     * application-test.yml), deci migratia V7__.sql (constrangerile EXCLUDE, plasa de siguranta
     * anti-coliziune la nivel de DB) nu ruleaza niciodata aici — Hibernate creeaza doar tabela, fara
     * constrangeri custom in SQL brut. Le adaugam idempotent aici, direct pe schema deja creata de
     * Hibernate, ca acest test sa verifice efectiv garantia pe care o cere NFR-2, nu doar verificarea
     * Java (care singura nu prinde doua tranzactii pornite in aceeasi fereastra de timp).
     */
    @BeforeEach
    void ensureExclusionConstraints() throws SQLException {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("CREATE EXTENSION IF NOT EXISTS btree_gist");
            addConstraintIfMissing(st, "excl_appointments_doctor_overlap", """
                    ALTER TABLE appointments ADD CONSTRAINT excl_appointments_doctor_overlap
                    EXCLUDE USING gist (doctor_id WITH =, tsrange(start_time, end_time) WITH &&)
                    WHERE (status NOT IN ('CANCELLED', 'NO_SHOW'))
                    """);
            addConstraintIfMissing(st, "excl_appointments_room_overlap", """
                    ALTER TABLE appointments ADD CONSTRAINT excl_appointments_room_overlap
                    EXCLUDE USING gist (room_id WITH =, tsrange(start_time, end_time) WITH &&)
                    WHERE (status NOT IN ('CANCELLED', 'NO_SHOW'))
                    """);
            addConstraintIfMissing(st, "excl_appointments_equipment_overlap", """
                    ALTER TABLE appointments ADD CONSTRAINT excl_appointments_equipment_overlap
                    EXCLUDE USING gist (equipment_id WITH =, tsrange(start_time, end_time) WITH &&)
                    WHERE (equipment_id IS NOT NULL AND status NOT IN ('CANCELLED', 'NO_SHOW'))
                    """);
        }
    }

    private void addConstraintIfMissing(Statement st, String constraintName, String ddl) throws SQLException {
        try (ResultSet rs = st.executeQuery(
                "SELECT 1 FROM pg_constraint WHERE conname = '" + constraintName + "'")) {
            if (!rs.next()) {
                st.execute(ddl);
            }
        }
    }

    @Test
    public void testConcurrentRoomReservation() throws InterruptedException {
        // --- 1. PREGĂTIRE FIXTURES (Baza de date) ---
        Doctor doc1 = createTestDoctor();
        Doctor doc2 = createTestDoctor();
        Room sharedRoom = createTestRoom(); // Resursa comună
        Patient pat1 = createTestPatient();
        Patient pat2 = createTestPatient();
        com.holisun.backend.entity.Service serv = createTestService();

        LocalDateTime time = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

        // --- 2. IZOLARE ---
        // 2 doctori diferiți, 2 pacienți diferiți, dar ACEEAȘI cameră
        AppointmentRequest req1 = new AppointmentRequest(pat1.getId(), doc1.getId(), sharedRoom.getId(), serv.getId(), time, "Test Cam 1");
        AppointmentRequest req2 = new AppointmentRequest(pat2.getId(), doc2.getId(), sharedRoom.getId(), serv.getId(), time, "Test Cam 2");

        runConcurrencyTest(req1, req2);
    }

    @Test
    public void testConcurrentDoctorReservation() throws InterruptedException {
        // --- 1. PREGĂTIRE FIXTURES (Baza de date) ---
        Doctor sharedDoctor = createTestDoctor(); // Resursa comună
        Room room1 = createTestRoom();
        Room room2 = createTestRoom();
        Patient pat1 = createTestPatient();
        Patient pat2 = createTestPatient();
        com.holisun.backend.entity.Service serv = createTestService();

        LocalDateTime time = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

        // --- 2. IZOLARE ---
        // 2 camere diferite, 2 pacienți diferiți, dar ACELAȘI medic
        AppointmentRequest req1 = new AppointmentRequest(pat1.getId(), sharedDoctor.getId(), room1.getId(), serv.getId(), time, "Test Doc 1");
        AppointmentRequest req2 = new AppointmentRequest(pat2.getId(), sharedDoctor.getId(), room2.getId(), serv.getId(), time, "Test Doc 2");

        runConcurrencyTest(req1, req2);
    }

    // --- METODE HELPER PENTRU GENERAREA DE DATE VALIDE ---

    private Doctor createTestDoctor() {
        User user = new User();
        String randomSuffix = UUID.randomUUID().toString().substring(0, 5);
        user.setUsername("doc_" + randomSuffix);
        user.setEmail("doc_" + randomSuffix + "@test.com");
        user.setPasswordHash("Parola123!");
        user.setRole(Role.DOCTOR);      // <-- Adăugat aici la cererea ei
        user.setEnabled(true);          // <-- Asigurat că e activ
        user = userRepository.save(user);

        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setSpeciality("General");
        doctor.setStandardConsultationDurationMinutes(30);
        doctor.setActive(true);

        // Program pe toate zilele saptamanii, ca testul sa nu depinda de ce zi cade "maine"
        // fata de data la care ruleaza (LocalDateTime.now().plusDays(1) variaza).
        Arrays.stream(DayOfWeek.values()).forEach(day -> {
            WorkSchedule schedule = new WorkSchedule();
            schedule.setDoctor(doctor);
            schedule.setDayOfWeek(day);
            schedule.setStartTime(LocalTime.of(8, 0));
            schedule.setEndTime(LocalTime.of(20, 0));
            doctor.getWeeklySchedule().add(schedule);
        });

        return doctorRepository.save(doctor);
    }

    private Room createTestRoom() {
        Room room = new Room();
        // Numele trebuie să fie unic (am pus UUID ca să nu se bată cap în cap la rulări multiple)
        room.setName("Cam " + UUID.randomUUID().toString().substring(0, 8));
        room.setActive(true);
        return roomRepository.save(room);
    }

    private Patient createTestPatient() {
        Patient patient = new Patient();
        patient.setFirstName("Ion");
        patient.setLastName("Test");
        patient.setPhone("0700000000");
        return patientRepository.save(patient);
    }

    private com.holisun.backend.entity.Service createTestService() {
        com.holisun.backend.entity.Service service = new com.holisun.backend.entity.Service();
        service.setName("Serv " + UUID.randomUUID().toString().substring(0, 8)); // Nume unic obligatoriu
        service.setPrice(new BigDecimal("100.00"));
        service.setDefaultDurationMinutes(30);
        service.setActive(true);
        return serviceRepository.save(service);
    }

    // --- LOGICA DE CONCURENȚĂ ---

    private void runConcurrencyTest(AppointmentRequest req1, AppointmentRequest req2) throws InterruptedException {
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Runnable task1 = () -> executeTask(req1, startLatch, endLatch, successCount, failCount);
        Runnable task2 = () -> executeTask(req2, startLatch, endLatch, successCount, failCount);

        executorService.submit(task1);
        executorService.submit(task2);

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        assertEquals(1, successCount.get(), "O singură programare ar fi trebuit să reușească.");
        assertEquals(1, failCount.get(), "A doua programare ar fi trebuit să fie respinsă cu conflict.");
    }

    private void executeTask(AppointmentRequest request, CountDownLatch startLatch, CountDownLatch endLatch, AtomicInteger successCount, AtomicInteger failCount) {
        try {
            startLatch.await();
            appointmentService.create(request);
            successCount.incrementAndGet();
        } catch (ResourceConflictException | DataIntegrityViolationException | CannotAcquireLockException e) {
            // CannotAcquireLockException: verificat empiric, doua INSERT-uri concurente pe aceeasi
            // constrangere EXCLUDE pot ajunge la un deadlock real la nivel de Postgres, nu doar la o
            // violare curata de constrangere — tot un "conflict, a doua cerere respinsa", vezi
            // GlobalExceptionHandler.handleDataIntegrityViolation.
            failCount.incrementAndGet();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            endLatch.countDown();
        }
    }
}