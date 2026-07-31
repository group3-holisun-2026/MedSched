package com.holisun.backend.repository.projection;

import java.util.UUID;

public interface PatientNoShowCounts {

    UUID getPatientId();

    String getPatientName();

    long getTotal();

    long getNoShows();
}