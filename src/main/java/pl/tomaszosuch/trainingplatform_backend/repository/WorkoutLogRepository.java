package pl.tomaszosuch.trainingplatform_backend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutLog;

@Repository
public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, Long>, JpaSpecificationExecutor<WorkoutLog> {

    List<WorkoutLog> findByUserIdOrderByPerformedDateDesc(Long userId);

    List<WorkoutLog> findByUserIdAndPerformedDateBetweenOrderByPerformedDateDesc(Long userId, LocalDate from,
                                                                                 LocalDate to);

    List<WorkoutLog> findByUserIdAndCategoryIdOrderByPerformedDateDesc(Long userId, Long categoryId);

    boolean existsByCategoryId(Long categoryId);

    @Modifying(flushAutomatically = true)
    @Query("UPDATE WorkoutLog l SET l.plan = null WHERE l.plan.id = :planId")
    int detachLogsFromPlan(@Param("planId") Long planId);

    @Query("""
            SELECT c.id AS categoryId,
                   c.name AS categoryName,
                   c.color AS categoryColor,
                   COUNT(l.id) AS sessions,
                   COALESCE(SUM(l.durationMin), 0L) AS minutes
            FROM WorkoutLog l
            JOIN l.category c
            WHERE l.user.id = :userId
              AND l.performedDate >= :from
              AND l.performedDate <= :to
            GROUP BY c.id, c.name, c.color
            """)
    List<CategoryStatsView> aggregateByCategory(@Param("userId") Long userId,
                                                @Param("from") LocalDate from,
                                                @Param("to") LocalDate to);

}
