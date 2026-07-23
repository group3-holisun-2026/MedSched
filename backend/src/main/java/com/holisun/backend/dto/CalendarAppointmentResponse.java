package com.holisun.backend.dto;


import com.holisun.backend.enums.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class CalendarAppointmentResponse {
    private UUID id;
    private UUID doctorId;
    private String doctorName;
    private UUID roomId;
    private String roomName;
    private String patientName;
    private String serviceName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AppointmentStatus status;
}