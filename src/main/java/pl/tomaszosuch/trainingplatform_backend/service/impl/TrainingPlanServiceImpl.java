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
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutLogRepository;
import pl.tomaszosuch.trainingplatform_backend.service.TrainingPlanService;

@Service
@RequiredArgsConstructor
@Transactional
public class TrainingPlanServiceImpl implements TrainingPlanService {

    private static final String PAST_DATE_MESSAGE = "Data treningu nie może być z przeszłości";

    private final TrainingPlanRepository trainingPlanRepository;
    private final TrainingPlanMapper trainingPlanMapper;
    private final UserRepository userRepository;
    private final WorkoutCategoryRepository workoutCategoryRepository;
    private final WorkoutLogRepository workoutLogRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TrainingPlanResponse> getTrainingPlansByUserId(Long userId, LocalDate startDate, LocalDate endDate) {

        List<TrainingPlan> plans;
        if (startDate != null && endDate != null) {
            plans = trainingPlanRepository.findByUserIdAndPlannedDateBetweenOrderByPlannedDateAsc(userId, startDate,
                    endDate);
        } else {
            plans = trainingPlanRepository.findByUserIdOrderByPlannedDateAsc(userId);
        }

        return plans.stream()
                .map(trainingPlanMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TrainingPlanResponse getTrainingPlanById(Long planId, Long userId) {

        TrainingPlan plan = findOwnedPlan(planId, userId);

        return trainingPlanMapper.toResponse(plan);
    }

    @Override
    public TrainingPlanResponse createTrainingPlan(Long userId, TrainingPlanRequest request) {

        if (request.plannedDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(PAST_DATE_MESSAGE);
        }

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

        TrainingPlan plan = findOwnedPlan(id, userId);

        // Data z przeszłości jest dozwolona tylko wtedy, gdy użytkownik jej nie zmienia
        // (edycja starego planu).
        if (!request.plannedDate().equals(plan.getPlannedDate())
                && request.plannedDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(PAST_DATE_MESSAGE);
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

        TrainingPlan plan = findOwnedPlan(trainingPlanId, userId);

        plan.setStatus(request.status());

        return trainingPlanMapper.toResponse(trainingPlanRepository.save(plan));
    }

    @Override
    public void deleteTrainingPlan(Long userId, Long trainingPlanId) {

        TrainingPlan plan = findOwnedPlan(trainingPlanId, userId);

        // Wpisy w dzienniku to historia wykonanych treningów - nie kasujemy ich razem
        // z planem, tylko odpinamy (stają się wpisami ad-hoc).
        workoutLogRepository.detachLogsFromPlan(trainingPlanId);

        trainingPlanRepository.delete(plan);
    }

    private TrainingPlan findOwnedPlan(Long planId, Long userId) {

        TrainingPlan plan = trainingPlanRepository.findById(planId)
                .orElseThrow(() -> new TrainingPlanNotFoundException(planId));

        if (!plan.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Brak uprawnień do tego planu treningowego");
        }

        return plan;
    }

}
