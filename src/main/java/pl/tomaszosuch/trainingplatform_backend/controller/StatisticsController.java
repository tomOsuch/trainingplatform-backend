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
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.service.StatisticsService;

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

        LocalDate rangeFrom = from;
        LocalDate rangeTo = to;
        if (from == null && to == null) {
            YearMonth currentMonth = YearMonth.now();
            rangeFrom = currentMonth.atDay(1);
            rangeTo = currentMonth.atEndOfMonth();
        } else if (from == null || to == null) {
            // Uzupełnianie brakującej połówki byłoby zgadywaniem okresu za użytkownika.
            throw new IllegalArgumentException(PARTIAL_RANGE_MESSAGE);
        }

        return ResponseEntity.ok(statisticsService.statistics(currentUser.getId(), rangeFrom, rangeTo));
    }
}
