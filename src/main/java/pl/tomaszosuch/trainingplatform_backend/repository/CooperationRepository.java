package pl.tomaszosuch.trainingplatform_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.tomaszosuch.trainingplatform_backend.entity.Cooperation;
import pl.tomaszosuch.trainingplatform_backend.enums.CooperationStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CooperationRepository extends JpaRepository<Cooperation, Long> {

    List<Cooperation> findByCoachIdAndStatus(Long coachId, CooperationStatus status);

    List<Cooperation> findByAthleteIdAndStatus(Long athleteId, CooperationStatus status);

    boolean existsByCoachIdAndAthleteIdAndStatus(Long coachId, Long athleteId, CooperationStatus status);

    Optional<Cooperation> findByCoachIdAndAthleteIdAndStatusIn(Long coachId, Long athleteId, Collection<CooperationStatus> statuses);
}
