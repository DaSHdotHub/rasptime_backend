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
    @Transient
    public int getRequiredBreakMinutes() {
        long grossMinutes = getGrossDuration().toMinutes();
        if (grossMinutes > 540) return 45;  // >9h
        if (grossMinutes > 360) return 30;  // >6h
        return 0;
    }

    /**
     * Check if break requirement is satisfied
     */
    @Transient
    public boolean isBreakSufficient() {
        return breakMinutes >= getRequiredBreakMinutes();
    }
}