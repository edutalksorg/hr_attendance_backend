package com.megamart.backend.shift;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shift {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    // Optional: Grace period in minutes
    @Builder.Default
    @Column(name = "late_grace_minutes")
    private Integer lateGraceMinutes = 15;

    @Column(name = "half_day_time")
    private LocalTime halfDayTime;

    @Column(name = "absent_time")
    private LocalTime absentTime;

    @Builder.Default
    @Column(name = "late_count_limit")
    private Integer lateCountLimit = 3;
}
