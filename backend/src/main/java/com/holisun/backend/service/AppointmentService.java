package com.holisun.backend.service;

import com.holisun.backend.dto.AppointmentRequest;
import com.holisun.backend.dto.AppointmentResponse;
import com.holisun.backend.entity.*;
import com.holisun.backend.mapper.AppointmentMapper;
import com.holisun.backend.repository.*;
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
    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;

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

        // TODO(P2): AvailabilityValidatorService.validate(dto.doctorId(), dto.roomId(), dto.startTime(), endTime, null);
        // TODO(P3): Equipment equipment = EquipmentAllocationService.allocate(service, dto.roomId(), dto.startTime(), endTime, null);

        Appointment newAppointment = new Appointment();
        newAppointment.setPatient(patient);
        newAppointment.setDoctor(doctor);
        newAppointment.setRoom(room);
        newAppointment.setService(service);
        // newAppointment.setEquipment(equipment);
        newAppointment.setStartTime(dto.startTime());
        newAppointment.setEndTime(endTime);
        newAppointment.setNotes(dto.notes());

        Appointment saved = appointmentRepository.save(newAppointment);
        return appointmentMapper.toResponse(saved);
    }
}
