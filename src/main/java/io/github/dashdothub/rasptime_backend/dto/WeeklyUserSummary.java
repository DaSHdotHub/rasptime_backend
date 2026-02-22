package io.github.dashdothub.rasptime_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
public class WeeklyUserSummary {
    private Long userId;
    private String displayName;
    private boolean clockedIn;
    private Map<LocalDate, Long> dailyMinutes;
    private Long totalMinutes;
}