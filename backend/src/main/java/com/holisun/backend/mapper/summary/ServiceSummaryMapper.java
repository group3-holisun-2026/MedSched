package com.holisun.backend.mapper.summary;

import com.holisun.backend.dto.summary.RoomSummary;
import com.holisun.backend.dto.summary.ServiceSummary;
import com.holisun.backend.entity.Room;
import com.holisun.backend.entity.Service;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServiceSummaryMapper {
    ServiceSummary toSummary(Service service);
}
