package com.holisun.backend.repository.projection;

import java.util.UUID;

public interface ResourceMinutes {

    UUID getResourceId();

    double getBookedMinutes();
}