package io.github.dashdothub.rasptime_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "time_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime punchIn;

    private LocalDateTime punchOut;

    @Column(nullable = false)
    private LocalDate workDate;

    @Builder.Default
    private Integer breakMinutes = 0;

    @Builder.Default
    private boolean autoClosedOut = false;

    @PrePersist
    protected void onCreate() {
        if (workDate == null) {
            workDate = punchIn.toLocalDate();
        }
    }

    /**
     * Calculate gross work duration (without break deducted)
     */
    @Transient
    public Duration getGrossDuration() {
        if (punchOut == null) return Duration.ZERO;
        return Duration.between(punchIn, punchOut);
    }

    /**
     * Calculate net work duration (break deducted)
     */
    @Transient
    public Duration getNetDuration() {
        if (punchOut == null) return Duration.ZERO;
        return getGrossDuration().minusMinutes(breakMinutes);
    }

    /**
     * Minimum required break per German Arbeitszeitgesetz
     */
    /**
     * Calculate required break based on German labor law (Arbeitszeitgesetz)
     * - Over 6h: graduated break up to 30min
     * - Over 9h: graduated break up to 45min
     *
     * The break is graduated so:
     * - 6:00 = 0 min, 6:15 = 15 min, 6:30+ = 30 min
     * - 9:00 = 30 min, 9:15 = 45 min, 9:15+ = 45 min
     */
    @Transient
    public int getRequiredBreakMinutes() {
        long grossMinutes = getGrossDuration().toMinutes();

        if (grossMinutes <= 360) {
            // 6h or less: no break required
            return 0;
        } else if (grossMinutes <= 390) {
            // 6h01m to 6h30m: graduated break (1-30 min)
            return (int) (grossMinutes - 360);
        } else if (grossMinutes <= 540) {
            // 6h31m to 9h: 30min break required
            return 30;
        } else if (grossMinutes <= 555) {
            // 9h01m to 9h15m: graduated break (31-45 min)
            return (int) (30 + (grossMinutes - 540));
        } else {
            // Over 9h15m: 45min break required
            return 45;
        }
    }

    /**
     * Check if break requirement is satisfied
     */
    @Transient
    public boolean isBreakSufficient() {
        return breakMinutes >= getRequiredBreakMinutes();
    }
}