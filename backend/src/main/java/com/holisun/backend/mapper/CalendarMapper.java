package com.holisun.backend.mapper;

import com.holisun.backend.dto.CalendarAppointmentResponse;
import com.holisun.backend.entity.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CalendarMapper {

    @Mapping(target = "doctorName",
            source = "doctor.user.username")
    @Mapping(target = "roomName",
            source = "room.name")
    // Fara maparea asta serviceName ramanea null pe fiecare programare (Appointment are `service`,
    // nu `serviceName`, deci potrivirea automata dupa nume nu avea ce sa gaseasca), iar calendarul
    // afisa "Pacient — null (Medic)".
    @Mapping(target = "serviceName",
            source = "service.name")
    @Mapping(target = "patientName",
            expression = "java(appointment.getPatient().getFirstName() + \" \" + appointment.getPatient().getLastName())")
    CalendarAppointmentResponse toDto(Appointment appointment);


    List<CalendarAppointmentResponse> toDto(List<Appointment> appointments);
}
