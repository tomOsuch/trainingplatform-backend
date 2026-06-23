package pl.tomaszosuch.trainingplatform_backend.service;

import java.time.LocalDate;
import java.util.List;

import pl.tomaszosuch.trainingplatform_backend.dto.request.StatusUpdateRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.TrainingPlanRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.TrainingPlanResponse;

public interface TrainingPlanService {

    List<TrainingPlanResponse> getTrainingPlansByUserId(Long userId, LocalDate startDate, LocalDate endDate);

    TrainingPlanResponse getTrainingPlanById(Long planId, Long userId);

    TrainingPlanResponse createTrainingPlan(Long userId, TrainingPlanRequest request);

    TrainingPlanResponse updateTrainingPlan(Long userId, Long id, TrainingPlanRequest request);

    TrainingPlanResponse changeStatus(Long userId, Long trainingPlanId, StatusUpdateRequest request);

    void deleteTrainingPlan(Long userId, Long trainingPlanId);

}
