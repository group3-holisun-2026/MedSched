package com.holisun.backend.service;

import com.holisun.backend.repository.AppointmentRepository;
import com.holisun.backend.repository.EquipmentRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.holisun.backend.entity.Equipment;
import com.holisun.backend.entity.Service;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verifyNoInteractions;

import com.holisun.backend.entity.EquipmentType;

import com.holisun.backend.exception.ResourceConflictException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import com.holisun.backend.entity.Appointment;

import com.holisun.backend.entity.Room;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class EquipmentAllocationServiceTest {

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private EquipmentAllocationService allocationService;

    @Test
    void returnsNullWhenServiceRequiresNoEquipment() {
        Service clinicalService = new Service();

        LocalDateTime start =
                LocalDateTime.of(2026, 7, 23, 10, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 7, 23, 10, 30);

        Equipment result = allocationService.allocate(
                clinicalService,
                UUID.randomUUID(),
                start,
                end,
                null
        );

        assertNull(result);

        verifyNoInteractions(
                equipmentRepository,
                appointmentRepository
        );
    }

    @Test
    void returnsAvailableEquipment() {
        EquipmentType requiredType = new EquipmentType();
        requiredType.setId(UUID.randomUUID());
        requiredType.setName("Ecograf");

        Service clinicalService = new Service();
        clinicalService.setRequiredEquipmentTypes(
                Set.of(requiredType)
        );

        Equipment availableEquipment = new Equipment();
        availableEquipment.setId(UUID.randomUUID());
        availableEquipment.setName("Ecograf 1");
        availableEquipment.setEquipmentType(requiredType);
        availableEquipment.setActive(true);

        LocalDateTime start =
                LocalDateTime.of(2026, 7, 23, 10, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 7, 23, 10, 30);

        when(
                equipmentRepository.findByEquipmentTypeIdAndActiveTrue(
                        requiredType.getId()
                )
        ).thenReturn(List.of(availableEquipment));

        when(
                appointmentRepository.findOverlappingForEquipment(
                        availableEquipment.getId(),
                        start,
                        end,
                        null
                )
        ).thenReturn(List.of());

        Equipment result = allocationService.allocate(
                clinicalService,
                UUID.randomUUID(),
                start,
                end,
                null
        );

        assertSame(availableEquipment, result);
    }

    @Test
    void skipsOccupiedEquipmentAndReturnsNextAvailableOne() {
        EquipmentType requiredType = new EquipmentType();
        requiredType.setId(UUID.randomUUID());
        requiredType.setName("Ecograf");

        Service clinicalService = new Service();
        clinicalService.setRequiredEquipmentTypes(
                Set.of(requiredType)
        );

        Equipment occupiedEquipment = new Equipment();
        occupiedEquipment.setId(UUID.randomUUID());
        occupiedEquipment.setName("Ecograf ocupat");
        occupiedEquipment.setEquipmentType(requiredType);
        occupiedEquipment.setActive(true);

        Equipment availableEquipment = new Equipment();
        availableEquipment.setId(UUID.randomUUID());
        availableEquipment.setName("Ecograf liber");
        availableEquipment.setEquipmentType(requiredType);
        availableEquipment.setActive(true);

        LocalDateTime start =
                LocalDateTime.of(2026, 7, 23, 10, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 7, 23, 10, 30);

        when(
                equipmentRepository.findByEquipmentTypeIdAndActiveTrue(
                        requiredType.getId()
                )
        ).thenReturn(
                List.of(occupiedEquipment, availableEquipment)
        );

        when(
                appointmentRepository.findOverlappingForEquipment(
                        occupiedEquipment.getId(),
                        start,
                        end,
                        null
                )
        ).thenReturn(
                List.of(new Appointment())
        );

        when(
                appointmentRepository.findOverlappingForEquipment(
                        availableEquipment.getId(),
                        start,
                        end,
                        null
                )
        ).thenReturn(List.of());

        Equipment result = allocationService.allocate(
                clinicalService,
                UUID.randomUUID(),
                start,
                end,
                null
        );

        assertSame(availableEquipment, result);
    }
    @Test
    void prefersEquipmentAlreadyInRequestedRoom() {
        EquipmentType requiredType = new EquipmentType();
        requiredType.setId(UUID.randomUUID());
        requiredType.setName("Ecograf");

        Service clinicalService = new Service();
        clinicalService.setRequiredEquipmentTypes(
                Set.of(requiredType)
        );

        Room requestedRoom = new Room();
        requestedRoom.setId(UUID.randomUUID());
        requestedRoom.setName("Cabinet 3");

        Equipment mobileEquipment = new Equipment();
        mobileEquipment.setId(UUID.randomUUID());
        mobileEquipment.setName("Ecograf mobil");
        mobileEquipment.setEquipmentType(requiredType);
        mobileEquipment.setActive(true);
        mobileEquipment.setRoom(null);

        Equipment equipmentInRequestedRoom = new Equipment();
        equipmentInRequestedRoom.setId(UUID.randomUUID());
        equipmentInRequestedRoom.setName("Ecograf Cabinet 3");
        equipmentInRequestedRoom.setEquipmentType(requiredType);
        equipmentInRequestedRoom.setActive(true);
        equipmentInRequestedRoom.setRoom(requestedRoom);

        LocalDateTime start =
                LocalDateTime.of(2026, 7, 23, 10, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 7, 23, 10, 30);

        when(
                equipmentRepository.findByEquipmentTypeIdAndActiveTrue(
                        requiredType.getId()
                )
        ).thenReturn(
                List.of(mobileEquipment, equipmentInRequestedRoom)
        );

        when(
                appointmentRepository.findOverlappingForEquipment(
                        equipmentInRequestedRoom.getId(),
                        start,
                        end,
                        null
                )
        ).thenReturn(List.of());

        Equipment result = allocationService.allocate(
                clinicalService,
                requestedRoom.getId(),
                start,
                end,
                null
        );

        assertSame(equipmentInRequestedRoom, result);

        verify(
                appointmentRepository,
                never()
        ).findOverlappingForEquipment(
                mobileEquipment.getId(),
                start,
                end,
                null
        );
    }
    @Test
    void passesExcludedAppointmentIdWhenCheckingAvailability() {
        EquipmentType requiredType = new EquipmentType();
        requiredType.setId(UUID.randomUUID());
        requiredType.setName("Ecograf");

        Service clinicalService = new Service();
        clinicalService.setRequiredEquipmentTypes(
                Set.of(requiredType)
        );

        Equipment availableEquipment = new Equipment();
        availableEquipment.setId(UUID.randomUUID());
        availableEquipment.setName("Ecograf 1");
        availableEquipment.setEquipmentType(requiredType);
        availableEquipment.setActive(true);

        UUID excludedAppointmentId = UUID.randomUUID();

        LocalDateTime start =
                LocalDateTime.of(2026, 7, 23, 10, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 7, 23, 10, 30);

        when(
                equipmentRepository.findByEquipmentTypeIdAndActiveTrue(
                        requiredType.getId()
                )
        ).thenReturn(List.of(availableEquipment));

        when(
                appointmentRepository.findOverlappingForEquipment(
                        availableEquipment.getId(),
                        start,
                        end,
                        excludedAppointmentId
                )
        ).thenReturn(List.of());

        Equipment result = allocationService.allocate(
                clinicalService,
                UUID.randomUUID(),
                start,
                end,
                excludedAppointmentId
        );

        assertSame(availableEquipment, result);

        verify(appointmentRepository)
                .findOverlappingForEquipment(
                        availableEquipment.getId(),
                        start,
                        end,
                        excludedAppointmentId
                );
    }

    @Test
    void throwsConflictWhenAllEquipmentIsOccupied() {
        EquipmentType requiredType = new EquipmentType();
        requiredType.setId(UUID.randomUUID());
        requiredType.setName("Ecograf");

        Service clinicalService = new Service();
        clinicalService.setRequiredEquipmentTypes(
                Set.of(requiredType)
        );

        Equipment occupiedEquipment = new Equipment();
        occupiedEquipment.setId(UUID.randomUUID());
        occupiedEquipment.setName("Ecograf ocupat");
        occupiedEquipment.setEquipmentType(requiredType);
        occupiedEquipment.setActive(true);

        LocalDateTime start =
                LocalDateTime.of(2026, 7, 23, 10, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 7, 23, 10, 30);

        when(
                equipmentRepository.findByEquipmentTypeIdAndActiveTrue(
                        requiredType.getId()
                )
        ).thenReturn(List.of(occupiedEquipment));

        when(
                appointmentRepository.findOverlappingForEquipment(
                        occupiedEquipment.getId(),
                        start,
                        end,
                        null
                )
        ).thenReturn(List.of(new Appointment()));

        ResourceConflictException exception = assertThrows(
                ResourceConflictException.class,
                () -> allocationService.allocate(
                        clinicalService,
                        UUID.randomUUID(),
                        start,
                        end,
                        null
                )
        );

        assertEquals(
                ResourceConflictException.ResourceType.EQUIPMENT,
                exception.getConflictingResource()
        );

        assertTrue(
                exception.getMessage().contains("Ecograf")
        );
    }
}