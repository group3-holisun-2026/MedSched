package com.holisun.backend.service;

import com.holisun.backend.dto.AppointmentRequest;
import com.holisun.backend.entity.Doctor;
import com.holisun.backend.entity.Patient;
import com.holisun.backend.entity.Room;
import com.holisun.backend.entity.User;
import com.holisun.backend.repository.DoctorRepository;
import com.holisun.backend.repository.PatientRepository;
import com.holisun.backend.repository.RoomRepository;
import com.holisun.backend.repository.ServiceRepository;
import com.holisun.backend.repository.UserRepository;
import com.holisun.backend.exception.ResourceConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class AvailabilityConcurrencyIntegrationTest {

    @Autowired private AppointmentService appointmentService;

    // Injectăm toate repository-urile necesare pentru a crea Fixtures
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private ServiceRepository serviceRepository;
    @Autowired private UserRepository userRepository; // Necesar pentru că Doctor depinde de User

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
        // Medicul cere obligatoriu un user.
        User user = new User();
        String randomSuffix = UUID.randomUUID().toString().substring(0, 5);
        user.setUsername("doc_" + randomSuffix);
        user.setEmail("doc_" + randomSuffix + "@test.com");
        user.setPasswordHash("Parola123!");
        // Setează și alte câmpuri la user dacă mai are unele cu nullable=false
        user = userRepository.save(user);

        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setSpeciality("General");
        doctor.setStandardConsultationDurationMinutes(30);
        doctor.setActive(true);
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
        } catch (ResourceConflictException | DataIntegrityViolationException e) {
            failCount.incrementAndGet();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            endLatch.countDown();
        }
    }
}