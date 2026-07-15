package com.holisun.backend.service;

import com.holisun.backend.dto.DoctorCreateRequest;
import com.holisun.backend.dto.DoctorResponse;
import com.holisun.backend.dto.DoctorUpdateRequest;
import com.holisun.backend.entity.Doctor;
import com.holisun.backend.entity.User;
import com.holisun.backend.mapper.DoctorMapper;
import com.holisun.backend.repository.DoctorRepository;
import com.holisun.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final DoctorMapper doctorMapper;

    @Override
    @Transactional
    public DoctorResponse create(DoctorCreateRequest dto) {

        User user = userRepository.findById(dto.userId())
                .orElseThrow(() -> new EntityNotFoundException("Utilizatorul cu ID-ul " + dto.userId() + " nu există!"));


        if (doctorRepository.existsByUserId(dto.userId())) {
            throw new IllegalStateException("Acest utilizator este deja înregistrat ca doctor!");
        }


        Doctor doctor = doctorMapper.toEntity(dto);


        doctor.setUser(user);


        Doctor savedDoctor = doctorRepository.save(doctor);


        return doctorMapper.toResponse(savedDoctor);
    }

    @Override
    @Transactional
    public DoctorResponse update(UUID id, DoctorUpdateRequest dto) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Doctorul cu ID-ul " + id + " nu a fost găsit!"));


        doctorMapper.updateEntityFromDto(dto, doctor);

        Doctor updatedDoctor = doctorRepository.save(doctor);
        return doctorMapper.toResponse(updatedDoctor);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getById(UUID id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Doctorul cu ID-ul " + id + " nu a fost găsit!"));
        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponse> getAll() {
        return doctorRepository.findAll().stream()
                .map(doctorMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!doctorRepository.existsById(id)) {
            throw new EntityNotFoundException("Doctorul cu ID-ul " + id + " nu poate fi șters pentru că nu există!");
        }
        doctorRepository.deleteById(id);
    }
}