package com.holisun.backend.mapper.summary;

import com.holisun.backend.dto.summary.RoomSummary;
import com.holisun.backend.entity.Room;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoomSummaryMapper {
    RoomSummary toSummary(Room room);
}
