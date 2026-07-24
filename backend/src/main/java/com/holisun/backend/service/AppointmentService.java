package com.holisun.backend.service;

import com.holisun.backend.dto.AppointmentRequest;
import com.holisun.backend.dto.AppointmentResponse;
import com.holisun.backend.entity.*;
import com.holisun.backend.enums.AppointmentStatus;
import com.holisun.backend.mapper.AppointmentMapper;
import com.holisun.backend.repository.*;
import com.holisun.backend.util.AppointmentStateMachine;
import org.springframework.boot.actuate.startup.StartupEndpoint;
import org.springframework.http.HttpStatus;
import com.holisun.backend.entity.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
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
    private final AvailabilityValidatorService availabilityValidatorService;
    private final EquipmentAllocationService equipmentAllocationService;
    private final AppointmentStateMachine appointmentStateMachine;
    private final StartupEndpoint startupEndpoint;

    @Transactional
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

        availabilityValidatorService.validate(dto.doctorId(), dto.roomId(), dto.startTime(), endTime, null);
        Equipment equipment = equipmentAllocationService.allocate(service, dto.roomId(), dto.startTime(), endTime, null);

        Appointment newAppointment = new Appointment();
        newAppointment.setPatient(patient);
        newAppointment.setDoctor(doctor);
        newAppointment.setRoom(room);
        newAppointment.setService(service);
        newAppointment.setEquipment(equipment);
        newAppointment.setStartTime(dto.startTime());
        newAppointment.setEndTime(endTime);
        newAppointment.setNotes(dto.notes());
        newAppointment.setStatus(AppointmentStatus.SCHEDULED);

        Appointment saved = appointmentRepository.save(newAppointment);
        return appointmentMapper.toResponse(saved);
    }

    @Transactional
    public AppointmentResponse update(UUID id, AppointmentRequest dto) {

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programarea nu a fost gasita"));

        appointmentStateMachine.assertTransition(appointment.getStatus(), AppointmentStatus.SCHEDULED);

        Patient patient = patientRepository.findById(dto.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pacientul nu a fost gasit"));

        Doctor doctor = doctorRepository.findById(dto.doctorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctorul nu a fost gasita"));

        Room room = roomRepository.findById(dto.roomId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Camera nu a fost gasita"));

        Service service = serviceRepository.findById(dto.serviceId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Serviciul clinic nu a fost gasit"));

        LocalDateTime endTime = dto.startTime().plusMinutes(service.getDefaultDurationMinutes());

        availabilityValidatorService.validate(dto.doctorId(), dto.roomId(), dto.startTime(), endTime, id);
        Equipment equipment = equipmentAllocationService.allocate(service, dto.roomId(), dto.startTime(), endTime, id);

        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setRoom(room);
        appointment.setService(service);
        appointment.setEquipment(equipment);
        appointment.setStartTime(dto.startTime());
        appointment.setEndTime(endTime);
        appointment.setNotes(dto.notes());

        Appointment saved = appointmentRepository.save(appointment);
        return appointmentMapper.toResponse(saved);
    }

    @Transactional
    public AppointmentResponse getById(UUID id) {
        Appointment appointment = this.appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programarea nu a fost gasita"));

        return appointmentMapper.toResponse(appointment);
    }

    @Transactional
    public void cancel(UUID id){
        Appointment appointment = this.appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programarea nu a fost gasita"));

        appointmentStateMachine.assertTransition(appointment.getStatus(), AppointmentStatus.CANCELLED);

        appointment.setStatus(AppointmentStatus.CANCELLED);
        this.appointmentRepository.save(appointment);
    }

    @Transactional
    public void confirm(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programarea nu a fost gasita"));

        appointmentStateMachine.assertTransition(appointment.getStatus(), AppointmentStatus.CONFIRMED);

        appointment.setStatus(AppointmentStatus.CONFIRMED);
        this.appointmentRepository.save(appointment);
    }

    @Transactional
    public void checkIn(UUID id, UUID callerUserId, boolean isDoctorRole) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programarea nu a fost gasita"));

        if(isDoctorRole) {
            Doctor doctor = doctorRepository.findByUserId(callerUserId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctorul nu a fost gasit"));

            if(!doctor.getId().equals(appointment.getDoctor().getId()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doctorul nu a fost gasit");
        }

        appointmentStateMachine.assertTransition(appointment.getStatus(), AppointmentStatus.IN_PROGRESS);

        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        this.appointmentRepository.save(appointment);
    }

    @Transactional
    public void complete(UUID id, UUID callingDoctorId) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programarea nu a fost gasita"));

        Doctor doctor = doctorRepository.findById(callingDoctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctorul nu a fost gasit"));

        appointmentStateMachine.assertTransition(appointment.getStatus(), AppointmentStatus.COMPLETED);

        appointment.setStatus(AppointmentStatus.COMPLETED);
        appointment.setCompletedAt(LocalDateTime.now());
        this.appointmentRepository.save(appointment);
    }

    @Transactional
    public void noShow(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Programarea nu a fost gasita"));

        appointmentStateMachine.assertTransition(appointment.getStatus(), AppointmentStatus.NO_SHOW);

        appointment.setStatus(AppointmentStatus.NO_SHOW);
        this.appointmentRepository.save(appointment);
    }
}
