package com.holisun.backend.service;

import com.holisun.backend.entity.Equipment;
import com.holisun.backend.entity.Service;
import com.holisun.backend.repository.EquipmentRepository;
import com.holisun.backend.entity.EquipmentType;
import com.holisun.backend.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;

import com.holisun.backend.exception.ResourceConflictException;
import com.holisun.backend.exception.ResourceConflictException.ResourceType;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class EquipmentAllocationService {

    private final EquipmentRepository equipmentRepository;
    private final AppointmentRepository appointmentRepository;

    public Equipment allocate(
            Service clinicalService,
            UUID roomId,
            LocalDateTime start,
            LocalDateTime end,
            UUID excludeAppointmentId
    ){
        if(clinicalService.getRequiredEquipmentTypes().isEmpty())
        {
            return null;
        }
        /*
         * Appointment can currently store only one Equipment.
         * If a clinical service requires multiple equipment types,
         * supporting all of them will require a future schema change.
         */
        EquipmentType requiredType = clinicalService.getRequiredEquipmentTypes().iterator().next();
        List<Equipment> candidates = new ArrayList<>(
                equipmentRepository.findByEquipmentTypeIdAndActiveTrue(
                        requiredType.getId()
                )
        );
        candidates.sort(Comparator.comparingInt(candidate -> {
            if (
                    roomId != null
                            && candidate.getRoom() != null
                            && roomId.equals(candidate.getRoom().getId())
            ) {
                return 0;
            }
            return 1;
        }));
        for(Equipment candidate : candidates)
        {
            if(appointmentRepository.findOverlappingForEquipment(candidate.getId(), start, end, excludeAppointmentId).isEmpty())
            {
                return candidate;
            }
        }
        throw new ResourceConflictException(
                ResourceType.EQUIPMENT,
                "Niciun echipament de tip "
                        + requiredType.getName()
                        + " disponibil în acest interval"
        );
    }
}
