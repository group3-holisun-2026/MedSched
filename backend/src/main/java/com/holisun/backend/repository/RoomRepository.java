package com.holisun.backend.repository;

import com.holisun.backend.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {
    boolean existsByName(String name);
}
