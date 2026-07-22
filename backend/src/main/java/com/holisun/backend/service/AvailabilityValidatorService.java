package com.holisun.backend.service;

import com.holisun.backend.exception.ResourceConflictException;
import com.holisun.backend.repository.AppointmentRepository;
import com.holisun.backend.repository.DoctorRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AvailabilityValidatorService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final WorkScheduleValidator workScheduleValidator;

    public AvailabilityValidatorService(AppointmentRepository appointmentRepository,
                                        DoctorRepository doctorRepository,
                                        WorkScheduleValidator workScheduleValidator) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.workScheduleValidator = workScheduleValidator;
    }

    public void validate(UUID doctorId, UUID roomId, LocalDateTime start, LocalDateTime end, UUID excludeAppointmentId) {

        // 1. Verificare conflict programări pentru doctor
        var overlappingDoctorAppointments = appointmentRepository.findOverlappingForDoctor(doctorId, start, end, excludeAppointmentId);
        if (overlappingDoctorAppointments != null && !overlappingDoctorAppointments.isEmpty()) {
            throw new ResourceConflictException(
                    ResourceConflictException.ResourceType.DOCTOR,
                    "Medicul are deja o programare în acest interval."
            );
        }

        // 2. Verificare conflict programări pentru cameră (cabinet)
        var overlappingRoomAppointments = appointmentRepository.findOverlappingForRoom(roomId, start, end, excludeAppointmentId);
        if (overlappingRoomAppointments != null && !overlappingRoomAppointments.isEmpty()) {
            throw new ResourceConflictException(
                    ResourceConflictException.ResourceType.ROOM,
                    "Cabinetul selectat este deja ocupat în acest interval."
            );
        }

        // 3. Verificarea orarului de lucru al medicului
        var doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Medicul nu a fost găsit."));

        boolean isWithinSchedule = workScheduleValidator.isWithinSchedule(doctor.getWeeklySchedule(), start, end);
        if (!isWithinSchedule) {
            throw new ResourceConflictException(
                    ResourceConflictException.ResourceType.DOCTOR,
                    "Medicul nu are program de lucru în acest interval."
            );
        }
    }
}