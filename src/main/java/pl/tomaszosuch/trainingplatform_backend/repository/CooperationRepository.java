package pl.tomaszosuch.trainingplatform_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.tomaszosuch.trainingplatform_backend.entity.Cooperation;
import pl.tomaszosuch.trainingplatform_backend.enums.CooperationStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CooperationRepository extends JpaRepository<Cooperation, Long> {

    @Query("SELECT c FROM Cooperation c JOIN FETCH c.athlete WHERE c.coach.id = :coachId AND c.status = :status")
    List<Cooperation> findByCoachIdAndStatus(@Param("coachId") Long coachId,
                                             @Param("status") CooperationStatus status);

    @Query("SELECT c FROM Cooperation c JOIN FETCH c.coach WHERE c.athlete.id = :athleteId AND c.status = :status")
    List<Cooperation> findByAthleteIdAndStatus(@Param("athleteId") Long athleteId,
                                               @Param("status") CooperationStatus status);

    boolean existsByCoachIdAndAthleteIdAndStatus(Long coachId, Long athleteId, CooperationStatus status);

    Optional<Cooperation> findByCoachIdAndAthleteIdAndStatusIn(Long coachId, Long athleteId, Collection<CooperationStatus> statuses);
}
