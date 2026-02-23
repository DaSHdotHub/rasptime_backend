package io.github.dashdothub.rasptime_backend.dto;

import io.github.dashdothub.rasptime_backend.entity.Role;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank(message = "RFID tag is required")
    private String rfidTag;

    @NotBlank(message = "Display name is required")
    private String displayName;

    private Role role;

    private Integer contractedMinutesPerWeek;
}
