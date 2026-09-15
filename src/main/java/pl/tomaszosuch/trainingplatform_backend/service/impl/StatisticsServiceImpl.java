package pl.tomaszosuch.trainingplatform_backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.tomaszosuch.trainingplatform_backend.dto.response.StatisticsResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.WeeklyStatisticsResponse;
import pl.tomaszosuch.trainingplatform_backend.enums.PlanStatus;
import pl.tomaszosuch.trainingplatform_backend.mapper.StatisticsMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.*;
import pl.tomaszosuch.trainingplatform_backend.service.StatisticsService;
import pl.tomaszosuch.trainingplatform_backend.service.model.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {

    private static final String RANGE_REQUIRED_MESSAGE = "Zakres dat jest wymagany";
    private static final String RANGE_ORDER_MESSAGE = "Data początkowa nie może być późniejsza niż końcowa";
    private static final long[] EMPTY_WEEK = new long[2];
    private static final Comparator<CategoryStatistics> MOST_SIGNIFICANT_FIRST =
            Comparator.<CategoryStatistics>comparingLong(CategoryStatistics::minutes).reversed()
                    .thenComparing(Comparator.<CategoryStatistics>comparingLong(CategoryStatistics::sessions).reversed())
                    .thenComparing(CategoryStatistics::categoryName);

    private final WorkoutLogRepository workoutLogRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final StatisticsMapper statisticsMapper;

    @Override
    public WorkoutStatistics workoutStatistics(Long userId, LocalDate from, LocalDate to) {
        validateRange(from, to);

        List<CategoryStatistics> byCategory = workoutLogRepository.aggregateByCategory(userId, from, to).stream()
                .map(row -> new CategoryStatistics(
                        row.getCategoryId(),
                        row.getCategoryName(),
                        row.getCategoryColor(),
                        row.getCategoryIconName(),
                        row.getSessions(),
                        row.getMinutes()))
                .sorted(MOST_SIGNIFICANT_FIRST)
                .toList();

        long totalSessions = byCategory.stream().mapToLong(CategoryStatistics::sessions).sum();
        long totalMinutes = byCategory.stream().mapToLong(CategoryStatistics::minutes).sum();

        return new WorkoutStatistics(totalSessions, totalMinutes, byCategory);
    }

    @Override
    public PlanCompletion planCompletion(Long userId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        Map<PlanStatus, Long> counts = trainingPlanRepository
                .countByStatus(userId, from, to, LocalDate.now()).stream()
                .collect(Collectors.toMap(PlanStatusCountView::getStatus, PlanStatusCountView::getCount));
        return new PlanCompletion(
                counts.getOrDefault(PlanStatus.COMPLETED, 0L),
                counts.getOrDefault(PlanStatus.SKIPPED, 0L),
                counts.getOrDefault(PlanStatus.CANCELLED, 0L),
                counts.getOrDefault(PlanStatus.PLANNED, 0L));
    }

    @Override
    public IntensitySummary intensitySummary(Long userId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        IntensityStatsView row = workoutLogRepository.aggregateIntensity(userId, from, to);
        return new IntensitySummary(row.getTotalCount(), row.getRatedCount(), row.getIntensitySum());
    }

    @Override
    public StatisticsResponse statistics(Long userId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        return statisticsMapper.toResponse(from, to,
                workoutStatistics(userId, from, to),
                planCompletion(userId, from, to),
                intensitySummary(userId, from, to));
    }

    @Override
    public WeeklyStatisticsResponse weeklyStatistics(Long userId, LocalDate from, LocalDate to) {
        validateRange(from, to);

        Map<LocalDate, long[]> byWeek = new HashMap<>();
        for (DailyStatsView row : workoutLogRepository.aggregateByDay(userId, from, to)) {
            long[] bucket = byWeek.computeIfAbsent(mondayOf(row.getDay()), key -> new long[2]);
            bucket[0] += row.getSessions();
            bucket[1] += row.getMinutes();
        }

        List<WeeklyPoint> weeks = new ArrayList<>();
        LocalDate lastWeekStart = mondayOf(to);
        for (LocalDate weekStart = mondayOf(from); !weekStart.isAfter(lastWeekStart); weekStart = weekStart.plusWeeks(1)) {
            LocalDate weekEnd = weekStart.plusDays(6);
            long[] bucket = byWeek.getOrDefault(weekStart, EMPTY_WEEK);
            weeks.add(new WeeklyPoint(
                    weekStart,
                    weekEnd,
                    weekStart.isBefore(from) || weekEnd.isAfter(to),
                    bucket[0],
                    bucket[1]));
        }

        long totalSessions = weeks.stream().mapToLong(WeeklyPoint::sessions).sum();
        long totalMinutes = weeks.stream().mapToLong(WeeklyPoint::minutes).sum();

        return statisticsMapper.toWeeklyResponse(from, to,
                new WeeklyStatistics(totalSessions, totalMinutes, weeks));
    }

    private static LocalDate mondayOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
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
