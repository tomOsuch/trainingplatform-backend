package pl.tomaszosuch.trainingplatform_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pl.tomaszosuch.trainingplatform_backend.config.RemindersProperties;
import pl.tomaszosuch.trainingplatform_backend.entity.TrainingPlan;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.entity.WorkoutCategory;
import pl.tomaszosuch.trainingplatform_backend.enums.PlanStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.exception.EmailDeliveryException;
import pl.tomaszosuch.trainingplatform_backend.repository.TrainingPlanRepository;
import pl.tomaszosuch.trainingplatform_backend.service.impl.ReminderMarker;
import pl.tomaszosuch.trainingplatform_backend.service.impl.TrainingReminderServiceImpl;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrainingReminderServiceImplTest")
class TrainingReminderServiceImplTest {

    @Mock
    private TrainingPlanRepository trainingPlanRepository;

    @Mock
    private ReminderMarker reminderMarker;

    @Mock
    private EmailService emailService;

    @Mock
    private RemindersProperties properties;

    @InjectMocks
    private TrainingReminderServiceImpl service;

    private User user;
    private WorkoutCategory category;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).email("jan@example.com").firstName("Jan").lastName("Kowalski")
                .role(Role.USER).isActive(true)
                .remindersEnabled(true).reminderHoursBefore(24)
                .build();
        category = WorkoutCategory.builder().id(5L).name("Taniec").build();
    }

    private TrainingPlan plan(Long id, LocalDate date, LocalTime time) {
        return TrainingPlan.builder()
                .id(id).user(user).category(category).title("Salsa")
                .plannedDate(date).plannedTime(time).status(PlanStatus.PLANNED)
                .build();
    }

    private void givenCandidates(TrainingPlan... plans) {
        when(trainingPlanRepository.findReminderCandidates(any(LocalDate.class)))
                .thenReturn(List.of(plans));
    }

    @Test
    @DisplayName("wysyła przypomnienie, gdy do treningu zostało mniej niż wybrane wyprzedzenie")
    void shouldSendWhenReminderMomentPassed() {
        // trening jutro o tej samej porze, wyprzedzenie 24 h — moment przypomnienia właśnie minął
        LocalDateTime trainingAt = LocalDateTime.now().plusHours(23);
        givenCandidates(plan(10L, trainingAt.toLocalDate(), trainingAt.toLocalTime()));
        when(reminderMarker.claim(eq(10L), any(LocalDateTime.class))).thenReturn(true);

        assertEquals(1, service.sendDueReminders());

        verify(emailService).sendTrainingReminder(eq("jan@example.com"), eq("Salsa"), eq("Taniec"),
                any(LocalDate.class), any(LocalTime.class));
    }

    @Test
    @DisplayName("nie wysyła, gdy moment przypomnienia jeszcze nie nadszedł")
    void shouldNotSendTooEarly() {
        LocalDateTime trainingAt = LocalDateTime.now().plusHours(30);
        givenCandidates(plan(10L, trainingAt.toLocalDate(), trainingAt.toLocalTime()));

        assertEquals(0, service.sendDueReminders());

        verify(reminderMarker, never()).claim(anyLong(), any());
        verify(emailService, never()).sendTrainingReminder(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("plan bez godziny korzysta z domyślnej godziny i nadal generuje przypomnienie")
    void shouldUseDefaultStartTimeWhenPlanHasNoTime() {
        when(properties.getDefaultStartTime()).thenReturn(LocalTime.NOON);

        user.setReminderHoursBefore(96);
        givenCandidates(plan(10L, LocalDate.now().plusDays(3), null));
        when(reminderMarker.claim(eq(10L), any(LocalDateTime.class))).thenReturn(true);

        assertEquals(1, service.sendDueReminders());

        verify(properties).getDefaultStartTime();
        verify(emailService).sendTrainingReminder(eq("jan@example.com"), eq("Salsa"), eq("Taniec"),
                any(LocalDate.class), org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    @DisplayName("trening, który już się zaczął, nie generuje przypomnienia")
    void shouldSkipPlanAlreadyStarted() {
        LocalDateTime past = LocalDateTime.now().minusHours(2);
        givenCandidates(plan(10L, past.toLocalDate(), past.toLocalTime()));

        assertEquals(0, service.sendDueReminders());

        verify(reminderMarker, never()).claim(anyLong(), any());
    }

    @Test
    @DisplayName("plan zajęty przez inny przebieg nie dostaje drugiego maila")
    void shouldNotSendWhenClaimLost() {
        LocalDateTime trainingAt = LocalDateTime.now().plusHours(23);
        givenCandidates(plan(10L, trainingAt.toLocalDate(), trainingAt.toLocalTime()));
        when(reminderMarker.claim(eq(10L), any(LocalDateTime.class))).thenReturn(false);

        assertEquals(0, service.sendDueReminders());

        verify(emailService, never()).sendTrainingReminder(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    @DisplayName("błąd wysyłki jednego maila nie przerywa pozostałych")
    void shouldContinueAfterDeliveryFailure() {
        LocalDateTime trainingAt = LocalDateTime.now().plusHours(23);
        givenCandidates(
                plan(10L, trainingAt.toLocalDate(), trainingAt.toLocalTime()),
                plan(11L, trainingAt.toLocalDate(), trainingAt.toLocalTime()));
        when(reminderMarker.claim(anyLong(), any(LocalDateTime.class))).thenReturn(true);
        doThrow(new EmailDeliveryException("SMTP padł", new RuntimeException()))
                .doNothing()
                .when(emailService).sendTrainingReminder(anyString(), anyString(), anyString(), any(), any());

        assertEquals(1, service.sendDueReminders());

        verify(emailService, org.mockito.Mockito.times(2))
                .sendTrainingReminder(anyString(), anyString(), anyString(), any(), any());
    }

}