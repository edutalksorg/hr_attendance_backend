package com.megamart.backend.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue
    private UUID id;

    @JsonProperty("fullName")
    @Column(name = "full_name")
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;

    @Column(name = "employee_id")
    private String employeeId;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // attendance fields
    @Column(name = "last_login")
    private OffsetDateTime lastLogin;

    @Column(name = "last_logout")
    private OffsetDateTime lastLogout;

    @Column(name = "last_ip")
    private String lastIp;

    // for marketing exec only: navigation data reference or last known
    @Column(name = "location_json", columnDefinition = "text")
    private String locationJson;

    @ManyToOne
    @JoinColumn(name = "shift_id")
    private com.megamart.backend.shift.Shift shift;

    // approvals
    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Transient
    private String profilePhoto;

    @Transient
    private String bio;

    @Transient
    private String username; // Mapped from UserProfile or fullName
}
