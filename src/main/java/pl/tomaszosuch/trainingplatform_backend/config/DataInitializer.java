package pl.tomaszosuch.trainingplatform_backend.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.enums.CategoryIcon;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutCategoryRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final WorkoutCategoryRepository workoutCategoryRepository;

    @Override
    public void run(String... args) throws Exception {
        if (workoutCategoryRepository.count() == 0) {
            log.info("Inicjalizacja domyślnych kategorii treningów...");

            List<WorkoutCategory> defaults = List.of(
                    WorkoutCategory.builder()
                            .name("Taniec").color("#9B59B6").iconName(CategoryIcon.MUSIC.value()).build(),
                    WorkoutCategory.builder()
                            .name("Gimnastyka").color("#E74C3C").iconName(CategoryIcon.PERSON_STANDING.value()).build(),
                    WorkoutCategory.builder()
                            .name("Ogólnorozwojowy").color("#27AE60").iconName(CategoryIcon.DUMBBELL.value()).build()
            );

            workoutCategoryRepository.saveAll(defaults);
            log.info("Dodano {} domyślnych kategorii", defaults.size());
        }
    }
}
