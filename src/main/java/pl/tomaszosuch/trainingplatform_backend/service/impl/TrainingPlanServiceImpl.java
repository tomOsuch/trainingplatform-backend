package pl.tomaszosuch.trainingplatform_backend.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pl.tomaszosuch.trainingplatform_backend.dto.request.StatusUpdateRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.TrainingPlanRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.TrainingPlanResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.TrainingPlan;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.exception.TrainingPlanNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.exception.UserNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.exception.WorkoutCategoryNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.mapper.TrainingPlanMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.TrainingPlanRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutCategoryRepository;
import pl.tomaszosuch.trainingplatform_backend.service.TrainingPlanService;

@Service
@RequiredArgsConstructor
public class TrainingPlanServiceImpl implements TrainingPlanService {

    private final TrainingPlanRepository trainingPlanRepository;
    private final TrainingPlanMapper trainingPlanMapper;
    private final UserRepository userRepository;
    private final WorkoutCategoryRepository workoutCategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TrainingPlanResponse> getTrainingPlansByUserId(Long userId, LocalDate startDate, LocalDate endDate) {
        
        List<TrainingPlan> plans;
        if (startDate != null && endDate != null) {
            plans = trainingPlanRepository.findByUserIdAndPlannedDateBetweenOrderByPlannedDateAsc(userId, startDate, endDate);
        } else {
            plans = trainingPlanRepository.findByUserIdOrderByPlannedDateAsc(userId);
        }
        if (plans.isEmpty()) {
            return null;
        } else {
            return plans.stream()
                .map(trainingPlanMapper::toResponse)
                .toList();
        }
    }

    @Override
    public TrainingPlanResponse getTrainingPlanById(Long planId, Long userId) {

        TrainingPlan plan = trainingPlanRepository.findById(planId)
            .orElseThrow(() -> new TrainingPlanNotFoundException("Training plan with id " + planId + " not found."));

        if (!plan.getUser().getId().equals(userId)) {
            throw new AccessDeniedException(
                "Brak uprawnień do tego planu treningowego");
        }

        return trainingPlanMapper.toResponse(plan);
    }

    @Override
    public TrainingPlanResponse createTrainingPlan(Long userId, TrainingPlanRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        WorkoutCategory category = workoutCategoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new WorkoutCategoryNotFoundException(request.categoryId()));

        TrainingPlan plan = TrainingPlan.builder()
            .user(user)
            .category(category)
            .title(request.title())
            .plannedDate(request.plannedDate())
            .plannedTime(request.plannedTime())
            .durationMin(request.durationMin())
            .notes(request.notes())
            .build();

        return trainingPlanMapper.toResponse(trainingPlanRepository.save(plan));
    }

    @Override
    public TrainingPlanResponse updateTrainingPlan(Long userId, Long id, TrainingPlanRequest request) {
         TrainingPlan plan = trainingPlanRepository.findById(id)
            .orElseThrow(() -> new TrainingPlanNotFoundException("Training plan with id " + id + " not found."));

        if (!plan.getUser().getId().equals(userId)) {
            throw new AccessDeniedException(
                "Brak uprawnień do tego planu treningowego");
        }

        WorkoutCategory category = workoutCategoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new WorkoutCategoryNotFoundException(request.categoryId()));

        plan.setCategory(category);
        plan.setTitle(request.title());
        plan.setPlannedDate(request.plannedDate());
        plan.setPlannedTime(request.plannedTime());
        plan.setDurationMin(request.durationMin());
        plan.setNotes(request.notes());

        return trainingPlanMapper.toResponse(trainingPlanRepository.save(plan));
    }

    @Override
    public TrainingPlanResponse changeStatus(Long userId, Long trainingPlanId, StatusUpdateRequest request) {
        TrainingPlan plan = trainingPlanRepository.findById(trainingPlanId)
            .orElseThrow(() -> new TrainingPlanNotFoundException("Training plan with id " + trainingPlanId + " not found."));

        if (!plan.getUser().getId().equals(userId)) {
            throw new AccessDeniedException(
                "Brak uprawnień do tego planu treningowego");
        }

        plan.setStatus(request.status());
        return trainingPlanMapper.toResponse(trainingPlanRepository.save(plan));
    }

    @Override
    public void deleteTrainingPlan(Long userId, Long trainingPlanId) {
        TrainingPlan plan = trainingPlanRepository.findById(trainingPlanId)
            .orElseThrow(() -> new TrainingPlanNotFoundException("Training plan with id " + trainingPlanId + " not found."));
        if (!plan.getUser().getId().equals(userId)) {
            throw new AccessDeniedException(
                "Brak uprawnień do tego planu treningowego");
        }
        trainingPlanRepository.delete(plan);
    }

}
