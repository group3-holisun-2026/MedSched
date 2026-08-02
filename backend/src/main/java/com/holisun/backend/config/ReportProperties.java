package com.holisun.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "app.reports")
@Getter
@Setter
public class ReportProperties {

    private LocalTime clinicOpenTime = LocalTime.of(8, 0);

    private LocalTime clinicCloseTime = LocalTime.of(20, 0);

    private Set<DayOfWeek> clinicWorkingDays = EnumSet.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
    );
}