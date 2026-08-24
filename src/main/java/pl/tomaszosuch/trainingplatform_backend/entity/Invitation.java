package pl.tomaszosuch.trainingplatform_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import pl.tomaszosuch.trainingplatform_backend.enums.InvitationStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "invitation")
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Transient
    public InvitationStatus getStatus() {
        if (usedAt != null) {
            return InvitationStatus.ACCEPTED;
        }
        if (revokedAt != null) {
            return InvitationStatus.REVOKED;
        }
        if (expiresAt.isBefore(LocalDateTime.now())) {
            return InvitationStatus.EXPIRED;
        }
        return InvitationStatus.PENDING;
    }

    @Transient
    public boolean isUsable() {
        return getStatus() == InvitationStatus.PENDING;
    }
}
