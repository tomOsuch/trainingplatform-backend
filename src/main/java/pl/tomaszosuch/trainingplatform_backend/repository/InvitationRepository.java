package pl.tomaszosuch.trainingplatform_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.tomaszosuch.trainingplatform_backend.entity.Invitation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByTokenHash(String tokenHash);

    Optional<Invitation> findByEmailAndUsedAtIsNullAndRevokedAtIsNull(String email);

    @Query("SELECT i FROM Invitation i LEFT JOIN FETCH i.invitedBy ORDER BY i.createdAt DESC")
    List<Invitation> findAllWithInviter();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
           UPDATE Invitation i SET i.revokedAt = :now
           WHERE i.invitedBy.id = :userId
             AND i.usedAt IS NULL
             AND i.revokedAt IS NULL
           """)
    int revokePendingByInviter(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
