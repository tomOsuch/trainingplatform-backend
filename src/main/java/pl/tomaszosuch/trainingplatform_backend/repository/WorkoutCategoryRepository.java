package pl.tomaszosuch.trainingplatform_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;

@Repository
public interface WorkoutCategoryRepository extends JpaRepository<WorkoutCategory, Long> {
    boolean existsByName(String name);
}
