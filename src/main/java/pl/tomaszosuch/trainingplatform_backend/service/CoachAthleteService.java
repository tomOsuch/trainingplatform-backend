package pl.tomaszosuch.trainingplatform_backend.service;

import java.time.LocalDate;
import java.util.List;

import pl.tomaszosuch.trainingplatform_backend.dto.response.*;
import pl.tomaszosuch.trainingplatform_backend.enums.GoalStatus;

public interface CoachAthleteService {

    List<AthleteResponse> athletes(Long coachId);

    List<TrainingPlanResponse> trainingPlans(Long coachId, Long athleteId, LocalDate from, LocalDate to);

    List<WorkoutLogResponse> workoutLogs(Long coachId, Long athleteId, Long categoryId,
                                         LocalDate from, LocalDate to);

    List<GoalResponse> goals(Long coachId, Long athleteId, GoalStatus status);

    StatisticsResponse statistics(Long coachId, Long athleteId, LocalDate from, LocalDate to);

    WeeklyStatisticsResponse weeklyStatistics(Long coachId, Long athleteId, LocalDate from, LocalDate to);
}