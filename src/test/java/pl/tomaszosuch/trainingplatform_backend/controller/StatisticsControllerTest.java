package pl.tomaszosuch.trainingplatform_backend.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import pl.tomaszosuch.trainingplatform_backend.dto.response.CategoryStatisticsResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.PlanCompletionResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.StatisticsResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.security.JwtAuthenticationFilter;
import pl.tomaszosuch.trainingplatform_backend.service.StatisticsService;

@WebMvcTest(controllers = StatisticsController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class))
@WithMockUser
@Import(StatisticsControllerTest.TestConfig.class)
@DisplayName("StatisticsControllerTest")
class StatisticsControllerTest {

    private static final LocalDate FROM = LocalDate.of(2026, 3, 1);
    private static final LocalDate TO = LocalDate.of(2026, 3, 31);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StatisticsService statisticsService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
    }

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L).email("jan@example.com").firstName("Jan").lastName("Kowalski")
                .role(Role.USER).isActive(true)
                .build();
    }

    private static StatisticsResponse response(LocalDate from, LocalDate to) {
        return new StatisticsResponse(from, to, 7, 390,
                List.of(new CategoryStatisticsResponse(5L, "Taniec", "#9B59B6", 4, 240),
                        new CategoryStatisticsResponse(6L, "Siłownia", "#E67E22", 3, 150)),
                new PlanCompletionResponse(6, 2, 3, 4, 12, 50));
    }

    @Test
    @DisplayName("GET /statistics zwraca komplet danych ekranu w jednym żądaniu")
    void shouldReturnFullPayload() throws Exception {
        when(statisticsService.statistics(1L, FROM, TO)).thenReturn(response(FROM, TO));

        mockMvc.perform(get("/statistics")
                        .param("from", "2026-03-01").param("to", "2026-03-31")
                        .with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-03-01"))
                .andExpect(jsonPath("$.to").value("2026-03-31"))
                .andExpect(jsonPath("$.workoutCount").value(7))
                .andExpect(jsonPath("$.totalMinutes").value(390))
                .andExpect(jsonPath("$.byCategory.length()").value(2))
                .andExpect(jsonPath("$.byCategory[0].categoryId").value(5))
                .andExpect(jsonPath("$.byCategory[0].categoryName").value("Taniec"))
                .andExpect(jsonPath("$.byCategory[0].categoryColor").value("#9B59B6"))
                .andExpect(jsonPath("$.byCategory[0].workoutCount").value(4))
                .andExpect(jsonPath("$.byCategory[0].totalMinutes").value(240))
                .andExpect(jsonPath("$.planCompletion.completed").value(6))
                .andExpect(jsonPath("$.planCompletion.skipped").value(2))
                .andExpect(jsonPath("$.planCompletion.cancelled").value(3))
                .andExpect(jsonPath("$.planCompletion.unresolved").value(4))
                .andExpect(jsonPath("$.planCompletion.completionBase").value(12))
                .andExpect(jsonPath("$.planCompletion.completionRate").value(50));
    }

    @Test
    @DisplayName("bez parametrów bierze bieżący miesiąc i odsyła jego granice")
    void shouldDefaultToCurrentMonth() throws Exception {
        YearMonth current = YearMonth.now();
        LocalDate first = current.atDay(1);
        LocalDate last = current.atEndOfMonth();
        when(statisticsService.statistics(1L, first, last)).thenReturn(response(first, last));

        mockMvc.perform(get("/statistics").with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value(first.toString()))
                .andExpect(jsonPath("$.to").value(last.toString()));

        verify(statisticsService).statistics(1L, first, last);
    }

    @Test
    @DisplayName("podanie tylko jednej granicy zakresu daje 400 z komunikatem")
    void shouldRejectPartialRange() throws Exception {
        mockMvc.perform(get("/statistics").param("from", "2026-03-01").with(user(currentUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("obie granice")));

        mockMvc.perform(get("/statistics").param("to", "2026-03-31").with(user(currentUser)))
                .andExpect(status().isBadRequest());

        verify(statisticsService, never()).statistics(eq(1L), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("odwrócony zakres daje 400 z komunikatem z serwisu")
    void shouldReturn400ForReversedRange() throws Exception {
        when(statisticsService.statistics(1L, TO, FROM))
                .thenThrow(new IllegalArgumentException("Data początkowa nie może być późniejsza niż końcowa"));

        mockMvc.perform(get("/statistics")
                        .param("from", "2026-03-31").param("to", "2026-03-01")
                        .with(user(currentUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("początkowa")));
    }

    @Test
    @DisplayName("pusty okres: zera, pusta lista kategorii i procent null")
    void shouldReturnZerosForEmptyPeriod() throws Exception {
        when(statisticsService.statistics(1L, FROM, TO)).thenReturn(new StatisticsResponse(
                FROM, TO, 0, 0, List.of(), new PlanCompletionResponse(0, 0, 0, 0, 0, null)));

        mockMvc.perform(get("/statistics")
                        .param("from", "2026-03-01").param("to", "2026-03-31")
                        .with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workoutCount").value(0))
                .andExpect(jsonPath("$.totalMinutes").value(0))
                .andExpect(jsonPath("$.byCategory").isEmpty())
                .andExpect(jsonPath("$.planCompletion.completionRate").doesNotExist());
    }

}