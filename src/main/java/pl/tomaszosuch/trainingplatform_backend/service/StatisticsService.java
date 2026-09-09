package pl.tomaszosuch.trainingplatform_backend.service;

import pl.tomaszosuch.trainingplatform_backend.service.model.PlanCompletion;
import pl.tomaszosuch.trainingplatform_backend.service.model.WorkoutStatistics;

import java.time.LocalDate;

public interface StatisticsService {

    WorkoutStatistics workoutStatistics(Long userId, LocalDate from, LocalDate to);

    PlanCompletion planCompletion(Long userId, LocalDate from, LocalDate to);
}
