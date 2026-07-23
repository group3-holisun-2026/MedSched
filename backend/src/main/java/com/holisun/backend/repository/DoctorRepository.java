package com.holisun.backend.repository;

import com.holisun.backend.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
    // verificare daca un user este deja doctor
    boolean existsByUserId(UUID userId);

    // module3_p4:
    //  Needed by getByDateRangeAndDoctors() when no doctor IDs are given... or something like that
    @Query("select d.id from Doctor d where d.active = true")
    List<UUID> findActiveDoctorIds();

    Optional<Doctor> findByUserId(UUID userId);


}
