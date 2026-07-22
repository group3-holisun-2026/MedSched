package com.holisun.backend.config;

import com.holisun.backend.entity.*;
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
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

/**
 * Seeds demo accounts and sample clinic data for local development.
 * Only runs on the "dev" profile and is a no-op if the admin account already exists,
 * so it is safe to leave enabled across repeated app restarts.
 */
@Slf4j
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

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
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.findByEmail("admin@medsched.ro").isPresent()) {
            log.info("Dev seed data already present, skipping DevDataSeeder.");
            return;
        }

        log.info("Seeding demo credentials and sample clinic data...");

        User admin = createUser("seed-admin", "admin@medsched.ro", "Admin123!", Role.ADMIN);
        User doctorUser1 = createUser("seed-dr-popescu", "dr.popescu@medsched.ro", "Doctor123!", Role.DOCTOR);
        User doctorUser2 = createUser("seed-dr-ionescu", "dr.ionescu@medsched.ro", "Doctor123!", Role.DOCTOR);
        createUser("seed-receptie", "receptie@medsched.ro", "Receptie123!", Role.RECEPTION);

        Doctor doctor1 = createDoctor(doctorUser1, "Cardiologie", 30);
        Doctor doctor2 = createDoctor(doctorUser2, "Pediatrie", 20);

        createSchedule(doctor1, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(17, 0));
        createSchedule(doctor1, DayOfWeek.WEDNESDAY, LocalTime.of(9, 0), LocalTime.of(17, 0));
        createSchedule(doctor1, DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));

        createSchedule(doctor2, DayOfWeek.TUESDAY, LocalTime.of(8, 0), LocalTime.of(14, 0));
        createSchedule(doctor2, DayOfWeek.THURSDAY, LocalTime.of(14, 0), LocalTime.of(20, 0));

        Room room1 = createRoom("Cabinet 1", "Cabinet consultatii generale");
        Room room2 = createRoom("Cabinet 2", "Cabinet ecografie");
        createRoom("Cabinet 3", "Cabinet pediatrie");

        EquipmentType ecografType = createEquipmentType("Ecograf Doppler 4D", "Ecograf pentru investigatii abdominale si obstetricale");
        EquipmentType ecgType = createEquipmentType("Aparat ECG", "Electrocardiograf portabil");

        createEquipment("Ecograf Doppler 4D - Cabinet 2", ecografType, room2);
        createEquipment("Aparat ECG - Cabinet 1", ecgType, room1);

        createService("Consult Pediatric", new BigDecimal("150.00"), 20, Set.of());
        createService("Ecografie Abdominala", new BigDecimal("250.00"), 30, Set.of(ecografType));
        createService("Control Cardiologic", new BigDecimal("200.00"), 30, Set.of(ecgType));

        createPatient("Ana", "Maria Georgescu", "0721111111", "ana.georgescu@example.com",
                LocalDate.of(1990, 4, 12), "Polen", "Fara antecedente semnificative",
                "1900412123456");
        createPatient("Ion", "Vasilescu", "0722222222", "ion.vasilescu@example.com",
                LocalDate.of(1985, 11, 2), null, "Hipertensiune arteriala",
                "1851102123457");
        createPatient("Elena", "Dumitrescu", "0723333333", "elena.dumitrescu@example.com",
                LocalDate.of(2015, 7, 20), "Lactoza", null, null);
        createPatient("Mihai", "Constantin", "0724444444", null,
                null, null, null, null);

        ConsultationRecord record = new ConsultationRecord();
        record.setAppointmentId(UUID.randomUUID());
        record.setPresentationMotive("Control de rutina");
        record.setAnamnesis("Pacient fara acuze subiective la momentul prezentarii.");
        record.setClinicalExam("Stare generala buna, TA 120/80 mmHg, AV 72 bpm.");
        record.setDiagnosis("Fara modificari patologice");
        record.setPrescription("Reevaluare peste 6 luni");
        record.setLocked(true);
        consultationRecordRepository.save(record);

        log.info("Dev seed data created. Login with admin@medsched.ro / Admin123!, " +
                "dr.popescu@medsched.ro / Doctor123!, dr.ionescu@medsched.ro / Doctor123!, " +
                "receptie@medsched.ro / Receptie123! (all passwords above).");
    }

    private User createUser(String username, String email, String rawPassword, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    private Doctor createDoctor(User user, String speciality, int durationMinutes) {
        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setSpeciality(speciality);
        doctor.setStandardConsultationDurationMinutes(durationMinutes);
        doctor.setActive(true);
        return doctorRepository.save(doctor);
    }

    private void createSchedule(Doctor doctor, DayOfWeek day, LocalTime start, LocalTime end) {
        WorkSchedule schedule = new WorkSchedule();
        schedule.setDoctor(doctor);
        schedule.setDayOfWeek(day);
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        workScheduleRepository.save(schedule);
    }

    private Room createRoom(String name, String description) {
        Room room = new Room();
        room.setName(name);
        room.setDescription(description);
        room.setActive(true);
        return roomRepository.save(room);
    }

    private EquipmentType createEquipmentType(String name, String description) {
        EquipmentType type = new EquipmentType();
        type.setName(name);
        type.setDescription(description);
        return equipmentTypeRepository.save(type);
    }

    private void createEquipment(String name, EquipmentType type, Room room) {
        Equipment equipment = new Equipment();
        equipment.setName(name);
        equipment.setEquipmentType(type);
        equipment.setRoom(room);
        equipment.setActive(true);
        equipmentRepository.save(equipment);
    }

    private void createService(String name, BigDecimal price, int durationMinutes, Set<EquipmentType> requiredTypes) {
        Service service = new Service();
        service.setName(name);
        service.setPrice(price);
        service.setDefaultDurationMinutes(durationMinutes);
        service.setRequiredEquipmentTypes(requiredTypes);
        service.setActive(true);
        serviceRepository.save(service);
    }

    private void createPatient(String firstName, String lastName, String phone, String email,
                               LocalDate dateOfBirth, String allergies, String medicalHistory, String cnp) {
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
        patientRepository.save(patient);
    }
}
