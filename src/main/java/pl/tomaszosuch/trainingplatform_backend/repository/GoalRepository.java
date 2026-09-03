package pl.tomaszosuch.trainingplatform_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.tomaszosuch.trainingplatform_backend.entity.Goal;

import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Goal> findByUserIdAndAchievedAtIsNullOrderByCreatedAtDesc(Long userId);

    List<Goal> findByUserIdAndAchievedAtIsNotNullOrderByAchievedAtDesc(Long userId);

    Optional<Goal> findByIdAndUserId(Long id, Long userId);

    boolean existsByCategoryId(Long categoryId);
}
