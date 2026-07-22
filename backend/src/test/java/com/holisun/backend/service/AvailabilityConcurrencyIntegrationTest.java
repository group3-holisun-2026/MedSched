package com.holisun.backend.service;

import com.holisun.backend.dto.AppointmentRequest;
import com.holisun.backend.exception.ResourceConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

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
    private AppointmentService appointmentService;

    // --- TEST 1: Coliziune pe aceeași cameră ---
    @Test
    public void testConcurrentRoomReservation() throws InterruptedException {
        // Generăm UUID-uri valide automat
        runConcurrencyTest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    // --- TEST 2: Coliziune pe același medic ---
    @Test
    public void testConcurrentDoctorReservation() throws InterruptedException {
        runConcurrencyTest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }

    // --- LOGICA COMUNĂ ---
    private void runConcurrencyTest(UUID doctorId, UUID roomId, UUID patientId, UUID serviceId) throws InterruptedException {
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);


        LocalDateTime startTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);

        // Deoarece AppointmentRequest este un record, parametrii se pun direct în constructor.
        // ATENȚIE: Verifică ordinea parametrilor în fișierul AppointmentRequest.java și ajustează aici dacă diferă!
        AppointmentRequest request = new AppointmentRequest(patientId, doctorId, roomId, serviceId, startTime, "Teste concurenta");


        Runnable task = () -> {
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
        };

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(task);
        }

        startLatch.countDown();
        endLatch.await();
        executorService.shutdown();

        assertEquals(1, successCount.get(), "O singură programare ar fi trebuit să reușească.");
        assertEquals(1, failCount.get(), "A doua programare ar fi trebuit să fie respinsă cu conflict.");
    }
}