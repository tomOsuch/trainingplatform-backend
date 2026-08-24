package pl.tomaszosuch.trainingplatform_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.tomaszosuch.trainingplatform_backend.entity.Invitation;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    Optional<Invitation> findByTokenHash(String tokenHash);

    Optional<Invitation> findByEmailAndUsedAtIsNullAndRevokedAtIsNull(String email);

    @Query("SELECT i FROM Invitation i JOIN FETCH i.invitedBy ORDER BY i.createdAt DESC")
    List<Invitation> findAllWithInvited();
}
