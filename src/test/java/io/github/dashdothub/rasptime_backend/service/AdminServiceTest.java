package io.github.dashdothub.rasptime_backend.service;

import io.github.dashdothub.rasptime_backend.dto.CreateUserRequest;
import io.github.dashdothub.rasptime_backend.dto.UpdateUserRequest;
import io.github.dashdothub.rasptime_backend.dto.UserResponse;
import io.github.dashdothub.rasptime_backend.entity.Role;
import io.github.dashdothub.rasptime_backend.entity.User;
import io.github.dashdothub.rasptime_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService RFID Validation Tests")
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AdminService adminService;

    private User existingUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(1L)
                .rfidTag("EXISTING123")
                .displayName("Existing User")
                .role(Role.USER)
                .clockedIn(false)
                .active(true)
                .contractedMinutesPerWeek(2400)
                .createdAt(LocalDateTime.now())
                .build();

        anotherUser = User.builder()
                .id(2L)
                .rfidTag("ANOTHER456")
                .displayName("Another User")
                .role(Role.USER)
                .clockedIn(false)
                .active(true)
                .contractedMinutesPerWeek(2400)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Create User - RFID Validation")
    class CreateUserRfidValidationTests {

        @Test
        @DisplayName("Should create user when RFID tag is not in use")
        void createUserWithUniqueRfid() {
            // Given
            CreateUserRequest request = new CreateUserRequest();
            request.setRfidTag("NEW_RFID_123");
            request.setDisplayName("New User");
            request.setRole(Role.USER);
            request.setContractedMinutesPerWeek(2400);

            when(userRepository.existsByRfidTagAndActiveTrue("NEW_RFID_123")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(3L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });

            // When
            UserResponse response = adminService.createUser(request);

            // Then
            assertNotNull(response);
            assertEquals("New User", response.getDisplayName());
            assertEquals("NEW_RFID_123", response.getRfidTag());
            verify(userRepository).existsByRfidTagAndActiveTrue("NEW_RFID_123");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when RFID tag is already in use by active user")
        void createUserWithDuplicateRfid() {
            // Given
            CreateUserRequest request = new CreateUserRequest();
            request.setRfidTag("EXISTING123");
            request.setDisplayName("New User");
            request.setRole(Role.USER);

            when(userRepository.existsByRfidTagAndActiveTrue("EXISTING123")).thenReturn(true);

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> adminService.createUser(request)
            );

            assertEquals("RFID tag is already assigned to an active user", exception.getMessage());
            verify(userRepository).existsByRfidTagAndActiveTrue("EXISTING123");
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should allow creating user with RFID from deactivated user")
        void createUserWithRfidFromDeactivatedUser() {
            // Given
            CreateUserRequest request = new CreateUserRequest();
            request.setRfidTag("DEACTIVATED_RFID");
            request.setDisplayName("New User");
            request.setRole(Role.USER);
            request.setContractedMinutesPerWeek(2400);

            // RFID exists but user is inactive, so existsByRfidTagAndActiveTrue returns false
            when(userRepository.existsByRfidTagAndActiveTrue("DEACTIVATED_RFID")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(4L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });

            // When
            UserResponse response = adminService.createUser(request);

            // Then
            assertNotNull(response);
            assertEquals("DEACTIVATED_RFID", response.getRfidTag());
            verify(userRepository).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Update User - RFID Validation")
    class UpdateUserRfidValidationTests {

        @Test
        @DisplayName("Should update user when keeping same RFID tag")
        void updateUserKeepingSameRfid() {
            // Given
            UpdateUserRequest request = new UpdateUserRequest();
            request.setRfidTag("EXISTING123");  // Same as current
            request.setDisplayName("Updated Name");

            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenReturn(existingUser);

            // When
            UserResponse response = adminService.updateUser(1L, request);

            // Then
            assertNotNull(response);
            // Should NOT check for duplicate since it's the same tag
            verify(userRepository, never()).existsByRfidTagAndActiveTrue(anyString());
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should update user when changing to unused RFID tag")
        void updateUserWithNewUnusedRfid() {
            // Given
            UpdateUserRequest request = new UpdateUserRequest();
            request.setRfidTag("BRAND_NEW_TAG");
            request.setDisplayName("Updated Name");

            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userRepository.existsByRfidTagAndActiveTrue("BRAND_NEW_TAG")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserResponse response = adminService.updateUser(1L, request);

            // Then
            assertNotNull(response);
            assertEquals("BRAND_NEW_TAG", response.getRfidTag());
            verify(userRepository).existsByRfidTagAndActiveTrue("BRAND_NEW_TAG");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when changing to RFID already used by another user")
        void updateUserWithRfidUsedByAnotherUser() {
            // Given
            UpdateUserRequest request = new UpdateUserRequest();
            request.setRfidTag("ANOTHER456");  // Already used by anotherUser
            request.setDisplayName("Updated Name");

            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userRepository.existsByRfidTagAndActiveTrue("ANOTHER456")).thenReturn(true);

            // When & Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> adminService.updateUser(1L, request)
            );

            assertEquals("RFID tag is already assigned to an active user", exception.getMessage());
            verify(userRepository).existsByRfidTagAndActiveTrue("ANOTHER456");
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void updateUserNotFound() {
            // Given
            UpdateUserRequest request = new UpdateUserRequest();
            request.setDisplayName("Updated Name");

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> adminService.updateUser(999L, request)
            );

            assertEquals("User not found", exception.getMessage());
        }

        @Test
        @DisplayName("Should update other fields without changing RFID")
        void updateUserWithoutChangingRfid() {
            // Given
            UpdateUserRequest request = new UpdateUserRequest();
            request.setDisplayName("New Display Name");
            request.setRole(Role.ADMIN);
            request.setContractedMinutesPerWeek(1200);
            // rfidTag is null - should not change

            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserResponse response = adminService.updateUser(1L, request);

            // Then
            assertNotNull(response);
            assertEquals("New Display Name", response.getDisplayName());
            assertEquals(Role.ADMIN, response.getRole());
            assertEquals(1200, response.getContractedMinutesPerWeek());
            // Should NOT check for duplicate since RFID wasn't changed
            verify(userRepository, never()).existsByRfidTagAndActiveTrue(anyString());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty RFID tag on update")
        void updateUserWithEmptyRfid() {
            // Given - updating with empty string should be treated as a change
            UpdateUserRequest request = new UpdateUserRequest();
            request.setRfidTag("");
            request.setDisplayName("Updated Name");

            when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
            when(userRepository.existsByRfidTagAndActiveTrue("")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            UserResponse response = adminService.updateUser(1L, request);

            // Then
            assertNotNull(response);
            verify(userRepository).existsByRfidTagAndActiveTrue("");
        }

        @Test
        @DisplayName("Should handle case-sensitive RFID tags")
        void rfidTagsCaseSensitive() {
            // Given
            CreateUserRequest request = new CreateUserRequest();
            request.setRfidTag("existing123");  // lowercase version
            request.setDisplayName("New User");
            request.setRole(Role.USER);
            request.setContractedMinutesPerWeek(2400);

            // Assuming case-sensitive - lowercase version is not in use
            when(userRepository.existsByRfidTagAndActiveTrue("existing123")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(5L);
                user.setCreatedAt(LocalDateTime.now());
                return user;
            });

            // When
            UserResponse response = adminService.createUser(request);

            // Then
            assertNotNull(response);
            assertEquals("existing123", response.getRfidTag());
        }
    }
}
