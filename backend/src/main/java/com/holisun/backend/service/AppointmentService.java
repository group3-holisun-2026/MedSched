package com.holisun.backend.service;

import com.holisun.backend.dto.AppointmentRequest;
import com.holisun.backend.dto.AppointmentResponse;
import com.holisun.backend.entity.*;
import com.holisun.backend.enums.AppointmentStatus;
import com.holisun.backend.mapper.AppointmentMapper;
import com.holisun.backend.repository.*;
import org.springframework.http.HttpStatus;
import com.holisun.backend.entity.Service;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pacientul nu a fost gasit"));

        Doctor doctor = doctorRepository.findById(dto.doctorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctorul nu a fost gasita"));

        Room room = roomRepository.findById(dto.roomId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Camera nu a fost gasita"));

        Service service = serviceRepository.findById(dto.serviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Serviciul clinic nu a fost gasit"));

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
        newAppointment.setStatus(AppointmentStatus.SCHEDULED);

        Appointment saved = appointmentRepository.save(newAppointment);
        return appointmentMapper.toResponse(saved);
    }

    public AppointmentResponse update(UUID id, AppointmentRequest dto) {

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programarea nu a fost gasita"));

        switch (appointment.getStatus()){
            case AppointmentStatus.CANCELLED, AppointmentStatus.COMPLETED ->
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Programarea nu poate fi modificata");
        }

        Patient patient = patientRepository.findById(dto.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pacientul nu a fost gasit"));

        Doctor doctor = doctorRepository.findById(dto.doctorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctorul nu a fost gasita"));

        Room room = roomRepository.findById(dto.roomId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Camera nu a fost gasita"));

        Service service = serviceRepository.findById(dto.serviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Serviciul clinic nu a fost gasit"));

        LocalDateTime endTime = dto.startTime().plusMinutes(service.getDefaultDurationMinutes());

        // TODO(P2): AvailabilityValidatorService.validate(dto.doctorId(), dto.roomId(), dto.startTime(), endTime, id);
        // TODO(P3): Equipment equipment = EquipmentAllocationService.allocate(service, dto.roomId(), dto.startTime(), endTime, id);

        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setRoom(room);
        appointment.setService(service);
        // appointment.setEquipment(equipment);
        appointment.setStartTime(dto.startTime());
        appointment.setEndTime(endTime);
        appointment.setNotes(dto.notes());

        Appointment saved = appointmentRepository.save(appointment);
        return appointmentMapper.toResponse(saved);
    }

    public AppointmentResponse getById(UUID id) {
        Appointment appointment = this.appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programarea nu a fost gasita"));

        return appointmentMapper.toResponse(appointment);
    }

    public void cancel(UUID id){
        Appointment appointment = this.appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programarea nu a fost gasita"));

        switch (appointment.getStatus()){
            case AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED ->
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Programarea nu poate fi modificata");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        this.appointmentRepository.save(appointment);
    }
}
