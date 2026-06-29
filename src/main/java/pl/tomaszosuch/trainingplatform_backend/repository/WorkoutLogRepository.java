package pl.tomaszosuch.trainingplatform_backend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutLog;

@Repository
public interface WorkoutLogRepository extends JpaRepository<WorkoutLog, Long> {

    List<WorkoutLog> findByUserIdOrderByPerformedDateDesc(Long userId);

    List<WorkoutLog> findByUserIdAndPerformedDateBetweenOrderByPerformedDateDesc(Long userId, LocalDate from, LocalDate to);

    List<WorkoutLog> findByUserIdAndCategoryIdOrderByPerformedDateDesc(Long userId, Long categoryId);

    boolean existsByCategoryId(Long categoryId);

}
