package com.holisun.backend.service;

import com.holisun.backend.dto.AppointmentRequest;
import com.holisun.backend.exception.ResourceConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class AvailabilityConcurrencyIntegrationTest {

    @Autowired
    private AppointmentService appointmentService; // Aici vom testa salvarea efectivă (metoda scrisă de P1)

    @Test
    public void testConcurrentRoomReservation() throws InterruptedException {
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

        // CountDownLatch este "pistolul de start" care se asigură că ambele thread-uri pornesc FIX în același timp
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // Date de test (asigură-te că ID-urile astea există în baza de date de test)
        UUID doctorId = UUID.fromString("pune-un-id-de-doctor-valid-aici");
        UUID roomId = UUID.fromString("pune-un-id-de-camera-valid-aici");
        UUID patientId = UUID.fromString("pune-un-id-de-pacient-valid-aici");
        UUID serviceId = UUID.fromString("pune-un-id-de-serviciu-valid-aici");
        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);


        AppointmentRequest request = new AppointmentRequest();
        // request.setDoctorId(doctorId);
        // request.setRoomId(roomId);
        // request.setPatientId(patientId);
        // request.setServiceId(serviceId);
        // request.setStartTime(startTime);

        Runnable task = () -> {
            try {
                startLatch.await(); // Așteaptă semnalul de start
                appointmentService.create(request); // Încearcă să salveze programarea
                successCount.incrementAndGet();
            } catch (ResourceConflictException | DataIntegrityViolationException e) {
                // Dacă prinde excepția ta (sau cea de DB de la P1), înseamnă că a respins corect a doua programare
                failCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                endLatch.countDown();
            }
        };

        // Pregătim thread-urile
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(task);
        }

        // START! Dăm drumul la ambele thread-uri simultan
        startLatch.countDown();

        // Așteptăm să termine amândouă
        endLatch.await();
        executorService.shutdown();

        // Verificăm rezultatul: trebuie să fie o singură salvare cu succes și un singur eșec
        assertEquals(1, successCount.get(), "O singură programare ar fi trebuit să reușească.");
        assertEquals(1, failCount.get(), "A doua programare ar fi trebuit să fie respinsă cu conflict.");
    }
}