package com.holisun.backend.service;

import com.holisun.backend.dto.RoomRequest;
import com.holisun.backend.dto.RoomResponse;
import com.holisun.backend.entity.Room;
import com.holisun.backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomResponse create(RoomRequest dto) {
        if (roomRepository.existsByName(dto.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room name already exists");
        }

        Room room = new Room();
        room.setName(dto.getName());
        room.setDescription(dto.getDescription());
        room.setActive(true);

        Room saved = roomRepository.save(room);
        return toResponse(saved);
    }

    public RoomResponse update(UUID id, RoomRequest dto) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        room.setName(dto.getName());
        room.setDescription(dto.getDescription());

        Room saved = roomRepository.save(room);
        return toResponse(saved);
    }

    public RoomResponse getById(UUID id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        return toResponse(room);
    }

    public List<RoomResponse> getAll() {
        return roomRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void delete(UUID id) {
        if (!roomRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        roomRepository.deleteById(id);
    }

    private RoomResponse toResponse(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getDescription(),
                room.isActive()
        );
    }
}