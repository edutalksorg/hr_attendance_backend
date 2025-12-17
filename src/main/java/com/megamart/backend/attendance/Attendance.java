package com.megamart.backend.attendance;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "login_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "login_time")
    private OffsetDateTime loginTime;

    @Column(name = "logout_time")
    private OffsetDateTime logoutTime;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String metadata;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "logout_ip_address")
    private String logoutIpAddress;

    @Builder.Default
    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public boolean getCanCheckOut() {
        if (logoutTime != null) {
            return false;
        }
        if (loginTime == null) {
            return false;
        }
        return java.time.Duration.between(loginTime, OffsetDateTime.now()).toHours() < 10;
    }
}
