package pl.tomaszosuch.trainingplatform_backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutLog;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutLogRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.specification.WorkoutLogSpecifications;
import pl.tomaszosuch.trainingplatform_backend.service.model.GoalProgress;
import pl.tomaszosuch.trainingplatform_backend.entity.Goal;
import pl.tomaszosuch.trainingplatform_backend.enums.GoalMetric;
import pl.tomaszosuch.trainingplatform_backend.repository.GoalProgressView;
import pl.tomaszosuch.trainingplatform_backend.repository.GoalRepository;
import pl.tomaszosuch.trainingplatform_backend.service.GoalProgressService;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoalProgressServiceImpl implements GoalProgressService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("performedDate"), Sort.Order.desc("id"));

    private final GoalRepository goalRepository;
    private final WorkoutLogRepository workoutLogRepository;

    @Override
    public GoalProgress progressOf(Goal goal) {
        if (goal.isAchieved()) {
            return snapshot(goal);
        }
        return progressOf(goal, matchingLogs(goal));
    }

    @Override
    public GoalProgress progressOf(Goal goal, List<WorkoutLog> matchingLogs) {
        if (goal.isAchieved()) {
            return snapshot(goal);
        }
        long current = switch (goal.getMetric()) {
            case SESSIONS -> matchingLogs.size();
            case MINUTES -> matchingLogs.stream()
                    .map(WorkoutLog::getDurationMin)
                    .filter(Objects::nonNull)
                    .mapToLong(Integer::longValue)
                    .sum();

        };
        return new GoalProgress(current, goal.getTargetValue());
    }

    @Override
    public Map<Long, GoalProgress> progressOf(Long userId, Collection<Goal> goals) {

        Map<Long, GoalProgress> result = new HashMap<>();
        boolean anyActive = goals.stream().anyMatch(goal -> !goal.isAchieved());

        Map<Long, GoalProgressView> rows = anyActive
                ? goalRepository.findActiveProgressByUserId(userId).stream()
                .collect(Collectors.toMap(GoalProgressView::getGoalId, Function.identity()))
                : Map.of();
        for (Goal goal : goals) {
            if (goal.isAchieved()) {
                result.put(goal.getId(), snapshot(goal));
                continue;
            }
            GoalProgressView row = rows.get(goal.getId());
            long current = row == null ? 0L : valueFor(goal.getMetric(), row);
            result.put(goal.getId(), new GoalProgress(current, goal.getTargetValue()));
        }
        return result;
    }

    @Override
    public List<WorkoutLog> matchingLogs(Goal goal) {
        return workoutLogRepository.findAll(WorkoutLogSpecifications.matchingGoal(goal), NEWEST_FIRST);
    }

    private static GoalProgress snapshot(Goal goal) {
        return new GoalProgress(goal.getAchievedValue(), goal.getTargetValue());
    }

    private static long valueFor(GoalMetric metric, GoalProgressView row) {
        return switch (metric) {
            case SESSIONS -> row.getSessions();
            case MINUTES -> row.getMinutes();
        };
    }
}
