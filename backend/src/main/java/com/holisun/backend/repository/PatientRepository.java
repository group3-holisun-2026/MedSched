package com.holisun.backend.repository;

import com.holisun.backend.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByCnpHash(String cnpHash);

    @Query("""
                SELECT p FROM Patient p
                WHERE LOWER(CONCAT(p.firstName, ' ', p.lastName))
                      LIKE LOWER(CONCAT('%', :name, '%'))
            """)
    List<Patient> searchByName(String name);

    List<Patient> findByPhoneContaining(String phone);


    Page<Patient> findByProfileCompleteFalse(Pageable pageable);

    @Query("""
            SELECT p
            FROM Patient p
            WHERE p.profileComplete = false
              AND (
                    LOWER(p.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<Patient> searchIncompleteByName(@Param("keyword") String keyword,
                                         Pageable pageable);
}
