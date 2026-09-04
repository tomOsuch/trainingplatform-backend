package pl.tomaszosuch.trainingplatform_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.tomaszosuch.trainingplatform_backend.entity.Goal;

import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Goal> findByUserIdAndAchievedAtIsNullOrderByCreatedAtDesc(Long userId);

    List<Goal> findByUserIdAndAchievedAtIsNotNullOrderByAchievedAtDesc(Long userId);

    Optional<Goal> findByIdAndUserId(Long id, Long userId);

    @Query("""
            SELECT g.id AS goalId,
                   COUNT(l.id) AS sessions,
                   COALESCE(SUM(l.durationMin), 0L) AS minutes
            FROM Goal g
            LEFT JOIN WorkoutLog l
                ON l.user.id = g.user.id
               AND l.performedDate >= g.startDate
               AND (g.endDate IS NULL OR l.performedDate <= g.endDate)
               AND (g.category.id IS NULL OR l.category.id = g.category.id)
            WHERE g.id = :goalId
            GROUP BY g.id
            """)
    Optional<GoalProgressView> findProgressByGoalId(@Param("goalId") Long goalId);

    @Query("""
            SELECT g.id AS goalId,
                   COUNT(l.id) AS sessions,
                   COALESCE(SUM(l.durationMin), 0L) AS minutes
            FROM Goal g
            LEFT JOIN WorkoutLog l
                ON l.user.id = g.user.id
               AND l.performedDate >= g.startDate
               AND (g.endDate IS NULL OR l.performedDate <= g.endDate)
               AND (g.category.id IS NULL OR l.category.id = g.category.id)
            WHERE g.user.id = :userId
              AND g.achievedAt IS NULL
            GROUP BY g.id
            """)
    List<GoalProgressView> findActiveProgressByUserId(@Param("userId") Long userId);

    boolean existsByCategoryId(Long categoryId);
}
