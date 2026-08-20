package pl.tomaszosuch.trainingplatform_backend.service.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import pl.tomaszosuch.trainingplatform_backend.dto.request.WorkoutLogRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.WorkoutLogResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.TrainingPlan;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutLog;
import pl.tomaszosuch.trainingplatform_backend.exception.UserNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.exception.TrainingPlanNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.exception.WorkoutCategoryNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.exception.WorkoutLogNotFoundException;
import pl.tomaszosuch.trainingplatform_backend.mapper.WorkoutLogMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.TrainingPlanRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.UserRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutCategoryRepository;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutLogRepository;
import pl.tomaszosuch.trainingplatform_backend.service.WorkoutLogService;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkoutLogServiceImpl implements WorkoutLogService {

    private final WorkoutLogRepository workoutLogRepository;
    private final UserRepository userRepository;
    private final WorkoutCategoryRepository categoryRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final WorkoutLogMapper workoutLogMapper;

    @Override
    @Transactional(readOnly = true)
    public List<WorkoutLogResponse> getUserLogs(Long userId, Long categoryId, LocalDate from, LocalDate to) {
        List<WorkoutLog> logs;
        if (categoryId != null) {
            logs = workoutLogRepository.findByUserIdAndCategoryIdOrderByPerformedDateDesc(userId, categoryId);
        } else if (from != null && to != null) {
            logs = workoutLogRepository.findByUserIdAndPerformedDateBetweenOrderByPerformedDateDesc(userId, from, to);
        } else {
            logs = workoutLogRepository.findByUserIdOrderByPerformedDateDesc(userId);
        }
        return logs.stream().map(workoutLogMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkoutLogResponse getById(Long userId, Long logId) {
        WorkoutLog log = findOwnedLog(logId, userId);
        return workoutLogMapper.toResponse(log);
    }

    @Override
    public WorkoutLogResponse create(Long userId, WorkoutLogRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        WorkoutCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new WorkoutCategoryNotFoundException(request.categoryId()));

        TrainingPlan plan = resolvePlan(request.planId(), userId);

        WorkoutLog log = WorkoutLog.builder()
                .user(user)
                .category(category)
                .plan(plan)
                .title(request.title())
                .performedDate(request.performedDate())
                .performedTime(request.performedTime())
                .durationMin(request.durationMin())
                .intensity(request.intensity())
                .notes(request.notes())
                .build();

        return workoutLogMapper.toResponse(workoutLogRepository.save(log));
    }

    @Override
    public WorkoutLogResponse update(Long userId, Long logId, WorkoutLogRequest request) {
        WorkoutLog log = findOwnedLog(logId, userId);

        WorkoutCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new WorkoutCategoryNotFoundException(request.categoryId()));

        log.setCategory(category);
        log.setPlan(resolvePlan(request.planId(), userId));
        log.setTitle(request.title());
        log.setPerformedDate(request.performedDate());
        log.setPerformedTime(request.performedTime());
        log.setDurationMin(request.durationMin());
        log.setIntensity(request.intensity());
        log.setNotes(request.notes());

        return workoutLogMapper.toResponse(workoutLogRepository.save(log));
    }

    @Override
    public void delete(Long userId, Long logId) {
        WorkoutLog log = findOwnedLog(logId, userId);
        workoutLogRepository.delete(log);
    }

    private WorkoutLog findOwnedLog(Long logId, Long userId) {
        WorkoutLog log = workoutLogRepository.findById(logId)
                .orElseThrow(() -> new WorkoutLogNotFoundException(logId));

        if (!log.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Brak uprawnień do tego wpisu w dzienniku");
        }

        return log;
    }

    private TrainingPlan resolvePlan(Long planId, Long userId) {
        if (planId == null) {
            return null;
        }

        TrainingPlan plan = trainingPlanRepository.findById(planId)
                .orElseThrow(() -> new TrainingPlanNotFoundException(planId));

        if (!plan.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Brak uprawnień do tego planu treningowego");
        }

        return plan;
    }

}
