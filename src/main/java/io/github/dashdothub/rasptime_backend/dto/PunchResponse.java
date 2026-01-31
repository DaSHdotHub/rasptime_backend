package io.github.dashdothub.rasptime_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class PunchResponse {
    private String displayName;
    private String action;  // "CLOCK_IN" or "CLOCK_OUT"
    private LocalDateTime timestamp;
    private Integer breakMinutes;
    private String message;
}