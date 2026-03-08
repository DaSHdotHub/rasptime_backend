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

import java.time.LocalDate;
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

    public PunchResponse punch(PunchRequest request) {
        User user = userRepository.findByRfidTagAndActiveTrue(request.getRfid())
                .orElse(null);

        if (user == null) {
            auditService.log(AuditAction.UNKNOWN_RFID, null, request.getRfid(),
                    "Unknown RFID tag attempted");
            return null;
        }

        if (user.isClockedIn()) {
            // Clock OUT
            TimeEntry entry = timeEntryRepository
                    .findByUserAndPunchOutIsNull(user)
                    .orElse(null);

            if (entry == null) {
                user.setClockedIn(false);
                userRepository.save(user);
                return PunchResponse.builder()
                        .action("CLOCK_OUT")
                        .message("Ausgestempelt (kein offener Eintrag)")
                        .displayName(user.getDisplayName())
                        .timestamp(LocalDateTime.now())
                        .breakMinutes(0)
                        .build();
            }

            entry.setPunchOut(LocalDateTime.now());

            // Calculate required break based on gross duration
            int requiredBreak = entry.getRequiredBreakMinutes();

            // Use provided break minutes if given, otherwise use required minimum
            int actualBreak = request.getBreakMinutes() != null
                    ? request.getBreakMinutes()
                    : requiredBreak;

            // Ensure break is at least the required minimum
            if (actualBreak < requiredBreak) {
                actualBreak = requiredBreak;
            }

            entry.setBreakMinutes(actualBreak);
            timeEntryRepository.save(entry);

            user.setClockedIn(false);
            userRepository.save(user);

            long netMinutes = entry.getNetDuration().toMinutes();
            String message = String.format("Ausgestempelt - %d:%02d Netto (%d min Pause)",
                    netMinutes / 60, netMinutes % 60, actualBreak);

            auditService.log(AuditAction.CLOCK_OUT, user.getId(), request.getRfid(), message);

            return PunchResponse.builder()
                    .action("CLOCK_OUT")
                    .message(message)
                    .displayName(user.getDisplayName())
                    .timestamp(entry.getPunchOut())
                    .breakMinutes(actualBreak)
                    .build();
        } else {
            // Clock IN
            TimeEntry entry = TimeEntry.builder()
                    .user(user)
                    .punchIn(LocalDateTime.now())
                    .workDate(LocalDate.now())
                    .breakMinutes(0)
                    .autoClosedOut(false)
                    .build();
            timeEntryRepository.save(entry);

            user.setClockedIn(true);
            userRepository.save(user);

            auditService.log(AuditAction.CLOCK_IN, user.getId(), request.getRfid(),
                    "Eingestempelt");

            return PunchResponse.builder()
                    .action("CLOCK_IN")
                    .message("Eingestempelt")
                    .displayName(user.getDisplayName())
                    .timestamp(entry.getPunchIn())
                    .breakMinutes(0)
                    .build();
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
                .message("Auf Wiedersehen, " + user.getDisplayName() + "!")
                .build();
    }
}