package io.github.dashdothub.rasptime_backend.service;

import io.github.dashdothub.rasptime_backend.entity.AuditAction;
import io.github.dashdothub.rasptime_backend.entity.TimeEntry;
import io.github.dashdothub.rasptime_backend.entity.User;
import io.github.dashdothub.rasptime_backend.repository.TimeEntryRepository;
import io.github.dashdothub.rasptime_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClockOutScheduler {

    private final UserRepository userRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final AuditService auditService;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.scheduler.clockout.zone:UTC}")
    private String clockOutZone;

    @Scheduled(
            cron = "${app.scheduler.clockout.cron:0 59 23 * * *}",
            zone = "${app.scheduler.clockout.zone:UTC}"
    )
    public void autoClockOutAll() {
        runClockOutJob("scheduled");
    }

    public ClockOutRunResult runNow() {
        return runClockOutJob("manual");
    }

    private ClockOutRunResult runClockOutJob(String trigger) {
        ZoneId zoneId = ZoneId.of(clockOutZone);
        log.info("Running auto clock-out job (trigger: {}, zone: {}, now: {})", trigger, clockOutZone, LocalDateTime.now(zoneId));

        List<User> clockedInUsers = userRepository.findAllByClockedInTrue();

        if (clockedInUsers.isEmpty()) {
            log.info("No users to clock out");
            return new ClockOutRunResult(trigger, 0, 0, 0);
        }

        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(zoneId), LocalTime.of(23, 59, 59));
        int successCount = 0;
        int failureCount = 0;

        for (User user : clockedInUsers) {
            try {
                transactionTemplate.executeWithoutResult(status -> autoClockOutUser(user, endOfDay));
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("Failed to auto clock-out user {}", user.getId(), e);
            }
        }

        log.info("Auto clock-out completed: total={}, success={}, failed={}", clockedInUsers.size(), successCount, failureCount);
        return new ClockOutRunResult(trigger, clockedInUsers.size(), successCount, failureCount);
    }

    private void autoClockOutUser(User user, LocalDateTime clockOutTime) {
        TimeEntry entry = timeEntryRepository.findByUserAndPunchOutIsNull(user)
                .orElse(null);

        if (entry == null) {
            log.warn("User {} marked as clocked in but no open time entry found", user.getId());
            user.setClockedIn(false);
            userRepository.save(user);
            return;
        }

        // Calculate required break based on gross hours
        long grossMinutes = java.time.Duration.between(entry.getPunchIn(), clockOutTime).toMinutes();
        int requiredBreak = calculateRequiredBreak(grossMinutes);

        entry.setPunchOut(clockOutTime);
        entry.setAutoClosedOut(true);
        entry.setBreakMinutes(requiredBreak);
        timeEntryRepository.save(entry);

        user.setClockedIn(false);
        userRepository.save(user);

        auditService.log(
                AuditAction.CLOCK_OUT_AUTO,
                user.getId(),
                user.getRfidTag(),
                String.format("Auto clock-out at 23:59. Gross: %d min, Break: %d min", grossMinutes, requiredBreak)
        );

        log.info("Auto clocked out user: {} ({})", user.getDisplayName(), user.getRfidTag());
    }

    private int calculateRequiredBreak(long grossMinutes) {
        if (grossMinutes > 540) return 45;  // >9h
        if (grossMinutes > 360) return 30;  // >6h
        return 0;
    }

    public record ClockOutRunResult(String trigger, int totalUsers, int successfulUsers, int failedUsers) {}
}
