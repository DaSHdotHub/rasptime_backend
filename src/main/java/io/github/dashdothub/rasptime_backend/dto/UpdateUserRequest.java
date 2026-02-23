package io.github.dashdothub.rasptime_backend.dto;

import io.github.dashdothub.rasptime_backend.entity.Role;
import lombok.Data;

@Data
public class UpdateUserRequest {
    private String displayName;
    private String rfidTag;
    private Role role;
    private Boolean active;
    private Integer contractedMinutesPerWeek;
}
