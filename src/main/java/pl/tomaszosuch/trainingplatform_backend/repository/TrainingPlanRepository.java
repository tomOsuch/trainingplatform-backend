package pl.tomaszosuch.trainingplatform_backend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pl.tomaszosuch.trainingplatform_backend.entity.TrainingPlan;

@Repository
public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, Long> {

    List<TrainingPlan> findByUserIdOrderByPlannedDateAsc(Long userId);

    List<TrainingPlan> findByUserIdAndPlannedDateBetweenOrderByPlannedDateAsc(Long userId, LocalDate startDate, LocalDate endDate);

    boolean existsByCategoryId(Long categoryId);

}
