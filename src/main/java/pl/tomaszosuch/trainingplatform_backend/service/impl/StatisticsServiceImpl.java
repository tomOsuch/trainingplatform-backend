package pl.tomaszosuch.trainingplatform_backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutLogRepository;
import pl.tomaszosuch.trainingplatform_backend.service.StatisticsService;
import pl.tomaszosuch.trainingplatform_backend.service.model.CategoryStatistics;
import pl.tomaszosuch.trainingplatform_backend.service.model.WorkoutStatistics;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {

    private static final String RANGE_REQUIRED_MESSAGE = "Zakres dat jest wymagany";
    private static final String RANGE_ORDER_MESSAGE = "Data początkowa nie może być późniejsza niż końcowa";
    private static final Comparator<CategoryStatistics> MOST_SIGNIFICANT_FIRST =
            Comparator.<CategoryStatistics>comparingLong(CategoryStatistics::minutes).reversed()
                    .thenComparing(Comparator.<CategoryStatistics>comparingLong(CategoryStatistics::sessions).reversed())
                    .thenComparing(CategoryStatistics::categoryName);

    private final WorkoutLogRepository workoutLogRepository;

    @Override
    public WorkoutStatistics workoutStatistics(Long userId, LocalDate from, LocalDate to) {
        validateRange(from, to);

        List<CategoryStatistics> byCategory = workoutLogRepository.aggregateByCategory(userId, from, to).stream()
                .map(row -> new CategoryStatistics(
                        row.getCategoryId(),
                        row.getCategoryName(),
                        row.getCategoryColor(),
                        row.getSessions(),
                        row.getMinutes()))
                .sorted(MOST_SIGNIFICANT_FIRST)
                .toList();

        long totalSessions = byCategory.stream().mapToLong(CategoryStatistics::sessions).sum();
        long totalMinutes = byCategory.stream().mapToLong(CategoryStatistics::minutes).sum();

        return new WorkoutStatistics(totalSessions, totalMinutes, byCategory);
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException(RANGE_REQUIRED_MESSAGE);
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException(RANGE_ORDER_MESSAGE);
        }
    }
}
