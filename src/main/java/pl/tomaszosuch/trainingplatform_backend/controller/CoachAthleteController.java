package pl.tomaszosuch.trainingplatform_backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pl.tomaszosuch.trainingplatform_backend.dto.response.*;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.GoalStatus;
import pl.tomaszosuch.trainingplatform_backend.service.CoachAthleteService;
import pl.tomaszosuch.trainingplatform_backend.service.model.StatisticsRange;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/coach/athletes")
@RequiredArgsConstructor
public class CoachAthleteController {

    private final CoachAthleteService coachAthleteService;

    @GetMapping
    public ResponseEntity<List<AthleteResponse>> getAthletes(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(coachAthleteService.athletes(currentUser.getId()));
    }

    @GetMapping("/{id}/training-plans")
    public ResponseEntity<List<TrainingPlanResponse>> getTrainingPlans(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(
                coachAthleteService.trainingPlans(currentUser.getId(), id, from, to));
    }

    @GetMapping("/{id}/workout-logs")
    public ResponseEntity<List<WorkoutLogResponse>> getWorkoutLogs(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(
                coachAthleteService.workoutLogs(currentUser.getId(), id, categoryId, from, to));
    }

    @GetMapping("/{id}/goals")
    public ResponseEntity<List<GoalResponse>> getGoals(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @RequestParam(required = false) String status) {
        GoalStatus filter = status == null ? null : GoalStatus.fromParam(status);
        return ResponseEntity.ok(coachAthleteService.goals(currentUser.getId(), id, filter));
    }

    @GetMapping("/{id}/statistics")
    public ResponseEntity<StatisticsResponse> getStatistics(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        StatisticsRange range = StatisticsRange.of(from, to);

        return ResponseEntity.ok(
                coachAthleteService.statistics(currentUser.getId(), id, range.from(), range.to()));
    }

    @GetMapping("/{id}/statistics/weekly")
    public ResponseEntity<WeeklyStatisticsResponse> getWeeklyStatistics(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(
                coachAthleteService.weeklyStatistics(currentUser.getId(), id, from, to));
    }
}
