package pl.tomaszosuch.trainingplatform_backend.service;

import pl.tomaszosuch.trainingplatform_backend.dto.request.GoalRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.GoalStatusUpdateRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.GoalDetailsResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.GoalResponse;
import pl.tomaszosuch.trainingplatform_backend.enums.GoalStatus;

import java.nio.file.AccessDeniedException;
import java.util.List;

public interface GoalService {

    List<GoalResponse> getGoals(Long userId, GoalStatus status);

    GoalDetailsResponse getGoal(Long userId, Long goalId);

    GoalResponse createGoal(Long userId, GoalRequest request);

    GoalResponse updateGoal(Long userId, Long goalId, GoalRequest request);

    GoalResponse changeStatus(Long userId, Long goalId, GoalStatusUpdateRequest request);

    void deleteGoal(Long userId, Long goalId);
}
