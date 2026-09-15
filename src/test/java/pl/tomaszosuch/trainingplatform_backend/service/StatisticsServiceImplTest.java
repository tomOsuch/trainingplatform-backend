package pl.tomaszosuch.trainingplatform_backend.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.tomaszosuch.trainingplatform_backend.enums.PlanStatus;
import pl.tomaszosuch.trainingplatform_backend.mapper.StatisticsMapper;
import pl.tomaszosuch.trainingplatform_backend.repository.*;
import pl.tomaszosuch.trainingplatform_backend.service.impl.StatisticsServiceImpl;
import pl.tomaszosuch.trainingplatform_backend.service.model.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatisticsServiceImplTest")
class StatisticsServiceImplTest {

    private static final LocalDate FROM = LocalDate.of(2026, 3, 1);
    private static final LocalDate TO = LocalDate.of(2026, 3, 31);

    @Mock
    private WorkoutLogRepository workoutLogRepository;

    @Mock
    private TrainingPlanRepository trainingPlanRepository;

    @Mock
    private StatisticsMapper statisticsMapper;

    @InjectMocks
    private StatisticsServiceImpl service;

    private static CategoryStatsView row(Long id, String name, String color, String icon, long sessions, long minutes) {
        return new CategoryStatsView() {
            public Long getCategoryId() {
                return id;
            }

            public String getCategoryName() {
                return name;
            }

            public String getCategoryColor() {
                return color;
            }

            public String getCategoryIconName() {
                return icon;
            }

            public Long getSessions() {
                return sessions;
            }

            public Long getMinutes() {
                return minutes;
            }
        };
    }

    private static PlanStatusCountView statusRow(PlanStatus status, long count) {
        return new PlanStatusCountView() {
            public PlanStatus getStatus() {
                return status;
            }

            public Long getCount() {
                return count;
            }
        };
    }

    private void givenPlanCounts(Map<PlanStatus, Long> counts) {
        when(trainingPlanRepository.countByStatus(eq(7L), eq(FROM), eq(TO), any()))
                .thenReturn(counts.entrySet().stream()
                        .map(e -> statusRow(e.getKey(), e.getValue()))
                        .toList());
    }

    private static DailyStatsView day(LocalDate date, long sessions, long minutes) {
        return new DailyStatsView() {
            public LocalDate getDay() {
                return date;
            }

            public Long getSessions() {
                return sessions;
            }

            public Long getMinutes() {
                return minutes;
            }
        };
    }


    private static IntensityStatsView intensity(long total, long rated, long sum) {
        return new IntensityStatsView() {
            public long getTotalCount() {
                return total;
            }

            public long getRatedCount() {
                return rated;
            }

            public long getIntensitySum() {
                return sum;
            }
        };
    }

    private WeeklyStatistics capturedWeekly(LocalDate from, LocalDate to) {
        service.weeklyStatistics(7L, from, to);
        ArgumentCaptor<WeeklyStatistics> captor = ArgumentCaptor.forClass(WeeklyStatistics.class);
        verify(statisticsMapper).toWeeklyResponse(eq(from), eq(to), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("sumy łączne są sumą rozbicia na kategorie")
    void shouldSumTotalsFromBreakdown() {
        when(workoutLogRepository.aggregateByCategory(7L, FROM, TO)).thenReturn(List.of(
                row(1L, "Taniec", "#9B59B6", "music", 4, 240),
                row(2L, "Siłownia", "#E67E22", "music", 3, 150)));

        WorkoutStatistics stats = service.workoutStatistics(7L, FROM, TO);

        assertEquals(7, stats.totalSessions());
        assertEquals(390, stats.totalMinutes());
        assertEquals(390, stats.byCategory().stream().mapToLong(CategoryStatistics::minutes).sum());
    }

    @Test
    @DisplayName("kategorie sortowane malejąco po minutach, potem po liczbie sesji")
    void shouldSortByMinutesThenSessions() {
        when(workoutLogRepository.aggregateByCategory(7L, FROM, TO)).thenReturn(List.of(
                row(1L, "Bieganie", "#1ABC9C", "music", 2, 60),
                row(2L, "Taniec", "#9B59B6", "music", 1, 180),
                row(3L, "Rozciąganie", "#95A5A6", "music", 5, 60)));

        List<CategoryStatistics> byCategory = service.workoutStatistics(7L, FROM, TO).byCategory();

        assertEquals("Taniec", byCategory.get(0).categoryName());       // 180 min
        assertEquals("Rozciąganie", byCategory.get(1).categoryName());  // 60 min, 5 sesji
        assertEquals("Bieganie", byCategory.get(2).categoryName());     // 60 min, 2 sesje
    }

    @Test
    @DisplayName("przy równych minutach i sesjach decyduje nazwa")
    void shouldFallBackToNameForStableOrder() {
        when(workoutLogRepository.aggregateByCategory(7L, FROM, TO)).thenReturn(List.of(
                row(1L, "Zumba", "#111111", "music", 2, 60),
                row(2L, "Aqua aerobik", "#222222", "music", 2, 60)));

        List<CategoryStatistics> byCategory = service.workoutStatistics(7L, FROM, TO).byCategory();

        assertEquals("Aqua aerobik", byCategory.get(0).categoryName());
        assertEquals("Zumba", byCategory.get(1).categoryName());
    }

    @Test
    @DisplayName("okres bez treningów zwraca zera i pustą listę, nie błąd")
    void shouldReturnZerosForEmptyPeriod() {
        when(workoutLogRepository.aggregateByCategory(7L, FROM, TO)).thenReturn(List.of());

        WorkoutStatistics stats = service.workoutStatistics(7L, FROM, TO);

        assertEquals(0, stats.totalSessions());
        assertEquals(0, stats.totalMinutes());
        assertTrue(stats.byCategory().isEmpty());
    }

    @Test
    @DisplayName("kolor kategorii trafia do wyniku")
    void shouldCarryCategoryColor() {
        when(workoutLogRepository.aggregateByCategory(7L, FROM, TO))
                .thenReturn(List.of(row(1L, "Taniec", "#9B59B6", "music", 1, 60)));

        assertEquals("#9B59B6", service.workoutStatistics(7L, FROM, TO).byCategory().get(0).categoryColor());
        assertEquals("music", service.workoutStatistics(7L, FROM, TO).byCategory().get(0).categoryIconName());
    }

    @Test
    @DisplayName("brak dat albo odwrócony zakres kończy się błędem walidacji")
    void shouldRejectInvalidRange() {
        assertThrows(IllegalArgumentException.class, () -> service.workoutStatistics(7L, null, TO));
        assertThrows(IllegalArgumentException.class, () -> service.workoutStatistics(7L, FROM, null));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.workoutStatistics(7L, TO, FROM));
        assertTrue(ex.getMessage().contains("początkowa"));

        verify(workoutLogRepository, never()).aggregateByCategory(anyLong(), any(), any());
    }

    @Test
    @DisplayName("statusy trafiają do właściwych kubełków, PLANNED to nierozstrzygnięte")
    void shouldMapStatusesToBuckets() {
        givenPlanCounts(Map.of(
                PlanStatus.COMPLETED, 6L,
                PlanStatus.SKIPPED, 2L,
                PlanStatus.CANCELLED, 3L,
                PlanStatus.PLANNED, 4L));

        PlanCompletion completion = service.planCompletion(7L, FROM, TO);

        assertEquals(6, completion.completed());
        assertEquals(2, completion.skipped());
        assertEquals(3, completion.cancelled());
        assertEquals(4, completion.unresolved());
    }

    @Test
    @DisplayName("mianownik pomija anulowane, ale wlicza nierozstrzygnięte")
    void shouldExcludeCancelledAndIncludeUnresolvedInBase() {
        givenPlanCounts(Map.of(
                PlanStatus.COMPLETED, 6L,
                PlanStatus.SKIPPED, 2L,
                PlanStatus.CANCELLED, 3L,
                PlanStatus.PLANNED, 4L));

        PlanCompletion completion = service.planCompletion(7L, FROM, TO);

        assertEquals(12, completion.completionBase());   // 6 + 2 + 4, bez 3 anulowanych
        assertEquals(50, completion.completionRate());
    }

    @Test
    @DisplayName("brakujące statusy to zera")
    void shouldDefaultMissingStatusesToZero() {
        givenPlanCounts(Map.of(PlanStatus.COMPLETED, 5L));

        PlanCompletion completion = service.planCompletion(7L, FROM, TO);

        assertEquals(5, completion.completed());
        assertEquals(0, completion.skipped());
        assertEquals(0, completion.cancelled());
        assertEquals(0, completion.unresolved());
        assertEquals(100, completion.completionRate());
    }

    @Test
    @DisplayName("okres bez planów: zera i procent null, nie zero procent")
    void shouldReturnNullRateForEmptyPeriod() {
        givenPlanCounts(Map.of());

        PlanCompletion completion = service.planCompletion(7L, FROM, TO);

        assertEquals(0, completion.completionBase());
        assertNull(completion.completionRate());
    }

    @Test
    @DisplayName("same anulowane też dają procent null — nie ma czego liczyć")
    void shouldReturnNullRateWhenOnlyCancelled() {
        givenPlanCounts(Map.of(PlanStatus.CANCELLED, 4L));

        assertNull(service.planCompletion(7L, FROM, TO).completionRate());
    }

    @Test
    @DisplayName("procent jest obcinany, nie zaokrąglany")
    void shouldTruncatePercent() {
        assertEquals(66, new PlanCompletion(2, 1, 0, 0).completionRate());    // 66,67%
        assertEquals(99, new PlanCompletion(199, 1, 0, 0).completionRate());  // 99,5% to nie 100%
    }

    @Test
    @DisplayName("realizacja planu też waliduje zakres dat")
    void shouldRejectInvalidRangeForPlanCompletion() {
        assertThrows(IllegalArgumentException.class, () -> service.planCompletion(7L, null, TO));
        assertThrows(IllegalArgumentException.class, () -> service.planCompletion(7L, TO, FROM));

        verify(trainingPlanRepository, never()).countByStatus(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("statistics składa agregaty dziennika i realizację planu w jedną odpowiedź")
    void shouldComposeBothParts() {
        when(workoutLogRepository.aggregateByCategory(7L, FROM, TO))
                .thenReturn(List.of(row(5L, "Taniec", "#9B59B6", "music", 4, 240)));
        when(workoutLogRepository.aggregateIntensity(7L, FROM, TO)).thenReturn(intensity(4, 2, 17));
        givenPlanCounts(Map.of(PlanStatus.COMPLETED, 6L, PlanStatus.PLANNED, 2L));

        service.statistics(7L, FROM, TO);

        verify(statisticsMapper).toResponse(eq(FROM), eq(TO),
                any(WorkoutStatistics.class), any(PlanCompletion.class), any(IntensitySummary.class));
    }

    @Test
    @DisplayName("odwrócony zakres nie sięga do bazy ani do mappera")
    void shouldValidateRangeBeforeQuerying() {
        assertThrows(IllegalArgumentException.class, () -> service.statistics(7L, TO, FROM));

        verify(workoutLogRepository, never()).aggregateByCategory(anyLong(), any(), any());
        verify(trainingPlanRepository, never()).countByStatus(anyLong(), any(), any(), any());
        verifyNoInteractions(statisticsMapper);
        verify(workoutLogRepository, never()).aggregateIntensity(anyLong(), any(), any());
    }

    @Test
    @DisplayName("tydzień bez treningów wraca z zerem, nie znika z odpowiedzi")
    void shouldIncludeEmptyWeeks() {
        when(workoutLogRepository.aggregateByDay(7L, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 22)))
                .thenReturn(List.of(day(LocalDate.of(2026, 3, 3), 2, 90),
                        day(LocalDate.of(2026, 3, 17), 1, 45)));

        WeeklyStatistics weekly = capturedWeekly(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 22));

        assertEquals(3, weekly.weeks().size());
        assertEquals(0L, weekly.weeks().get(1).sessions());
        assertEquals(0L, weekly.weeks().get(1).minutes());
        assertEquals(LocalDate.of(2026, 3, 9), weekly.weeks().get(1).weekStart());
    }

    @Test
    @DisplayName("niedziela należy do tygodnia zaczynającego się w poprzedni poniedziałek")
    void shouldBucketSundayIntoPreviousMonday() {
        when(workoutLogRepository.aggregateByDay(any(), any(), any()))
                .thenReturn(List.of(day(LocalDate.of(2026, 3, 8), 1, 60)));

        WeeklyStatistics weekly = capturedWeekly(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 8));

        assertEquals(1, weekly.weeks().size());
        assertEquals(LocalDate.of(2026, 3, 2), weekly.weeks().get(0).weekStart());
        assertEquals(1L, weekly.weeks().get(0).sessions());
    }

    @Test
    @DisplayName("skrajne tygodnie zakresu są oznaczone jako niepełne")
    void shouldMarkPartialWeeks() {
        when(workoutLogRepository.aggregateByDay(any(), any(), any())).thenReturn(List.of());

        WeeklyStatistics weekly = capturedWeekly(LocalDate.of(2026, 3, 4), LocalDate.of(2026, 3, 18));

        assertTrue(weekly.weeks().get(0).partial());
        assertFalse(weekly.weeks().get(1).partial());
        assertTrue(weekly.weeks().get(2).partial());
    }

    @Test
    @DisplayName("odwrócony zakres odrzucony bez pytania bazy")
    void shouldRejectInvertedRangeInWeekly() {
        assertThrows(IllegalArgumentException.class, () -> service.weeklyStatistics(
                7L, LocalDate.of(2026, 3, 20), LocalDate.of(2026, 3, 1)));

        verify(workoutLogRepository, never()).aggregateByDay(any(), any(), any());
    }


    @Test
    @DisplayName("okres bez ani jednej oceny nie ma średniej — null, nie zero")
    void shouldReturnNoAverageWithoutRatings() {
        when(workoutLogRepository.aggregateIntensity(7L, FROM, TO)).thenReturn(intensity(5, 0, 0));

        IntensitySummary summary = service.intensitySummary(7L, FROM, TO);

        assertNull(summary.average());
        assertEquals(0L, summary.ratedCount());
        assertEquals(5L, summary.totalCount());
    }

    @Test
    @DisplayName("średnia liczy się wyłącznie z ocenionych treningów")
    void shouldAverageOnlyRatedWorkouts() {
        when(workoutLogRepository.aggregateIntensity(7L, FROM, TO)).thenReturn(intensity(20, 4, 34));

        assertEquals(new BigDecimal("8.5"), service.intensitySummary(7L, FROM, TO).average());
    }

}