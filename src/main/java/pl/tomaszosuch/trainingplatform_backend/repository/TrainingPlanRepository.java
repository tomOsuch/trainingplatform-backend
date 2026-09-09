package pl.tomaszosuch.trainingplatform_backend.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import pl.tomaszosuch.trainingplatform_backend.entity.TrainingPlan;

@Repository
public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, Long> {

    List<TrainingPlan> findByUserIdOrderByPlannedDateAsc(Long userId);

    List<TrainingPlan> findByUserIdAndPlannedDateBetweenOrderByPlannedDateAsc(Long userId, LocalDate startDate, LocalDate endDate);

    boolean existsByCategoryId(Long categoryId);

    @Query("""
            SELECT p.status AS status,
                   COUNT(p.id) AS count
            FROM TrainingPlan p
            WHERE p.user.id = :userId
              AND p.plannedDate >= :from
              AND p.plannedDate <= :to
              AND (p.status <> pl.tomaszosuch.trainingplatform_backend.enums.PlanStatus.PLANNED
                   OR p.plannedDate < :today)
            GROUP BY p.status
            """)
    List<PlanStatusCountView> countByStatus(@Param("userId") Long userId,
                                            @Param("from") LocalDate from,
                                            @Param("to") LocalDate to,
                                            @Param("today") LocalDate today);

    @Query("""
            SELECT p FROM TrainingPlan p
            JOIN FETCH p.user u
            JOIN FETCH p.category
            WHERE p.status = pl.tomaszosuch.trainingplatform_backend.enums.PlanStatus.PLANNED
              AND p.reminderSentAt IS NULL
              AND u.remindersEnabled = true
              AND p.plannedDate >= :fromDate
            ORDER BY p.plannedDate ASC
            """)
    List<TrainingPlan> findReminderCandidates(@Param("fromDate") LocalDate fromDate);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE TrainingPlan p SET p.reminderSentAt = :sentAt WHERE p.id = :id AND p.reminderSentAt IS NULL")
    int markReminderSent(@Param("id") Long id, @Param("sentAt") LocalDateTime sentAt);
}
