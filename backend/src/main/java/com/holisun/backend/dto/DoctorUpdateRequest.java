package com.holisun.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class DoctorUpdateRequest {

    @NotBlank
    private String specialty;

    private String contactInfo;

    @NotNull
    @Positive
    private Integer standardConsultationDurationMinutes;

    @Valid
    private List<WorkingHoursDto> weeklySchedule;

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getContactInfo() {
        return contactInfo;
    }

    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }

    public Integer getStandardConsultationDurationMinutes() {
        return standardConsultationDurationMinutes;
    }

    public void setStandardConsultationDurationMinutes(Integer standardConsultationDurationMinutes) {
        this.standardConsultationDurationMinutes = standardConsultationDurationMinutes;
    }

    public List<WorkingHoursDto> getWeeklySchedule() {
        return weeklySchedule;
    }

    public void setWeeklySchedule(List<WorkingHoursDto> weeklySchedule) {
        this.weeklySchedule = weeklySchedule;
    }
}