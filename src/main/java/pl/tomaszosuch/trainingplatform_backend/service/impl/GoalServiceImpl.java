package pl.tomaszosuch.trainingplatform_backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.tomaszosuch.trainingplatform_backend.dto.request.GoalRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.request.GoalStatusUpdateRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.GoalResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.Goal;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.enums.GoalStatus;
import pl.tomaszosuch.trainingplatform_backend.exception.GoalNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.exception.UserNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.exception.WorkoutCategoryNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.mapper.GoalMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.GoalRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutCategoryRepository;
import pl.tomaszosuch.trainingplatform_backend.service.GoalProgressService;
import pl.tomaszosuch.trainingplatform_backend.service.GoalService;
import pl.tomaszosuch.trainingplatform_backend.service.model.GoalProgress;

import org.springframework.security.access.AccessDeniedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class GoalServiceImpl implements GoalService {

    private static final String WINDOW_MESSAGE = "Data końcowa nie może być wcześniejsza niż data początkowa";
    private static final String ACHIEVED_EDIT_MESSAGE =
            "Osiągnięty cel nie podlega edycji — najpierw cofnij jego osiągnięcie";

    private final GoalRepository goalRepository;
    private final GoalMapper goalMapper;
    private final GoalProgressService goalProgressService;
    private final UserRepository userRepository;
    private final WorkoutCategoryRepository workoutCategoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getGoals(Long userId, GoalStatus status) {

        List<Goal> goals = switch (status == null ? "ALL" : status.name()){
            case "ACTIVE" -> goalRepository.findByUserIdAndAchievedAtIsNullOrderByCreatedAtDesc(userId);
            case "ACHIEVED" -> goalRepository.findByUserIdAndAchievedAtIsNotNullOrderByAchievedAtDesc(userId);
            default -> goalRepository.findByUserIdOrderByCreatedAtDesc(userId);
        };
        Map<Long, GoalProgress> progress = goalProgressService.progressOf(userId, goals);

        return goals.stream()
                .map(goal -> goalMapper.toResponse(goal, progress.get(goal.getId())))
                .toList();
    }

    @Override
    public GoalResponse createGoal(Long userId, GoalRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        LocalDate startDate = request.startDate() != null ? request.startDate() : LocalDate.now();
        validateWindow(startDate, request.endDate());

        Goal goal = Goal.builder()
                .user(user)
                .category(resolveCategory(request.categoryId()))
                .title(request.title())
                .description(request.description())
                .metric(request.metric())
                .targetValue(request.targetValue())
                .startDate(startDate)
                .endDate(request.endDate())
                .build();

        Goal saved = goalRepository.save(goal);
        return goalMapper.toResponse(saved, goalProgressService.progressOf(saved));
    }

    @Override
    public GoalResponse updateGoal(Long userId, Long goalId, GoalRequest request) {
        Goal goal = findOwnedGoal(goalId, userId);

        if (goal.isAchieved()) {
            throw new IllegalArgumentException(ACHIEVED_EDIT_MESSAGE);
        }

        LocalDate startDate = request.startDate() != null ? request.startDate() : goal.getStartDate();
        validateWindow(startDate, request.endDate());

        goal.setCategory(resolveCategory(request.categoryId()));
        goal.setTitle(request.title());
        goal.setDescription(request.description());
        goal.setMetric(request.metric());
        goal.setTargetValue(request.targetValue());
        goal.setStartDate(startDate);
        goal.setEndDate(request.endDate());

        Goal saved = goalRepository.save(goal);
        return goalMapper.toResponse(saved, goalProgressService.progressOf(saved));
    }

    @Override
    public GoalResponse changeStatus(Long userId, Long goalId, GoalStatusUpdateRequest request) {
        Goal goal = findOwnedGoal(goalId, userId);

        switch (request.status()) {
            case ACHIEVED -> {
                // Ponowne oznaczenie nadpisałoby migawkę żywą wartością — a migawka ma być
                // zamrożona (US-019). Cel już osiągnięty zostawiamy bez zmian.
                if (!goal.isAchieved()) {
                    GoalProgress progress = goalProgressService.progressOf(goal);
                    goal.setAchievedValue(Math.toIntExact(progress.currentValue()));
                    goal.setAchievedAt(LocalDateTime.now());
                }
            }
            case ACTIVE -> {
                goal.setAchievedAt(null);
                goal.setAchievedValue(null);
            }
        }

        Goal saved = goalRepository.save(goal);
        return goalMapper.toResponse(saved, goalProgressService.progressOf(saved));
    }

    @Override
    public void deleteGoal(Long userId, Long goalId) {
        goalRepository.delete(findOwnedGoal(goalId, userId));
    }

    private Goal findOwnedGoal(Long goalId, Long userId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));

        // BR-05: każdy widzi wyłącznie własne cele. Odmowa, nie 404 — spójnie z planami.
        if (!goal.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Brak uprawnień do tego celu");
        }
        return goal;
    }

    private WorkoutCategory resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return workoutCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new WorkoutCategoryNotFoundException(categoryId));
    }

    private static void validateWindow(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(WINDOW_MESSAGE);
        }
    }
}
