package io.github.dashdothub.rasptime_backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private boolean autoClosedOut = false;

    @PrePersist
    protected void onCreate() {
        if (workDate == null) {
            workDate = punchIn.toLocalDate();
        }
    }
}