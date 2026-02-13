package io.github.dashdothub.rasptime_backend.service;

import io.github.dashdothub.rasptime_backend.dto.CreateUserRequest;
import io.github.dashdothub.rasptime_backend.dto.UpdateUserRequest;
import io.github.dashdothub.rasptime_backend.dto.UserResponse;
import io.github.dashdothub.rasptime_backend.entity.AuditAction;
import io.github.dashdothub.rasptime_backend.entity.Role;
import io.github.dashdothub.rasptime_backend.entity.User;
import io.github.dashdothub.rasptime_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final AuditService auditService;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    public List<UserResponse> getActiveUsers() {
        return userRepository.findAllByActiveTrue().stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByRfidTag(request.getRfidTag())) {
            throw new IllegalArgumentException("RFID tag already exists");
        }

        User user = User.builder()
                .rfidTag(request.getRfidTag())
                .displayName(request.getDisplayName())
                .role(request.getRole() != null ? request.getRole() : Role.USER)
                .build();

        user = userRepository.save(user);

        auditService.log(AuditAction.USER_CREATED, user.getId(), user.getRfidTag(),
                "Created user: " + user.getDisplayName());

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getRfidTag() != null) {
            if (!user.getRfidTag().equals(request.getRfidTag())
                    && userRepository.existsByRfidTag(request.getRfidTag())) {
                throw new IllegalArgumentException("RFID tag already exists");
            }
            user.setRfidTag(request.getRfidTag());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        user = userRepository.save(user);

        auditService.log(AuditAction.USER_UPDATED, user.getId(), user.getRfidTag(),
                "Updated user: " + user.getDisplayName());

        return UserResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setActive(false);
        userRepository.save(user);

        auditService.log(AuditAction.USER_DELETED, user.getId(), user.getRfidTag(),
                "Soft deleted user: " + user.getDisplayName());
    }
}