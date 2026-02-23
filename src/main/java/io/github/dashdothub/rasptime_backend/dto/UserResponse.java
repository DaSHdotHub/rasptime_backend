package io.github.dashdothub.rasptime_backend.dto;

import io.github.dashdothub.rasptime_backend.entity.Role;
import io.github.dashdothub.rasptime_backend.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String rfidTag;
    private String displayName;
    private Role role;
    private boolean clockedIn;
    private boolean active;
    private Integer contractedMinutesPerWeek;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .rfidTag(user.getRfidTag())
                .displayName(user.getDisplayName())
                .role(user.getRole())
                .clockedIn(user.isClockedIn())
                .active(user.isActive())
                .contractedMinutesPerWeek(user.getContractedMinutesPerWeek())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
