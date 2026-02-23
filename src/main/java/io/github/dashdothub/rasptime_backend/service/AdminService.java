package io.github.dashdothub.rasptime_backend.service;

import io.github.dashdothub.rasptime_backend.dto.*;
import io.github.dashdothub.rasptime_backend.entity.AuditAction;
import io.github.dashdothub.rasptime_backend.entity.Role;
import io.github.dashdothub.rasptime_backend.entity.User;
import io.github.dashdothub.rasptime_backend.entity.TimeEntry;
import io.github.dashdothub.rasptime_backend.repository.UserRepository;
import io.github.dashdothub.rasptime_backend.repository.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final TimeEntryRepository timeEntryRepository;
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

    public List<WeeklyUserSummary> getWeeklyOverview(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(4); // Mon-Fri

        List<User> users = userRepository.findAllByActiveTrue();

        return users.stream().map(user -> {
            Map<LocalDate, Long> dailyMinutes = new HashMap<>();

            List<TimeEntry> entries = timeEntryRepository.findByUserAndWorkDateBetween(user, weekStart, weekEnd);

            for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
                final LocalDate currentDate = date;
                long dayMinutes = entries.stream()
                        .filter(e -> e.getWorkDate().equals(currentDate))
                        .filter(e -> e.getPunchOut() != null)
                        .mapToLong(e -> {
                            long gross = Duration.between(e.getPunchIn(), e.getPunchOut()).toMinutes();
                            return gross - (e.getBreakMinutes() != null ? e.getBreakMinutes() : 0);
                        })
                        .sum();
                dailyMinutes.put(date, dayMinutes);
            }

            long totalMinutes = dailyMinutes.values().stream().mapToLong(Long::longValue).sum();

            return WeeklyUserSummary.builder()
                    .userId(user.getId())
                    .displayName(user.getDisplayName())
                    .clockedIn(user.isClockedIn())
                    .dailyMinutes(dailyMinutes)
                    .totalMinutes(totalMinutes)
                    .contractedMinutesPerWeek(user.getContractedMinutesPerWeek())
                    .build();
        }).toList();
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
                .contractedMinutesPerWeek(request.getContractedMinutesPerWeek() != null ? request.getContractedMinutesPerWeek() : 2400)
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
        if (request.getContractedMinutesPerWeek() != null) {
            user.setContractedMinutesPerWeek(request.getContractedMinutesPerWeek());
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

    public TimeReportResponse getTimeReport(Long userId, LocalDate from, LocalDate to) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    List<TimeEntry> entries = timeEntryRepository.findByUserAndWorkDateBetween(user, from, to);

    List<TimeEntryResponse> entryResponses = entries.stream()
            .map(TimeEntryResponse::from)
            .toList();

    long totalNet = entryResponses.stream()
            .filter(e -> e.getNetMinutes() != null)
            .mapToLong(TimeEntryResponse::getNetMinutes)
            .sum();

    int totalDays = (int) entries.stream()
            .map(TimeEntry::getWorkDate)
            .distinct()
            .count();

    return TimeReportResponse.builder()
            .userId(user.getId())
            .displayName(user.getDisplayName())
            .from(from)
            .to(to)
            .entries(entryResponses)
            .totalNetMinutes(totalNet)
            .totalDays(totalDays)
            .build();
    }
}
