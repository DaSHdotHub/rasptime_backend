package io.github.dashdothub.rasptime_backend.service;

import io.github.dashdothub.rasptime_backend.entity.AuditAction;
import io.github.dashdothub.rasptime_backend.entity.TimeEntry;
import io.github.dashdothub.rasptime_backend.entity.User;
import io.github.dashdothub.rasptime_backend.repository.TimeEntryRepository;
import io.github.dashdothub.rasptime_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClockOutScheduler {

    private final UserRepository userRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final AuditService auditService;

    @Scheduled(cron = "0 59 23 * * *")  // 23:59 every day
    @Transactional
    public void autoClockOut() {
        log.info("Running auto clock-out job");

        List<User> clockedInUsers = userRepository.findAllByClockedInTrue();

        for (User user : clockedInUsers) {
            try {
                TimeEntry entry = timeEntryRepository
                        .findByUserAndPunchOutIsNull(user)
                        .orElse(null);

                if (entry != null) {
                    LocalDateTime closeTime = LocalDateTime.now()
                            .withHour(23).withMinute(59).withSecond(59);
                    entry.setPunchOut(closeTime);
                    entry.setAutoClosedOut(true);

                    // Calculate required break based on gross duration
                    int requiredBreak = entry.getRequiredBreakMinutes();
                    entry.setBreakMinutes(requiredBreak);

                    timeEntryRepository.save(entry);

                    auditService.log(AuditAction.CLOCK_OUT_AUTO, user.getId(), null,
                            String.format("Auto clock-out at 23:59 - %d min break applied",
                                    requiredBreak));
                }

                user.setClockedIn(false);
                userRepository.save(user);

            } catch (Exception e) {
                log.error("Error auto-clocking out user " + user.getId() + ": " + e.getMessage());
            }
        }

        log.info("Auto clock-out completed for " + clockedInUsers.size() + " users");
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
}