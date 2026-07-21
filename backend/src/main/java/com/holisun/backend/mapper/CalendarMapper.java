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
    @Mapping(target = "patientName",
            expression = "java(appointment.getPatient().getFirstName() + \" \" + appointment.getPatient().getLastName())")
    CalendarAppointmentResponse toDto(Appointment appointment);


    List<CalendarAppointmentResponse> toDto(List<Appointment> appointments);
}
