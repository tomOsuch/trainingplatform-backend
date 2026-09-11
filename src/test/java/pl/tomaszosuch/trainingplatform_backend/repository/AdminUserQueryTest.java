package pl.tomaszosuch.trainingplatform_backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import pl.tomaszosuch.trainingplatform_backend.dto.response.AdminUserResponse;
import pl.tomaszosuch.trainingplatform_backend.dto.response.PageResponse;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.AccountStatus;
import pl.tomaszosuch.trainingplatform_backend.enums.AdminUserSort;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;
import pl.tomaszosuch.trainingplatform_backend.mapper.UserMapperImpl;
import pl.tomaszosuch.trainingplatform_backend.service.AdminUserService;
import pl.tomaszosuch.trainingplatform_backend.service.impl.AdminUserServiceImpl;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AdminUserServiceImpl.class, UserMapperImpl.class})
@DisplayName("AdminUserQueryTest")
class AdminUserQueryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private TestEntityManager em;

    @Autowired
    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        // Kolejność zapisu ma znaczenie dla sortowania NEWEST.
        em.persist(user("Jan", "Kowalski", "jan.kowalski@example.com", true));
        em.persist(user("Anna", "Nowak", "anna_nowak@example.com", true));
        em.persist(user("Ewa", "Kowalska", "ewa@firma.pl", false));
        em.persist(user("Piotr", "Wiśniewski", "piotr@kowal.pl", true));
        em.flush();
    }

    private static User user(String firstName, String lastName, String email, boolean active) {
        return User.builder()
                .email(email).password("hash").firstName(firstName).lastName(lastName)
                .role(Role.USER).isActive(active)
                .build();
    }

    private List<String> lastNames(PageResponse<AdminUserResponse> page) {
        return page.content().stream().map(AdminUserResponse::lastName).toList();
    }

    private PageResponse<AdminUserResponse> find(String search, AccountStatus status) {
        return adminUserService.findUsers(search, status, AdminUserSort.LAST_NAME, 0, 20);
    }

    @Test
    @DisplayName("szuka w nazwisku i adresie bez względu na wielkość liter")
    void shouldSearchLastNameAndEmailIgnoringCase() {
        assertEquals(List.of("Kowalska", "Kowalski", "Wiśniewski"), lastNames(find("KOWAL", null)));
    }

    @Test
    @DisplayName("filtr stanu: wyłączone, aktywne i brak filtra")
    void shouldFilterByAccountStatus() {
        assertEquals(List.of("Kowalska"), lastNames(find(null, AccountStatus.INACTIVE)));
        assertEquals(List.of("Kowalski", "Wiśniewski"), lastNames(find("kowal", AccountStatus.ACTIVE)));
        assertEquals(4, find(null, null).totalElements());
    }

    @Test
    @DisplayName("podkreślnik w wyszukiwaniu jest znakiem, nie symbolem wieloznacznym")
    void shouldTreatUnderscoreLiterally() {
        assertEquals(List.of("Nowak"), lastNames(find("_", null)));
    }

    @Test
    @DisplayName("puste wyszukiwanie zwraca wszystkich")
    void shouldReturnEveryoneForBlankSearch() {
        assertEquals(4, find("   ", null).totalElements());
    }

    @Test
    @DisplayName("NEWEST: najpóźniej zarejestrowani na początku")
    void shouldSortNewestFirst() {
        PageResponse<AdminUserResponse> page =
                adminUserService.findUsers(null, null, AdminUserSort.NEWEST, 0, 20);

        assertEquals(List.of("Wiśniewski", "Kowalska", "Nowak", "Kowalski"), lastNames(page));
    }

    @Test
    @DisplayName("stronicowanie: druga strona i liczniki")
    void shouldPaginate() {
        PageResponse<AdminUserResponse> page =
                adminUserService.findUsers(null, null, AdminUserSort.LAST_NAME, 1, 2);

        assertEquals(List.of("Nowak", "Wiśniewski"), lastNames(page));
        assertEquals(1, page.page());
        assertEquals(4, page.totalElements());
        assertEquals(2, page.totalPages());
    }
}