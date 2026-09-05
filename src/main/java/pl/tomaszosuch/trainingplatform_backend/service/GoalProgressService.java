package pl.tomaszosuch.trainingplatform_backend.service;

import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutLog;
import pl.tomaszosuch.trainingplatform_backend.service.model.GoalProgress;
import pl.tomaszosuch.trainingplatform_backend.entity.Goal;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface GoalProgressService {

    GoalProgress progressOf(Goal goal);

    GoalProgress progressOf(Goal goal, List<WorkoutLog> matchingLogs);

    Map<Long, GoalProgress> progressOf(Long userId, Collection<Goal> goals);

    List<WorkoutLog> matchingLogs(Goal goal);
}
