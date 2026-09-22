package pl.tomaszosuch.trainingplatform_backend.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.tomaszosuch.trainingplatform_backend.entity.Cooperation;
import pl.tomaszosuch.trainingplatform_backend.enums.CooperationStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CooperationRepository extends JpaRepository<Cooperation, Long> {

    @Query("SELECT c FROM Cooperation c JOIN FETCH c.athlete WHERE c.coach.id = :coachId AND c.status = :status")
    List<Cooperation> findByCoachIdAndStatus(@Param("coachId") Long coachId,
                                             @Param("status") CooperationStatus status);

    boolean existsByCoachIdAndAthleteIdAndStatus(Long coachId, Long athleteId, CooperationStatus status);

    long countByCoachIdAndStatusAndExpiresAtAfter(Long coachId, CooperationStatus status, LocalDateTime now);

    Optional<Cooperation> findByCoachIdAndAthleteIdAndStatusIn(Long coachId, Long athleteId, Collection<CooperationStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cooperation c WHERE c.id = :id")
    Optional<Cooperation> lockById(@Param("id") Long id);

    @Query("""
            SELECT c FROM Cooperation c
            JOIN FETCH c.coach
            JOIN FETCH c.athlete
            WHERE (c.coach.id = :userId OR c.athlete.id = :userId)
              AND c.status = :status
            ORDER BY COALESCE(c.respondedAt, c.createdAt) DESC
            """)
    List<Cooperation> findByParticipantAndStatus(@Param("userId") Long userId,
                                                 @Param("status") CooperationStatus status);
}
