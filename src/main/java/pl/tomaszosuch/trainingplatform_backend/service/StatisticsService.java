package pl.tomaszosuch.trainingplatform_backend.service;

import pl.tomaszosuch.trainingplatform_backend.dto.response.StatisticsResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.WeeklyStatisticsResponse;
import pl.tomaszosuch.trainingplatform_backend.service.model.IntensitySummary;
import pl.tomaszosuch.trainingplatform_backend.service.model.PlanCompletion;
import pl.tomaszosuch.trainingplatform_backend.service.model.WorkoutStatistics;

import java.time.LocalDate;

public interface StatisticsService {

    WorkoutStatistics workoutStatistics(Long userId, LocalDate from, LocalDate to);

    PlanCompletion planCompletion(Long userId, LocalDate from, LocalDate to);

    IntensitySummary intensitySummary(Long userId, LocalDate from, LocalDate to);

    StatisticsResponse statistics(Long userId, LocalDate from, LocalDate to);

    WeeklyStatisticsResponse weeklyStatistics(Long userId, LocalDate from, LocalDate to);
}
