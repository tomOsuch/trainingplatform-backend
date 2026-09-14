package pl.tomaszosuch.trainingplatform_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutTemplate;

import java.util.List;

public interface WorkoutTemplateRepository extends JpaRepository<WorkoutTemplate, Long> {

    List<WorkoutTemplate> findByUserIdOrderByNameAsc(Long userId);

    boolean existsByCategoryId(Long categoryId);
}
