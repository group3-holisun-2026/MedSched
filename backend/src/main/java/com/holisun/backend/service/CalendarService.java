package com.holisun.backend.service;

import com.holisun.backend.dto.CalendarAppointmentResponse;
import com.holisun.backend.entity.Appointment;
import com.holisun.backend.entity.Doctor;
import com.holisun.backend.mapper.CalendarMapper;
import com.holisun.backend.repository.AppointmentRepository;
import com.holisun.backend.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CalendarService {

    @Autowired
    private AppointmentRepository appointmentRepository;
    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private CalendarMapper mapper;

    public List<CalendarAppointmentResponse> getByDateRangeAndDoctors (LocalDateTime from, LocalDateTime to, List<UUID> doctorIds){
        validateDateRange(from, to);
        List<UUID> ids = doctorIds;
        if(ids == null  || ids.isEmpty()){
            ids = doctorRepository.findActiveDoctorIds();
        }
        return mapper.toDto(appointmentRepository.findByDoctorIdInAndStartTimeLessThanAndEndTimeGreaterThan(ids,to,from));

    }
    public List<CalendarAppointmentResponse> getByDateRangeAndRoom(LocalDateTime from, LocalDateTime to, UUID roomId){
        validateDateRange(from,to);
        List<Appointment> appointments = appointmentRepository.findByRoomIdAndStartTimeBetween(roomId,from,to);

        return mapper.toDto(appointments);
    }
    public List<CalendarAppointmentResponse> getForDoctor(UUID doctorId, LocalDateTime from, LocalDateTime to){
        return getByDateRangeAndDoctors(from,to,List.of(doctorId));
    }

    private void validateDateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Both from and to must be provided.");
        }

        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be before to.");
        }
    }

}
