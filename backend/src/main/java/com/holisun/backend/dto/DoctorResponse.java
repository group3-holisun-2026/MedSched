package com.holisun.backend.dto;

import java.util.List;
import java.util.UUID;

public class DoctorResponse {

    private UUID id;
    private String fullName;
    private String specialty;
    private String contactInfo;
    private Integer standardConsultationDurationMinutes;
    private List<WorkingHoursDto> weeklySchedule;

    public DoctorResponse() {
    }

    public DoctorResponse(UUID id, String fullName, String specialty, String contactInfo,
                          Integer standardConsultationDurationMinutes,
                          List<WorkingHoursDto> weeklySchedule) {
        this.id = id;
        this.fullName = fullName;
        this.specialty = specialty;
        this.contactInfo = contactInfo;
        this.standardConsultationDurationMinutes = standardConsultationDurationMinutes;
        this.weeklySchedule = weeklySchedule;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

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