package io.github.dashdothub.rasptime_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PunchResponse {
    private String action;
    private String message;
    private String displayName;
    private LocalDateTime timestamp;
    private Integer breakMinutes;
}