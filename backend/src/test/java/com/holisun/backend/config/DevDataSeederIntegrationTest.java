package com.holisun.backend.config;

import com.holisun.backend.repository.AppointmentRepository;
import com.holisun.backend.repository.DoctorRepository;
import com.holisun.backend.repository.PatientRepository;
import com.holisun.backend.repository.RoomRepository;
import com.holisun.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Seeder-ul ruleaza la fiecare pornire pe profilul dev, deci trebuie sa fie idempotent:
 * a doua rulare peste o baza deja populata nu are voie sa dubleze nimic si nici sa crape.
 * Varianta anterioara "sarea" complet daca exista contul de admin, deci datele noi nu ajungeau
 * niciodata intr-o baza de dev existenta — testul de mai jos blocheaza ambele regresii.
 *
 * Seeder-ul e `@Profile("dev")`, deci profilul `dev` trebuie activat ca sa existe bean-ul. Efectul
 * secundar, descoperit cand a aparut prima coloana noua dupa scrierea testului: `application-dev.yml`
 * e ultimul in lista de profiluri, deci suprascria si `spring.datasource.url` si `ddl-auto`. Testul
 * rula, tacut, pe baza de dezvoltare a fiecaruia (`medsched_dev`) — nu doar o citea, ci ii si stergea
 * si recrea programarile — si o valida cu `validate`, desi acolo schema e detinuta de Flyway, care in
 * profilul de test e oprit. Prima coloana noua pe o entitate pica astfel aici, cu "missing column"
 * sau "contains null values", desi cauza n-are nimic de-a face cu seeder-ul.
 *
 * Fixam explicit conexiunea pe baza de test si `ddl-auto` pe `update`: completeaza schema fara sa
 * stearga nimic, spre deosebire de `create-drop`, care ar goli baza sub celelalte contexte de test.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/medsched_test",
        "spring.datasource.username=postgres",
        "spring.datasource.password=postgres",
        "spring.jpa.hibernate.ddl-auto=update"
})
@ActiveProfiles({"test", "dev"})
class DevDataSeederIntegrationTest {

    @Autowired private DevDataSeeder seeder;
    @Autowired private UserRepository userRepository;
    @Autowired private DoctorRepository doctorRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private AppointmentRepository appointmentRepository;

    @Test
    void seedsAClinicWorthOfDataAndStaysIdempotent() {
        // Seeder-ul a rulat deja o data la pornirea contextului (CommandLineRunner).
        assertTrue(doctorRepository.count() >= 8,
                "asteptam cel putin 8 medici de test, am gasit " + doctorRepository.count());
        assertTrue(roomRepository.count() >= 10,
                "asteptam cel putin 10 cabinete, am gasit " + roomRepository.count());

        // Golim programarile generate anterior ca sa testam efectiv calea de creare, nu doar
        // ramura de "sar peste" (baza de test e reutilizata intre rulari).
        appointmentRepository.deleteAll(appointmentRepository.findAll().stream()
                .filter(a -> a.getNotes() != null && a.getNotes().contains("DevDataSeeder"))
                .toList());

        seeder.run();
        long afterFirstRun = appointmentRepository.count();
        assertTrue(afterFirstRun > 0, "seeder-ul nu a creat nicio programare in saptamana curenta");

        long doctors = doctorRepository.count();
        long rooms = roomRepository.count();
        long patients = patientRepository.count();

        seeder.run();

        assertEquals(doctors, doctorRepository.count(), "a doua rulare a dublat medicii");
        assertEquals(rooms, roomRepository.count(), "a doua rulare a dublat cabinetele");
        assertEquals(patients, patientRepository.count(), "a doua rulare a dublat pacientii");
        assertEquals(afterFirstRun, appointmentRepository.count(), "a doua rulare a dublat programarile");
    }

    @Test
    void doctorsHaveHumanReadableDisplayNames() {
        // `DoctorResponse.fullName` vine din `user.username`, deci un username tehnic ajunge
        // direct in dropdown-ul de medici de la receptie.
        boolean anyTechnicalName = userRepository.findAll().stream()
                .map(user -> user.getUsername())
                .anyMatch(username -> username != null && username.startsWith("seed-"));

        assertFalse(anyTechnicalName, "au ramas username-uri tehnice 'seed-*' vizibile in UI");
    }
}
