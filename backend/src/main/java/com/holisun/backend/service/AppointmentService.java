package com.holisun.backend.service;

import com.holisun.backend.dto.AppointmentRequest;
import com.holisun.backend.dto.AppointmentResponse;
import com.holisun.backend.entity.*;
import com.holisun.backend.repository.*;
import org.flywaydb.core.internal.util.DateUtils;
import org.springframework.http.HttpStatus;
import com.holisun.backend.entity.Service;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AppointmentService {
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final RoomRepository roomRepository;
    private final ServiceRepository serviceRepository;
    private final EquipmentRepository equipmentRepository;

    public AppointmentResponse create(AppointmentRequest dto) {

        Patient patient = patientRepository.findById(dto.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));

        Doctor doctor = doctorRepository.findById(dto.doctorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));

        Room room = roomRepository.findById(dto.roomId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        Service service = serviceRepository.findById(dto.serviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Service not found"));

        LocalDateTime endTime = dto.startTime().plusMinutes(service.getDefaultDurationMinutes());

        // Equipment equipment = EquipmentAllocationService.allocate(service, dto.roomId(), dto.startTime(), endTime, true);



         // AvailabilityValidatorService.validate(dto.doctorId(), dto.roomId(), dto.startTime(), endTime, null);

        if (service.getRequiredEquipmentTypes() != null) {
            // EquipmentAllocationService.
        }

        Appointment newAppointment = new Appointment();
        newAppointment.setPatient(patient);
        newAppointment.setDoctor(doctor);
        newAppointment.setRoom(room);
        newAppointment.setService(service);
        // newAppointment.setEquipment(equipment);
        newAppointment.setStartTime(dto.startTime());
        newAppointment.setEndTime(newAppointment.getStartTime().plusMinutes(service.getDefaultDurationMinutes()));

        /*
        appointmentRepository.save(newAppointment);
        return appointmentMapper.toResponse(newAppointment);*/
        return null;
    }
}