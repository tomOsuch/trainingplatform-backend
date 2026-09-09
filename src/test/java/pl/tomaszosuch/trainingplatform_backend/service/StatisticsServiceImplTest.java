package pl.tomaszosuch.trainingplatform_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.tomaszosuch.trainingplatform_backend.repository.CategoryStatsView;
import pl.tomaszosuch.trainingplatform_backend.repository.WorkoutLogRepository;
import pl.tomaszosuch.trainingplatform_backend.service.impl.StatisticsServiceImpl;
import pl.tomaszosuch.trainingplatform_backend.service.model.CategoryStatistics;
import pl.tomaszosuch.trainingplatform_backend.service.model.WorkoutStatistics;

@ExtendWith(MockitoExtension.class)
@DisplayName("StatisticsServiceImplTest")
class StatisticsServiceImplTest {

    private static final LocalDate FROM = LocalDate.of(2026, 3, 1);
    private static final LocalDate TO = LocalDate.of(2026, 3, 31);

    @Mock
    private WorkoutLogRepository workoutLogRepository;

    @InjectMocks
    private StatisticsServiceImpl service;

    private static CategoryStatsView row(Long id, String name, String color, long sessions, long minutes) {
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

            public Long getSessions() {
                return sessions;
            }

            public Long getMinutes() {
                return minutes;
            }
        };
    }

    @Test
    @DisplayName("sumy łączne są sumą rozbicia na kategorie")
    void shouldSumTotalsFromBreakdown() {
        when(workoutLogRepository.aggregateByCategory(7L, FROM, TO)).thenReturn(List.of(
                row(1L, "Taniec", "#9B59B6", 4, 240),
                row(2L, "Siłownia", "#E67E22", 3, 150)));

        WorkoutStatistics stats = service.workoutStatistics(7L, FROM, TO);

        assertEquals(7, stats.totalSessions());
        assertEquals(390, stats.totalMinutes());
        assertEquals(390, stats.byCategory().stream().mapToLong(CategoryStatistics::minutes).sum());
    }

    @Test
    @DisplayName("kategorie sortowane malejąco po minutach, potem po liczbie sesji")
    void shouldSortByMinutesThenSessions() {
        when(workoutLogRepository.aggregateByCategory(7L, FROM, TO)).thenReturn(List.of(
                row(1L, "Bieganie", "#1ABC9C", 2, 60),
                row(2L, "Taniec", "#9B59B6", 1, 180),
                row(3L, "Rozciąganie", "#95A5A6", 5, 60)));

        List<CategoryStatistics> byCategory = service.workoutStatistics(7L, FROM, TO).byCategory();

        assertEquals("Taniec", byCategory.get(0).categoryName());       // 180 min
        assertEquals("Rozciąganie", byCategory.get(1).categoryName());  // 60 min, 5 sesji
        assertEquals("Bieganie", byCategory.get(2).categoryName());     // 60 min, 2 sesje
    }

    @Test
    @DisplayName("przy równych minutach i sesjach decyduje nazwa")
    void shouldFallBackToNameForStableOrder() {
        when(workoutLogRepository.aggregateByCategory(7L, FROM, TO)).thenReturn(List.of(
                row(1L, "Zumba", "#111111", 2, 60),
                row(2L, "Aqua aerobik", "#222222", 2, 60)));

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
                .thenReturn(List.of(row(1L, "Taniec", "#9B59B6", 1, 60)));

        assertEquals("#9B59B6", service.workoutStatistics(7L, FROM, TO).byCategory().get(0).categoryColor());
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

}