package pl.tomaszosuch.trainingplatform_backend.service;

import pl.tomaszosuch.trainingplatform_backend.service.model.GoalProgress;
import pl.tomaszosuch.trainingplatform_backend.entity.Goal;

import java.util.Collection;
import java.util.Map;

public interface GoalProgressService {

    GoalProgress progressOf(Goal goal);

    Map<Long, GoalProgress> progressOf(Long userId, Collection<Goal> goals);
}
