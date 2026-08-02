package com.holisun.backend.config;

import com.holisun.backend.entity.*;
import com.holisun.backend.enums.AppointmentStatus;
import com.holisun.backend.enums.Role;
import com.holisun.backend.repository.*;
import com.holisun.backend.util.CnpHasher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Seeds demo accounts and sample clinic data for local development ("dev" profile).
 *
 * Fiecare `ensureX` e idempotent (cauta intai dupa cheia naturala — email / nume), deci
 * seeder-ul poate rula peste o baza care are deja datele vechi si adauga doar ce lipseste.
 * Inainte era un no-op total daca exista contul de admin, ceea ce insemna ca orice date noi
 * adaugate aici nu ajungeau niciodata intr-o baza de dev existenta fara un drop manual.
 */
@Slf4j
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private static final String DOCTOR_PASSWORD = "Doctor123!";
    private static final String SEED_MARKER = "DevDataSeeder";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DoctorRepository doctorRepository;
    @Autowired
    private WorkScheduleRepository workScheduleRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private EquipmentTypeRepository equipmentTypeRepository;
    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private ClinicalServiceRepository serviceRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private ConsultationRecordRepository consultationRecordRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Seeding demo credentials and sample clinic data...");

        ensureUser("Administrator Clinica", "admin@medsched.ro", "Admin123!", Role.ADMIN);
        ensureUser("Receptie Clinica", "receptie@medsched.ro", "Receptie123!", Role.RECEPTION);

        List<Doctor> doctors = seedDoctors();
        List<Room> rooms = seedRooms();
        List<Service> services = seedServices();
        List<Patient> patients = seedPatients();

        seedSampleConsultationRecord();
        seedAppointments(doctors, rooms, services, patients);

        log.info("Dev seed data ready: {} medici, {} cabinete, {} servicii, {} pacienti. " +
                        "Login: admin@medsched.ro / Admin123!, receptie@medsched.ro / Receptie123!, " +
                        "orice dr.*@medsched.ro / {}",
                doctors.size(), rooms.size(), services.size(), patients.size(), DOCTOR_PASSWORD);
    }

    // ---------------------------------------------------------------- doctors

    /**
     * `DoctorResponse.fullName` e mapat din `user.username`, deci username-ul E numele afisat
     * in tot UI-ul (dropdown-ul de medici, calendar). Vechile valori de tip "seed-dr-popescu"
     * faceau lista de medici ilizibila la receptie — de aceea sunt nume reale aici, iar
     * `ensureUser` rescrie username-urile vechi "seed-*" pe bazele existente.
     */
    private List<Doctor> seedDoctors() {
        List<Doctor> doctors = new ArrayList<>();

        doctors.add(ensureDoctor("Dr. Andrei Popescu", "dr.popescu@medsched.ro", "Cardiologie", 30, List.of(
                schedule(DayOfWeek.MONDAY, 9, 17),
                schedule(DayOfWeek.WEDNESDAY, 9, 17),
                schedule(DayOfWeek.FRIDAY, 9, 13))));

        doctors.add(ensureDoctor("Dr. Maria Ionescu", "dr.ionescu@medsched.ro", "Pediatrie", 20, List.of(
                schedule(DayOfWeek.TUESDAY, 8, 14),
                schedule(DayOfWeek.THURSDAY, 14, 20))));

        doctors.add(ensureDoctor("Dr. Radu Stanciu", "dr.stanciu@medsched.ro", "Medicina interna", 30, List.of(
                schedule(DayOfWeek.MONDAY, 8, 16),
                schedule(DayOfWeek.TUESDAY, 8, 16),
                schedule(DayOfWeek.WEDNESDAY, 8, 16),
                schedule(DayOfWeek.THURSDAY, 8, 16),
                schedule(DayOfWeek.FRIDAY, 8, 16))));

        doctors.add(ensureDoctor("Dr. Cristina Marin", "dr.marin@medsched.ro", "Dermatologie", 20, List.of(
                schedule(DayOfWeek.MONDAY, 12, 20),
                schedule(DayOfWeek.WEDNESDAY, 12, 20),
                schedule(DayOfWeek.FRIDAY, 12, 18))));

        doctors.add(ensureDoctor("Dr. Alexandru Dobre", "dr.dobre@medsched.ro", "Ortopedie", 40, List.of(
                schedule(DayOfWeek.TUESDAY, 9, 17),
                schedule(DayOfWeek.THURSDAY, 9, 17))));

        doctors.add(ensureDoctor("Dr. Ioana Neagu", "dr.neagu@medsched.ro", "Neurologie", 30, List.of(
                schedule(DayOfWeek.MONDAY, 10, 18),
                schedule(DayOfWeek.THURSDAY, 10, 18))));

        doctors.add(ensureDoctor("Dr. Vlad Tudor", "dr.tudor@medsched.ro", "ORL", 20, List.of(
                schedule(DayOfWeek.WEDNESDAY, 8, 14),
                schedule(DayOfWeek.FRIDAY, 8, 14))));

        // Persona din caietul de sarcini — medicul care foloseste tableta in cabinet.
        doctors.add(ensureDoctor("Dr. Simona Vasile", "dr.vasile@medsched.ro", "Ginecologie", 30, List.of(
                schedule(DayOfWeek.MONDAY, 9, 15),
                schedule(DayOfWeek.TUESDAY, 9, 15),
                schedule(DayOfWeek.WEDNESDAY, 9, 15),
                schedule(DayOfWeek.THURSDAY, 9, 15))));

        return doctors;
    }

    private Doctor ensureDoctor(String displayName, String email, String speciality,
                                int durationMinutes, List<WorkSchedule> schedules) {
        User user = ensureUser(displayName, email, DOCTOR_PASSWORD, Role.DOCTOR);

        Doctor doctor = doctorRepository.findByUserId(user.getId()).orElseGet(() -> {
            Doctor created = new Doctor();
            created.setUser(user);
            created.setSpeciality(speciality);
            created.setStandardConsultationDurationMinutes(durationMinutes);
            created.setActive(true);
            return doctorRepository.save(created);
        });

        List<WorkSchedule> existing = workScheduleRepository.findAll().stream()
                .filter(s -> s.getDoctor() != null && s.getDoctor().getId().equals(doctor.getId()))
                .toList();

        for (WorkSchedule wanted : schedules) {
            boolean alreadyThere = existing.stream()
                    .anyMatch(s -> s.getDayOfWeek() == wanted.getDayOfWeek());
            if (!alreadyThere) {
                wanted.setDoctor(doctor);
                workScheduleRepository.save(wanted);
            }
        }

        return doctor;
    }

    private WorkSchedule schedule(DayOfWeek day, int startHour, int endHour) {
        WorkSchedule schedule = new WorkSchedule();
        schedule.setDayOfWeek(day);
        schedule.setStartTime(LocalTime.of(startHour, 0));
        schedule.setEndTime(LocalTime.of(endHour, 0));
        return schedule;
    }

    private User ensureUser(String displayName, String email, String rawPassword, Role role) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User user = existing.get();
            // Migreaza username-urile vechi generate de seeder ("seed-dr-popescu") la numele real,
            // altfel bazele de dev existente raman cu lista de medici ilizibila.
            if (user.getUsername() != null && user.getUsername().startsWith("seed-")) {
                user.setUsername(displayName);
                return userRepository.save(user);
            }
            return user;
        }

        User user = new User();
        user.setUsername(displayName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    // ------------------------------------------------------------ rooms/equip

    private List<Room> seedRooms() {
        return List.of(
                ensureRoom("Cabinet 1", "Consultatii generale"),
                ensureRoom("Cabinet 2", "Ecografie"),
                ensureRoom("Cabinet 3", "Pediatrie"),
                ensureRoom("Cabinet 4", "Cardiologie"),
                ensureRoom("Cabinet 5", "Dermatologie"),
                ensureRoom("Cabinet 6", "Ortopedie si mica chirurgie"),
                ensureRoom("Cabinet 7", "Neurologie"),
                ensureRoom("Cabinet 8", "ORL"),
                ensureRoom("Cabinet 9", "Ginecologie"),
                ensureRoom("Sala de tratamente", "Tratamente si pansamente")
        );
    }

    private Room ensureRoom(String name, String description) {
        return roomRepository.findAll().stream()
                .filter(r -> name.equals(r.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Room room = new Room();
                    room.setName(name);
                    room.setDescription(description);
                    room.setActive(true);
                    return roomRepository.save(room);
                });
    }

    private EquipmentType ensureEquipmentType(String name, String description) {
        return equipmentTypeRepository.findAll().stream()
                .filter(t -> name.equals(t.getName()))
                .findFirst()
                .orElseGet(() -> {
                    EquipmentType type = new EquipmentType();
                    type.setName(name);
                    type.setDescription(description);
                    return equipmentTypeRepository.save(type);
                });
    }

    private void ensureEquipment(String name, EquipmentType type, Room room) {
        boolean exists = equipmentRepository.findAll().stream().anyMatch(e -> name.equals(e.getName()));
        if (exists) return;

        Equipment equipment = new Equipment();
        equipment.setName(name);
        equipment.setEquipmentType(type);
        equipment.setRoom(room);
        equipment.setActive(true);
        equipmentRepository.save(equipment);
    }

    // ----------------------------------------------------------------- services

    private List<Service> seedServices() {
        Room room1 = ensureRoom("Cabinet 1", "Consultatii generale");
        Room room2 = ensureRoom("Cabinet 2", "Ecografie");
        Room room5 = ensureRoom("Cabinet 5", "Dermatologie");
        Room room8 = ensureRoom("Cabinet 8", "ORL");

        EquipmentType ecograf = ensureEquipmentType("Ecograf Doppler 4D",
                "Ecograf pentru investigatii abdominale si obstetricale");
        EquipmentType ecg = ensureEquipmentType("Aparat ECG", "Electrocardiograf portabil");
        EquipmentType dermatoscop = ensureEquipmentType("Dermatoscop digital",
                "Dermatoscop cu captura de imagine");
        EquipmentType audiometru = ensureEquipmentType("Audiometru", "Audiometru clinic");

        ensureEquipment("Ecograf Doppler 4D - Cabinet 2", ecograf, room2);
        ensureEquipment("Aparat ECG - Cabinet 1", ecg, room1);
        ensureEquipment("Dermatoscop digital - Cabinet 5", dermatoscop, room5);
        ensureEquipment("Audiometru - Cabinet 8", audiometru, room8);

        // Serviciile fara echipament necesar sunt cele folosite la seed-ul de programari,
        // ca sa nu depindem de alocarea automata de echipament (restanta de Modul 3).
        return List.of(
                ensureService("Consult Medicina Interna", "170.00", 30, Set.of()),
                ensureService("Consult Pediatric", "150.00", 20, Set.of()),
                ensureService("Consult Dermatologic", "180.00", 20, Set.of()),
                ensureService("Consult Ortopedic", "220.00", 40, Set.of()),
                ensureService("Consult Neurologic", "240.00", 30, Set.of()),
                ensureService("Consult ORL", "160.00", 20, Set.of()),
                ensureService("Consult Ginecologic", "200.00", 30, Set.of()),
                ensureService("Control Cardiologic", "200.00", 30, Set.of(ecg)),
                ensureService("Ecografie Abdominala", "250.00", 30, Set.of(ecograf))
        );
    }

    private Service ensureService(String name, String price, int durationMinutes, Set<EquipmentType> requiredTypes) {
        return serviceRepository.findAll().stream()
                .filter(s -> name.equals(s.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Service service = new Service();
                    service.setName(name);
                    service.setPrice(new BigDecimal(price));
                    service.setDefaultDurationMinutes(durationMinutes);
                    service.setRequiredEquipmentTypes(requiredTypes);
                    service.setActive(true);
                    return serviceRepository.save(service);
                });
    }

    // ----------------------------------------------------------------- patients

    private List<Patient> seedPatients() {
        return List.of(
                ensurePatient("Ana", "Maria Georgescu", "0721111111", "ana.georgescu@example.com",
                        LocalDate.of(1990, 4, 12), "Polen", "Fara antecedente semnificative", "1900412123456"),
                ensurePatient("Ion", "Vasilescu", "0722222222", "ion.vasilescu@example.com",
                        LocalDate.of(1985, 11, 2), null, "Hipertensiune arteriala", "1851102123457"),
                ensurePatient("Elena", "Dumitrescu", "0723333333", "elena.dumitrescu@example.com",
                        LocalDate.of(2015, 7, 20), "Lactoza", null, null),
                ensurePatient("Mihai", "Constantin", "0724444444", null, null, null, null, null),
                ensurePatient("Andreea", "Pop", "0725555555", "andreea.pop@example.com",
                        LocalDate.of(1998, 2, 8), null, null, "2980208123458"),
                ensurePatient("George", "Barbu", "0726666666", "george.barbu@example.com",
                        LocalDate.of(1972, 9, 30), "Penicilina", "Diabet zaharat tip 2", "1720930123459"),
                ensurePatient("Raluca", "Ene", "0727777777", "raluca.ene@example.com",
                        LocalDate.of(2011, 1, 17), null, "Astm bronsic", null),
                ensurePatient("Stefan", "Munteanu", "0728888888", "stefan.munteanu@example.com",
                        LocalDate.of(1965, 6, 3), null, "Interventie de menisc 2019", "1650603123460"),
                ensurePatient("Diana", "Lungu", "0729999999", "diana.lungu@example.com",
                        LocalDate.of(1993, 12, 25), "Fructe de mare", null, "2931225123461"),
                ensurePatient("Cosmin", "Radulescu", "0730000000", null,
                        LocalDate.of(2004, 3, 14), null, null, null)
        );
    }

    private Patient ensurePatient(String firstName, String lastName, String phone, String email,
                                  LocalDate dateOfBirth, String allergies, String medicalHistory, String cnp) {
        return patientRepository.findAll().stream()
                .filter(p -> firstName.equals(p.getFirstName()) && lastName.equals(p.getLastName()))
                .findFirst()
                .orElseGet(() -> {
                    Patient patient = new Patient();
                    patient.setFirstName(firstName);
                    patient.setLastName(lastName);
                    patient.setPhone(phone);
                    patient.setEmail(email);
                    patient.setDateOfBirth(dateOfBirth);
                    patient.setAllergies(allergies);
                    patient.setMedicalHistory(medicalHistory);
                    if (cnp != null) {
                        patient.setCnp(cnp);
                        patient.setCnpHash(CnpHasher.hash(cnp));
                    }
                    return patientRepository.save(patient);
                });
    }

    private void seedSampleConsultationRecord() {
        if (consultationRecordRepository.count() > 0) return;

        ConsultationRecord record = new ConsultationRecord();
        record.setAppointmentId(UUID.randomUUID());
        record.setPresentationMotive("Control de rutina");
        record.setAnamnesis("Pacient fara acuze subiective la momentul prezentarii.");
        record.setClinicalExam("Stare generala buna, TA 120/80 mmHg, AV 72 bpm.");
        record.setDiagnosis("Fara modificari patologice");
        record.setPrescription("Reevaluare peste 6 luni");
        record.setLocked(true);
        consultationRecordRepository.save(record);
    }

    // ------------------------------------------------------------- appointments

    /**
     * Umple saptamana curenta cu programari, ca ecranul de calendar sa nu fie gol si ca
     * tranzitiile F-401 sa aiba pe ce fi testate (fiecare status apare cel putin o data).
     *
     * Fiecare medic primeste un cabinet dedicat (index-based), iar sloturile unui medic sunt
     * secventiale — asa nu se calca niciun EXCLUDE de la V7 (doctor/room overlap), fara sa fie
     * nevoie sa trecem prin AvailabilityValidatorService.
     */
    private void seedAppointments(List<Doctor> doctors, List<Room> rooms,
                                  List<Service> services, List<Patient> patients) {
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDateTime weekStart = monday.atStartOfDay();
        LocalDateTime weekEnd = monday.plusDays(7).atStartOfDay();

        List<Appointment> thisWeek = appointmentRepository.findAll().stream()
                .filter(a -> a.getStartTime().isBefore(weekEnd) && a.getEndTime().isAfter(weekStart))
                .toList();

        boolean alreadySeeded = thisWeek.stream()
                .anyMatch(a -> a.getNotes() != null && a.getNotes().contains(SEED_MARKER));
        if (alreadySeeded) {
            log.info("Saptamana curenta are deja programari generate de seeder, sar peste.");
            return;
        }

        // Programarile facute manual raman intacte: sarim orice slot care s-ar suprapune peste
        // ele pe acelasi medic sau acelasi cabinet, ca sa nu lovim constrangerile EXCLUDE din V7
        // (o violare ar strica tranzactia si ar impiedica pornirea aplicatiei).
        List<Appointment> occupied = new ArrayList<>(thisWeek);
        LocalDateTime now = LocalDateTime.now();
        int patientCursor = 0;
        int created = 0;
        int skipped = 0;

        for (int d = 0; d < doctors.size(); d++) {
            Doctor doctor = doctors.get(d);
            Room room = rooms.get(d % rooms.size());
            Service service = services.get(d % services.size());
            int duration = service.getDefaultDurationMinutes();

            List<WorkSchedule> schedules = workScheduleRepository.findAll().stream()
                    .filter(s -> s.getDoctor() != null && s.getDoctor().getId().equals(doctor.getId()))
                    .toList();

            for (WorkSchedule workDay : schedules) {
                LocalDate date = monday.with(workDay.getDayOfWeek());

                // 3 sloturi pe zi lucratoare, la 2 ore distanta, toate in interiorul programului.
                for (int slot = 0; slot < 3; slot++) {
                    LocalDateTime start = date.atTime(workDay.getStartTime()).plusHours(2L * slot);
                    LocalDateTime end = start.plusMinutes(duration);

                    if (end.toLocalTime().isAfter(workDay.getEndTime())) break;

                    if (collidesWithExisting(occupied, doctor, room, start, end)) {
                        skipped++;
                        continue;
                    }

                    Patient patient = patients.get(patientCursor++ % patients.size());
                    occupied.add(appointmentRepository.save(
                            buildAppointment(patient, doctor, room, service, start, end, now)));
                    created++;
                }
            }
        }

        log.info("Am creat {} programari de test in saptamana curenta ({} - {}), {} sloturi sarite " +
                "din cauza programarilor existente.", created, monday, monday.plusDays(4), skipped);
    }

    private boolean collidesWithExisting(List<Appointment> existing, Doctor doctor, Room room,
                                         LocalDateTime start, LocalDateTime end) {
        return existing.stream().anyMatch(a -> {
            boolean sameResource =
                    (a.getDoctor() != null && a.getDoctor().getId().equals(doctor.getId()))
                            || (a.getRoom() != null && a.getRoom().getId().equals(room.getId()));
            boolean overlaps = a.getStartTime().isBefore(end) && a.getEndTime().isAfter(start);
            return sameResource && overlaps;
        });
    }

    private Appointment buildAppointment(Patient patient, Doctor doctor, Room room, Service service,
                                         LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setRoom(room);
        appointment.setService(service);
        appointment.setStartTime(start);
        appointment.setEndTime(end);
        appointment.setNotes("Programare generata automat pentru testare (" + SEED_MARKER + ").");
        appointment.setStatus(statusFor(start, end, now));
        appointment.setPriceAtBooking(service.getPrice());

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            // Unele finalizate ies din fereastra de gratie de 30 min, altele nu — ca sa se poata
            // testa si bannerul de gratie, si fisa blocata de scheduler.
            appointment.setCompletedAt(end.isBefore(now) ? end : now.minusMinutes(5));
        }

        return appointment;
    }

    /**
     * Statusul e derivat din pozitia slotului fata de "acum", ca datele sa arate plauzibil:
     * trecutul e inchis (finalizat / neprezentat / anulat), prezentul e in desfasurare,
     * viitorul e programat sau confirmat.
     */
    private AppointmentStatus statusFor(LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        if (end.isBefore(now)) {
            long bucket = Math.floorMod(start.getHour() + start.getDayOfYear(), 5);
            return switch ((int) bucket) {
                case 0 -> AppointmentStatus.NO_SHOW;
                case 1 -> AppointmentStatus.CANCELLED;
                default -> AppointmentStatus.COMPLETED;
            };
        }
        if (!start.isAfter(now)) {
            return AppointmentStatus.IN_PROGRESS;
        }
        return Math.floorMod(start.getDayOfYear() + start.getHour(), 2) == 0
                ? AppointmentStatus.CONFIRMED
                : AppointmentStatus.SCHEDULED;
    }
}
