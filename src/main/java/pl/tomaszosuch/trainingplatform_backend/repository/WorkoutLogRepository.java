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

}
