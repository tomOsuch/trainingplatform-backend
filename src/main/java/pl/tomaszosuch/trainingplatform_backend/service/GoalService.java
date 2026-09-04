package pl.tomaszosuch.trainingplatform_backend.service;

import pl.tomaszosuch.trainingplatform_backend.dto.request.GoalRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.GoalResponse;
import pl.tomaszosuch.trainingplatform_backend.enums.GoalStatus;

import java.nio.file.AccessDeniedException;
import java.util.List;

public interface GoalService {

    List<GoalResponse> getGoals(Long userId, GoalStatus status);

    GoalResponse createGoal(Long userId, GoalRequest request);

    GoalResponse updateGoal(Long userId, Long goalId, GoalRequest request) throws AccessDeniedException;

    void deleteGoal(Long userId, Long goalId) throws AccessDeniedException;
}
