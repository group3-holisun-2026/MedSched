package com.holisun.backend.dto;

import com.holisun.backend.enums.AppointmentStatus;
import java.time.LocalDateTime;

public record PublicAppointmentResponse(
    String patientFirstName, 
    LocalDateTime startTime, 
    LocalDateTime endTime,
    String doctorName, 
    String roomName, 
    AppointmentStatus status,
    boolean canConfirm, 
    boolean canCancel
) {}
