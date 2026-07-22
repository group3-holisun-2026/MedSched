package com.holisun.backend.mapper;

import com.holisun.backend.dto.AppointmentResponse;
import com.holisun.backend.entity.Appointment;
import com.holisun.backend.mapper.EquipmentMapper;
import com.holisun.backend.mapper.summary.DoctorSummaryMapper;
import com.holisun.backend.mapper.summary.PatientSummaryMapper;
import com.holisun.backend.mapper.summary.RoomSummaryMapper;
import com.holisun.backend.mapper.summary.ServiceSummaryMapper;
import org.mapstruct.Mapper;

@Mapper(
        componentModel = "spring",
        uses = {
                PatientSummaryMapper.class,
                DoctorSummaryMapper.class,
                RoomSummaryMapper.class,
                ServiceSummaryMapper.class,
                EquipmentMapper.class
        }
)
public interface AppointmentMapper {
    AppointmentResponse toResponse(Appointment appointment);
}