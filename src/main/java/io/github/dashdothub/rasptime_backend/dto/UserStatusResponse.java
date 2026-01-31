package io.github.dashdothub.rasptime_backend.dto;

import io.github.dashdothub.rasptime_backend.entity.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserStatusResponse {
    private Long userId;
    private String displayName;
    private Role role;
    private boolean clockedIn;
}