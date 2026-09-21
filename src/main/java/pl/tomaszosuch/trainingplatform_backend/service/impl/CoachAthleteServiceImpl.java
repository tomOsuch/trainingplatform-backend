package pl.tomaszosuch.trainingplatform_backend.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.tomaszosuch.trainingplatform_backend.dto.request.TrainingPlanRequest;
import pl.tomaszosuch.trainingplatform_backend.dto.response.*;
import pl.tomaszosuch.trainingplatform_backend.enums.CooperationStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.GoalStatus;
import pl.tomaszosuch.trainingplatform_backend.mapper.CooperationMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.CooperationRepository;
import pl.tomaszosuch.trainingplatform_backend.security.AthleteAccessGuard;
import pl.tomaszosuch.trainingplatform_backend.service.*;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CoachAthleteServiceImpl implements CoachAthleteService {

    private final AthleteAccessGuard athleteAccessGuard;
    private final CooperationRepository cooperationRepository;
    private final CooperationMapper cooperationMapper;

    private final TrainingPlanService trainingPlanService;
    private final WorkoutLogService workoutLogService;
    private final GoalService goalService;
    private final StatisticsService statisticsService;

    @Override
    public List<AthleteResponse> athletes(Long coachId) {
        return cooperationRepository.findByCoachIdAndStatus(coachId, CooperationStatus.ACTIVE).stream()
                .map(cooperationMapper::toAthleteResponse)
                .toList();
    }

    @Override
    public List<TrainingPlanResponse> trainingPlans(Long coachId, Long athleteId, LocalDate from, LocalDate to) {
        athleteAccessGuard.requireActiveCooperation(coachId, athleteId);
        return trainingPlanService.getTrainingPlansByUserId(athleteId, from, to);
    }

    @Override
    @Transactional
    public TrainingPlanResponse createTrainingPlan(Long coachId, Long athleteId, TrainingPlanRequest request) {
        athleteAccessGuard.requireActiveCooperation(coachId, athleteId);
        return trainingPlanService.createTrainingPlanForAthlete(athleteId, coachId, request);
    }

    @Override
    @Transactional
    public TrainingPlanResponse updateTrainingPlan(Long coachId, Long athleteId, Long planId,
                                                   TrainingPlanRequest request) {
        athleteAccessGuard.requireActiveCooperation(coachId, athleteId);
        return trainingPlanService.updateTrainingPlanForAthlete(athleteId, coachId, planId, request);
    }

    @Override
    public List<WorkoutLogResponse> workoutLogs(Long coachId, Long athleteId, Long categoryId, LocalDate from, LocalDate to) {
        athleteAccessGuard.requireActiveCooperation(coachId, athleteId);
        return workoutLogService.getUserLogs(athleteId, categoryId, from, to);
    }

    @Override
    public List<GoalResponse> goals(Long coachId, Long athleteId, GoalStatus status) {
        athleteAccessGuard.requireActiveCooperation(coachId, athleteId);
        return goalService.getGoals(athleteId, status);
    }

    @Override
    public StatisticsResponse statistics(Long coachId, Long athleteId, LocalDate from, LocalDate to) {
        athleteAccessGuard.requireActiveCooperation(coachId, athleteId);
        return statisticsService.statistics(athleteId, from, to);
    }

    @Override
    public WeeklyStatisticsResponse weeklyStatistics(Long coachId, Long athleteId, LocalDate from, LocalDate to) {
        athleteAccessGuard.requireActiveCooperation(coachId, athleteId);
        return statisticsService.weeklyStatistics(athleteId, from, to);
    }
}
