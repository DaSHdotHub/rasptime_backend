package io.github.dashdothub.rasptime_backend.dto;

import lombok.Data;

@Data
public class PunchRequest {
    private String rfid;
    private Integer breakMinutes;  // Optional, for clock-out
}