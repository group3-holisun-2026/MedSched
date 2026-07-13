package com.holisun.backend.service;

import com.holisun.backend.dto.DoctorCreateRequest;
import com.holisun.backend.dto.DoctorResponse;
import com.holisun.backend.dto.DoctorUpdateRequest;
import com.holisun.backend.dto.WorkingHoursDto;
import com.holisun.backend.embeddables.WorkingHours;
import com.holisun.backend.entity.Doctor;
import com.holisun.backend.entity.User;
import com.holisun.backend.repository.DoctorRepository;
import com.holisun.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;

    public DoctorResponse create(DoctorCreateRequest dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setSpeciality(dto.getSpecialty());
        doctor.setContactInfo(dto.getContactInfo());
        doctor.setStandardConsultationDurationMinutes(dto.getStandardConsultationDurationMinutes());
        doctor.setWeeklySchedule(toEntityList(dto.getWeeklySchedule()));

        Doctor saved = doctorRepository.save(doctor);
        return toResponse(saved);
    }

    public DoctorResponse update(UUID id, DoctorUpdateRequest dto) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));

        doctor.setSpeciality(dto.getSpecialty());
        doctor.setContactInfo(dto.getContactInfo());
        doctor.setStandardConsultationDurationMinutes(dto.getStandardConsultationDurationMinutes());
        if (dto.getWeeklySchedule() != null) {
            doctor.setWeeklySchedule(toEntityList(dto.getWeeklySchedule()));
        }

        Doctor saved = doctorRepository.save(doctor);
        return toResponse(saved);
    }

    public DoctorResponse getById(UUID id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
        return toResponse(doctor);
    }

    public List<DoctorResponse> getAll() {
        return doctorRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void delete(UUID id) {
        if (!doctorRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found");
        }
        doctorRepository.deleteById(id);
    }

    public List<WorkingHoursDto> getWorkingHours(UUID id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
        return toDtoList(doctor.getWeeklySchedule());
    }

    private List<WorkingHours> toEntityList(List<WorkingHoursDto> dtos) {
        return dtos.stream()
                .map(d -> {
                    WorkingHours wh = new WorkingHours();
                    wh.setDayOfWeek(d.getDayOfWeek());
                    wh.setStartTime(d.getStartTime());
                    wh.setEndTime(d.getEndTime());
                    return wh;
                })
                .toList();
    }

    private List<WorkingHoursDto> toDtoList(List<WorkingHours> entities) {
        return entities.stream()
                .map(wh -> new WorkingHoursDto(wh.getDayOfWeek(), wh.getStartTime(), wh.getEndTime()))
                .toList();
    }

    private DoctorResponse toResponse(Doctor doctor) {
        return new DoctorResponse(
                doctor.getId(),
                doctor.getUser().getUsername(),
                doctor.getSpeciality(),
                doctor.getContactInfo(),
                doctor.getStandardConsultationDurationMinutes(),
                toDtoList(doctor.getWeeklySchedule())
        );
    }
}