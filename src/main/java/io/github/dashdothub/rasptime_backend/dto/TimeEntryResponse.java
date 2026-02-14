package io.github.dashdothub.rasptime_backend.dto;

import io.github.dashdothub.rasptime_backend.entity.TimeEntry;
import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class TimeEntryResponse {
    private Long id;
    private LocalDate workDate;
    private LocalDateTime punchIn;
    private LocalDateTime punchOut;
    private Integer breakMinutes;
    private Long netMinutes;
    private boolean autoClosedOut;

    public static TimeEntryResponse from(TimeEntry entry) {
        Long net = null;
        if (entry.getPunchOut() != null) {
            long gross = Duration.between(entry.getPunchIn(), entry.getPunchOut()).toMinutes();
            net = gross - (entry.getBreakMinutes() != null ? entry.getBreakMinutes() : 0);
        }

        return TimeEntryResponse.builder()
                .id(entry.getId())
                .workDate(entry.getWorkDate())
                .punchIn(entry.getPunchIn())
                .punchOut(entry.getPunchOut())
                .breakMinutes(entry.getBreakMinutes())
                .netMinutes(net)
                .autoClosedOut(entry.isAutoClosedOut())
                .build();
    }
}
