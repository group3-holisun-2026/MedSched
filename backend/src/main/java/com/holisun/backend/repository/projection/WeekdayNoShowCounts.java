package com.holisun.backend.repository.projection;

public interface WeekdayNoShowCounts {

    int getDayOfWeekValue();

    long getTotal();

    long getNoShows();
}