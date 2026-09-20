package pl.tomaszosuch.trainingplatform_backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.tomaszosuch.trainingplatform_backend.dto.response.StatisticsResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.WeeklyStatisticsResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.service.StatisticsService;
import pl.tomaszosuch.trainingplatform_backend.service.model.StatisticsRange;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private static final String PARTIAL_RANGE_MESSAGE =
            "Podaj obie granice zakresu albo żadnej — wtedy statystyki obejmą bieżący miesiąc";

    private final StatisticsService statisticsService;

    @GetMapping
    public ResponseEntity<StatisticsResponse> getStatistics(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        StatisticsRange range = StatisticsRange.of(from, to);

        return ResponseEntity.ok(
                statisticsService.statistics(currentUser.getId(), range.from(), range.to()));
    }

    @GetMapping("/weekly")
    public ResponseEntity<WeeklyStatisticsResponse> getWeeklyStatistics(
            @AuthenticationPrincipal User currentUser,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(statisticsService.weeklyStatistics(currentUser.getId(), from, to));
    }

}
