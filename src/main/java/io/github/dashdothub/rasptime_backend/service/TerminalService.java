package io.github.dashdothub.rasptime_backend.service;

import io.github.dashdothub.rasptime_backend.dto.PunchRequest;
import io.github.dashdothub.rasptime_backend.dto.PunchResponse;
import io.github.dashdothub.rasptime_backend.dto.UserStatusResponse;
import io.github.dashdothub.rasptime_backend.entity.AuditAction;
import io.github.dashdothub.rasptime_backend.entity.TimeEntry;
import io.github.dashdothub.rasptime_backend.entity.User;
import io.github.dashdothub.rasptime_backend.repository.TimeEntryRepository;
import io.github.dashdothub.rasptime_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TerminalService {

    private final UserRepository userRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final AuditService auditService;

    public Optional<UserStatusResponse> getUserByRfid(String rfid) {
        return userRepository.findByRfidTagAndActiveTrue(rfid)
                .map(user -> UserStatusResponse.builder()
                        .userId(user.getId())
                        .displayName(user.getDisplayName())
                        .role(user.getRole())
                        .clockedIn(user.isClockedIn())
                        .build());
    }

    @Transactional
    public PunchResponse punch(PunchRequest request) {
        User user = userRepository.findByRfidTagAndActiveTrue(request.getRfid())
                .orElseThrow(() -> new IllegalArgumentException("Unknown RFID"));

        if (user.isClockedIn()) {
            return clockOut(user, request.getBreakMinutes());
        } else {
            return clockIn(user);
        }
    }

    private PunchResponse clockIn(User user) {
        LocalDateTime now = LocalDateTime.now();

        TimeEntry entry = TimeEntry.builder()
                .user(user)
                .punchIn(now)
                .workDate(now.toLocalDate())
                .build();
        timeEntryRepository.save(entry);

        user.setClockedIn(true);
        userRepository.save(user);

        auditService.log(AuditAction.CLOCK_IN, user.getId(), user.getRfidTag(), null);

        return PunchResponse.builder()
                .displayName(user.getDisplayName())
                .action("CLOCK_IN")
                .timestamp(now)
                .message("Willkommen, " + user.getDisplayName() + "!")
                .build();
    }

    private PunchResponse clockOut(User user, Integer breakMinutes) {
        LocalDateTime now = LocalDateTime.now();

        TimeEntry entry = timeEntryRepository.findByUserAndPunchOutIsNull(user)
                .orElseThrow(() -> new IllegalStateException("No open time entry found"));

        entry.setPunchOut(now);
        entry.setBreakMinutes(breakMinutes != null ? breakMinutes : 0);
        timeEntryRepository.save(entry);

        user.setClockedIn(false);
        userRepository.save(user);

        auditService.log(AuditAction.CLOCK_OUT, user.getId(), user.getRfidTag(),
                "Break: " + entry.getBreakMinutes() + " min");

        return PunchResponse.builder()
                .displayName(user.getDisplayName())
                .action("CLOCK_OUT")
                .timestamp(now)
                .breakMinutes(entry.getBreakMinutes())
                .message("Auf Wiedersehen, " + user.getDisplayName() + "!")
                .build();
    }
}