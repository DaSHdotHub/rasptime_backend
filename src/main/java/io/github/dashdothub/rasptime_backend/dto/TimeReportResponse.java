package io.github.dashdothub.rasptime_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class TimeReportResponse {
    private Long userId;
    private String displayName;
    private LocalDate from;
    private LocalDate to;
    private List<TimeEntryResponse> entries;
    private Long totalNetMinutes;
    private Integer totalDays;
}
