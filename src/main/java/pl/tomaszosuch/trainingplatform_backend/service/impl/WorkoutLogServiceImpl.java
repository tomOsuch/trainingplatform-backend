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
        WorkoutLog log = workoutLogRepository.findById(logId)
            .orElseThrow(() -> new WorkoutLogNotFoundException(logId));
        if (!log.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have access to this workout log.");
        }
        return workoutLogMapper.toResponse(log);
    }

    @Override
    public WorkoutLogResponse create(Long userId, WorkoutLogRequest request) {
         User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        WorkoutCategory category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new WorkoutCategoryNotFoundException(request.categoryId()));

        TrainingPlan plan = null;
        if (request.planId() != null) {
            plan = trainingPlanRepository.findById(request.planId())
                .orElseThrow(() -> new TrainingPlanNotFoundException(request.planId()));
            if (!plan.getUser().getId().equals(userId)) {
                throw new AccessDeniedException("You do not have access to this training plan.");
            }
        }

        WorkoutLog log = WorkoutLog.builder()
            .user(user)
            .category(category)
            .plan(plan)
            .performedDate(request.performedDate())
            .durationMin(request.durationMin())
            .intensity(request.intensity())
            .notes(request.notes())
            .build();

        return workoutLogMapper.toResponse(workoutLogRepository.save(log));
    }

    @Override
    public WorkoutLogResponse update(Long userId, Long logId, WorkoutLogRequest request) {
         WorkoutLog log = workoutLogRepository.findById(logId).orElseThrow(() -> new RuntimeException("Log not found"));

        WorkoutCategory category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new WorkoutCategoryNotFoundException(request.categoryId()));

        log.setCategory(category);
        log.setPlan(trainingPlanRepository.findById(request.planId()).orElse(null));
        log.setPerformedDate(request.performedDate());
        log.setDurationMin(request.durationMin());
        log.setIntensity(request.intensity());
        log.setNotes(request.notes());

        return workoutLogMapper.toResponse(workoutLogRepository.save(log));
    }

    @Override
    public void delete(Long userId, Long logId) {
         WorkoutLog log = workoutLogRepository.findById(logId).orElseThrow(() -> new RuntimeException("Log not found"));
        workoutLogRepository.delete(log);
    }

}
