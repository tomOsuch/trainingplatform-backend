# 🏋️ Platforma Treningowa — Backend

REST API dla aplikacji webowej umożliwiającej sportowcom planowanie, rejestrowanie oraz analizowanie treningów tanecznych, gimnastycznych i ogólnorozwojowych. Inspirowane TrainingPeaks, ale skupione na potrzebach zawodnika amatorskiego i półprofesjonalnego.

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Flyway](https://img.shields.io/badge/Flyway-migrations-red)
![Docker](https://img.shields.io/badge/Docker-ready-blue)

---

## 📋 Spis treści

- [Opis projektu](#-opis-projektu)
- [Stack technologiczny](#-stack-technologiczny)
- [Architektura](#-architektura)
- [Wymagania](#-wymagania)
- [Uruchomienie](#-uruchomienie)
- [Konfiguracja i sekrety](#-konfiguracja-i-sekrety)
- [Dostęp do aplikacji](#-dostęp-do-aplikacji)
- [Migracje bazy danych](#-migracje-bazy-danych)
- [Dokumentacja API](#-dokumentacja-api)
- [Endpointy](#-endpointy)
- [Obsługa błędów](#️-obsługa-błędów)
- [Model danych](#-model-danych)
- [Testy](#-testy)

---

## 🎯 Opis projektu

Aplikacja zastępuje prowadzenie dziennika treningowego w formie papierowej lub w arkuszach kalkulacyjnych. Umożliwia:

- **Planowanie treningów** w kalendarzu z podziałem na kategorie
- **Rejestrowanie wykonanych sesji** wraz z czasem trwania, intensywnością i notatkami
- **Śledzenie postępów** i analizę aktywności
- **Zarządzanie profilem** użytkownika, z możliwością usunięcia konta wraz z danymi

Aplikacja jest **zamknięta** — konto można założyć wyłącznie na podstawie zaproszenia wystawionego przez administratora.

Projekt realizowany w architekturze warstwowej z naciskiem na czysty, testowalny kod i standardy branżowe.

---

## 🛠 Stack technologiczny

### Backend
- **Java 25** (LTS)
- **Spring Boot 4.0.6** — Spring Web, Spring Data JPA, Spring Security, Validation, Mail
- **PostgreSQL 15** — baza danych
- **Flyway** — wersjonowane migracje schematu bazy
- **JWT** (jjwt 0.12.5) — autoryzacja bezstanowa
- **MapStruct 1.6.3** — mapowanie encji na DTO
- **Thymeleaf** — szablony wiadomości e-mail
- **Lombok** — redukcja boilerplate
- **springdoc-openapi 3.x** — dokumentacja Swagger/OpenAPI

### Testy
- **JUnit 5** + **Mockito** — testy jednostkowe warstwy serwisowej
- **MockMvc** (`@WebMvcTest`) — testy warstwy kontrolerów
- **Testcontainers 1.21.4** — test kontekstu na realnym PostgreSQL w kontenerze

### Infrastruktura
- **Docker** + **Docker Compose** — aplikacja, baza, pgAdmin, Mailpit
- **Maven** — zarządzanie zależnościami

---

## 🏗 Architektura

```
Controller  →  Service (interfejs + impl)  →  Repository  →  Baza danych
                      ↓
                   Mapper (MapStruct)
                      ↓
                     DTO
```

### Zastosowane wzorce i konwencje

- **Separacja interfejs + implementacja** dla serwisów (`AuthService` + `AuthServiceImpl`)
- **DTO jako Java records** — niezmienne obiekty transferu danych
- **MapStruct** — automatyczne mapowanie encji na DTO w czasie kompilacji
- **Dedykowane wyjątki** per encja, mapowane w `@RestControllerAdvice`
- **Walidacja własności zasobów** — użytkownik operuje wyłącznie na swoich danych
- **Autoryzacja oparta na rolach** (`USER`, `ADMIN`) z `@PreAuthorize`
- **Transakcyjność na poziomie serwisu** — `@Transactional` na klasach implementacji, `open-in-view: false`
- **Wymienne implementacje wybierane właściwością** — `EmailService` ma wariant logujący i SMTP-owy
- **Konfiguracja przez `@ConfigurationProperties` z walidacją** — brakująca wartość zatrzymuje start aplikacji zamiast po cichu podstawiać wartość domyślną

### Struktura pakietów

```
pl.tomaszosuch.trainingplatform_backend
├── config          # Security, OpenAPI, DataInitializer, AdminBootstrap, klasy *Properties
├── controller      # Kontrolery REST
├── dto
│   ├── request     # DTO żądań (z walidacją)
│   └── response    # DTO odpowiedzi
├── entity          # Encje JPA
├── enums           # Role, PlanStatus, InvitationStatus
├── exception       # Wyjątki + GlobalExceptionHandler
├── mapper          # Mappery MapStruct
├── repository      # Repozytoria Spring Data JPA
├── security        # JWT, SecureTokenGenerator
└── service         # Logika biznesowa (interfejsy + impl/)
```

Migracje w `src/main/resources/db/migration/`, szablony maili w `templates/mail/`, skrypty pomocnicze w `scripts/`.

---

## ✅ Wymagania

- **Docker** i **Docker Compose**
- **openssl** — do wygenerowania klucza JWT

Do uruchomienia bez Dockera dodatkowo: **Java 25**, **Maven 3.9+**, **PostgreSQL 15**.

> Docker jest wymagany również do uruchomienia pełnego zestawu testów — test kontekstu startuje bazę w kontenerze (Testcontainers).

---

## 🚀 Uruchomienie

```bash
git clone https://github.com/<twoj-username>/trainingplatform-backend.git
cd trainingplatform-backend

./scripts/setup-env.sh
docker compose up -d --build
```

Skrypt tworzy `.env` na podstawie `.env.example` i **generuje sekrety** — klucz JWT (64 losowe bajty) oraz hasła. Wypisze hasło administratora; **zapisz je**, bo na nim powstanie konto przy pierwszym starcie.

Skrypt nie nadpisze istniejącego `.env`. Aby wygenerować od nowa: `rm .env && ./scripts/setup-env.sh`.

Dostępne po starcie:

| Adres | Co to |
|---|---|
| `http://localhost:8080/api` | API |
| `http://localhost:8080/api/swagger-ui.html` | Swagger UI (wyłączony w profilu `prod`) |
| `http://localhost:8025` | **Mailpit** — skrzynka przechwytująca maile |
| `http://localhost:5050` | pgAdmin |

> Przy pierwszym uruchomieniu Flyway tworzy schemat, `DataInitializer` dodaje 3 domyślne kategorie (**Taniec**, **Gimnastyka**, **Ogólnorozwojowy**), a `AdminBootstrap` zakłada konto administratora.

---

## 🔐 Konfiguracja i sekrety

**W repozytorium nie ma żadnych haseł ani kluczy.** Cała konfiguracja wrażliwa pochodzi ze zmiennych środowiskowych wczytywanych z `.env`, który jest w `.gitignore`.

`docker-compose.yml` korzysta z `.env` na dwa sposoby, które łatwo pomylić:

- **podstawienie w treści pliku** (`${DB_NAME}`) — robi je Docker Compose przed startem, czytając `.env` automatycznie
- **`env_file: .env`** — wstrzykuje zmienne do wnętrza kontenera; dzięki temu `JWT_SECRET`, `ADMIN_*`, `MAIL_*` docierają do aplikacji, mimo że nie ma ich w bloku `environment`

### Kluczowe zmienne

| Zmienna | Znaczenie |
|---|---|
| `JWT_SECRET` | Klucz podpisujący tokeny. **Wymagany** — brak zatrzymuje start. Minimum 64 bajty |
| `JWT_EXPIRATION` | Czas życia tokenu w ms (domyślnie 24 h) |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Konto administratora zakładane przy pierwszym starcie |
| `MAIL_PROVIDER` | `log` (treść do konsoli) albo `smtp` (realna wysyłka) |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_FROM` | Konfiguracja SMTP |
| `INVITATION_EXPIRATION_DAYS` | Ważność zaproszenia (domyślnie 7 dni) |
| `INVITATION_ACCEPT_BASE_URL` | Adres strony rejestracji frontendu — dokleja się `?token=…` |
| `PASSWORD_RESET_EXPIRATION_MINUTES` | Ważność linku resetu (domyślnie 60 minut) |
| `PASSWORD_RESET_BASE_URL` | Adres strony resetu hasła |
| `SPRING_PROFILES_ACTIVE` | `dev` (logi DEBUG, Swagger) albo `prod` (logi WARN, Swagger wyłączony) |

```bash
openssl rand -base64 64 | tr -d '\n'
```

Podmiana klucza JWT unieważnia wszystkie wydane tokeny — użytkownicy muszą zalogować się ponownie.

**Klasy `*Properties` są walidowane** (`@Validated`): brak `INVITATION_ACCEPT_BASE_URL` albo `expirationMinutes = 0` zatrzyma start aplikacji z komunikatem wskazującym właściwość. Głośna awaria przy starcie jest zawsze lepsza od cichej awarii w działaniu.

---

## 🚪 Dostęp do aplikacji

Rejestracja jest **zamknięta**. Konto może powstać wyłącznie z ważnego zaproszenia.

```
Administrator                 Zapraszany
     │                             │
     │ POST /invitations           │
     │   { email, role }           │
     │────────────────────►        │
     │                             │
     │        e-mail z linkiem     │
     │        ?token=…             │
     │────────────────────────────►│
     │                             │
     │                             │ GET /auth/invitation?token=…
     │                             │   → adres i termin ważności
     │                             │
     │                             │ POST /auth/register
     │                             │   { …, token }
     │                             │   → konto z rolą z zaproszenia
```

### Pierwszy administrator

Rejestracja nadaje rolę z zaproszenia, a zaproszenia wystawia tylko administrator — więc pierwszy musi powstać poza tym obiegiem. Robi to `AdminBootstrap` przy starcie:

1. jeśli w bazie jest **jakiekolwiek** konto z rolą `ADMIN` — nic się nie dzieje
2. jeśli nie ma, a brakuje `ADMIN_EMAIL` / `ADMIN_PASSWORD` — ostrzeżenie w logu, start bez seedu
3. jeśli konto o podanym adresie istnieje — zostaje **promowane** do `ADMIN`
4. jeśli nie istnieje — zostaje **utworzone**

Warunkiem jest brak jakiegokolwiek administratora, nie brak konkretnego adresu. Dzięki temu zmiana `ADMIN_EMAIL` nie produkuje drugiego konta administratora.

### Zasady zaproszeń

- Token jawny istnieje **wyłącznie w treści maila** — w bazie leży jego skrót SHA-256
- Jeden adres może mieć tylko jedno **oczekujące** zaproszenie; wystawienie nowego unieważnia poprzednie
- Nie można zaprosić adresu, który ma już konto (`409`)
- Rola nowego konta pochodzi **z rekordu zaproszenia**, nigdy z ciała żądania rejestracji
- Rejestracja zapisuje adres z zaproszenia; pole `email` w żądaniu służy tylko do potwierdzenia
- Zaproszenie jest jednorazowe — po rejestracji token jest spalony

### Reset hasła

Ten sam mechanizm tokenów, z krótszym terminem: **60 minut** zamiast 7 dni. Reset robi się od razu, a długi termin to niepotrzebnie szerokie okno dla kogoś, kto przejmie skrzynkę.

`POST /auth/password-reset` zwraca **`202` niezależnie od tego, czy konto istnieje**. Gdyby nieznany adres dawał `404`, formularz „zapomniałem hasła" stałby się narzędziem do sprawdzania, kto ma konto w systemie.

⚠️ **Znane ograniczenie:** tokeny JWT wydane przed resetem pozostają ważne do wygaśnięcia. Zmiana hasła nie kończy istniejących sesji — wymaga to wersjonowania tokenów i należy do zakresu A5.

### Usunięcie konta

`DELETE /profile` z potwierdzeniem hasłem. Usuwa konto wraz z planami, wpisami dziennika i tokenami resetu — kaskadą na poziomie bazy.

Dwie reguły:

- **Ostatniego administratora nie da się usunąć** (`403`). Gdyby jedyny admin skasował konto, nikt już nikogo nie zaprosi i aplikacja zamyka się na głucho
- **Zaproszenia przeżywają usunięcie swojego autora.** `invitation.invited_by` przechodzi na `NULL`, bo rekord dokumentuje, jak do systemu trafił ktoś inny — to nie są dane usuwanego użytkownika. Jego **niezużyte** zaproszenia są przy okazji unieważniane

Błędne hasło daje **`400`, nie `401`** — użytkownik jest zalogowany, a token ważny; `401` skłoniłoby przechwytywacz na froncie do wylogowania go za literówkę.

### Wysyłka maili

| `app.mail.provider` | Zachowanie |
|---|---|
| `log` (domyślnie) | Treść trafia do logu aplikacji w obramowanym bloku. Nic nie wychodzi na zewnątrz — bezpieczne dla testów i CI |
| `smtp` | Realna wysyłka. Lokalnie do **Mailpita** (`http://localhost:8025`) |

Mailpit jest częścią `docker-compose.yml`, przyjmuje pocztę na porcie 1025 i **niczego nie przekazuje dalej**. Przejście na dostawcę produkcyjnego to zmiana zmiennych `MAIL_*`, bez zmian w kodzie.

Niepowodzenie wysyłki **nie wywraca operacji** — zaproszenie zostaje zapisane z pustym `sent_at`, a błąd trafia do logu.

---

## 🗃 Migracje bazy danych

Schemat jest wersjonowany przez **Flyway**. Hibernate pracuje w trybie `ddl-auto: validate` — **nie tworzy ani nie zmienia tabel**, tylko sprawdza przy starcie zgodność encji ze schematem.

```
src/main/resources/db/migration/
├── V1__baseline.sql                               # snapshot schematu po Fazie 1
├── V2__composite_indexes_and_birth_date_type.sql  # indeksy złożone + birth_date → DATE
├── V3__invitations.sql                            # tabela invitation
├── V4__password_reset_tokens.sql                  # tabela password_reset_token
└── V5__account_deletion_cascades.sql              # ON DELETE CASCADE / SET NULL
```

### Jak dodać nową migrację

1. Utwórz plik `V<numer>__krotki_opis.sql`
2. Numer większy od ostatniego; opis w `snake_case` po podwójnym podkreśleniu
3. Uruchom aplikację — Flyway wykona migrację i dopisze wpis do `flyway_schema_history`

> **Plików już zastosowanych się nie edytuje** — nawet formatowania. Flyway trzyma sumę kontrolną i przerwie start przy niezgodności. Poprawki wprowadza się kolejną migracją.

```sql
SELECT version, description, success, execution_time
FROM flyway_schema_history
ORDER BY installed_rank;
```

---

## 📖 Dokumentacja API

Swagger UI: `http://localhost:8080/api/swagger-ui.html` *(wyłączony w profilu `prod`)*

Dołączona jest kolekcja **Postman** (`trainingplatform.postman_collection.json`) — 13 folderów, 103 requesty, 216 asercji. Przechodzi pełne przepływy: zaproszenie → rejestracja → reset hasła → usunięcie konta.

⚠️ Kolekcja **odczytuje tokeny z maili przez API Mailpita**, bo token jawny nie wraca z backendu. Wymaga `MAIL_PROVIDER=smtp` i uruchomionego środowiska. Folder **12 usuwa konto testowe**, więc uruchamiaj całość od folderu `0. Setup`.

---

## 🔌 Endpointy

### Autoryzacja (`/auth`)

| Metoda | Endpoint | Opis | Dostęp |
|--------|----------|------|--------|
| `POST` | `/auth/register` | Rejestracja z tokenem zaproszenia | Publiczny |
| `POST` | `/auth/login` | Logowanie (zwraca token JWT) | Publiczny |
| `GET` | `/auth/invitation?token=` | Sprawdzenie zaproszenia przed formularzem | Publiczny |
| `POST` | `/auth/password-reset` | Żądanie resetu hasła | Publiczny |
| `GET` | `/auth/password-reset?token=` | Sprawdzenie tokenu resetu | Publiczny |
| `POST` | `/auth/password-reset/confirm` | Ustawienie nowego hasła | Publiczny |

> `POST /auth/register` wymaga pola `token` i zwraca `201 Created`. Rola konta pochodzi z zaproszenia.
> `POST /auth/password-reset` zwraca `202 Accepted` **zawsze**, także dla nieistniejącego adresu.
> `POST /auth/password-reset/confirm` zwraca `204 No Content`.

### Zaproszenia (`/invitations`)

| Metoda | Endpoint | Opis | Dostęp |
|--------|----------|------|--------|
| `POST` | `/invitations` | Wystawienie (`{ email, role? }`) | ADMIN |
| `GET` | `/invitations` | Lista z wyliczonym statusem | ADMIN |
| `DELETE` | `/invitations/{id}` | Unieważnienie | ADMIN |

> `role` opcjonalne, domyślnie `USER`. Odpowiedź **nigdy** nie zawiera tokenu ani jego skrótu.

### Profil użytkownika (`/profile`)

| Metoda | Endpoint | Opis | Dostęp |
|--------|----------|------|--------|
| `GET` | `/profile` | Pobranie profilu | USER |
| `PUT` | `/profile` | Aktualizacja danych | USER |
| `POST` | `/profile/change-password` | Zmiana hasła | USER |
| `DELETE` | `/profile` | Usunięcie konta (`{ password }`) | USER |

> `UserResponse` zawiera `birthDate` w formacie `yyyy-MM-dd`.

### Kategorie treningów (`/workout-categories`)

| Metoda | Endpoint | Opis | Dostęp |
|--------|----------|------|--------|
| `GET` | `/workout-categories` | Lista kategorii | USER |
| `GET` | `/workout-categories/{id}` | Szczegóły kategorii | USER |
| `POST` | `/workout-categories` | Utworzenie kategorii | ADMIN |
| `PUT` | `/workout-categories/{id}` | Edycja kategorii | ADMIN |
| `DELETE` | `/workout-categories/{id}` | Usunięcie kategorii | ADMIN |

> `POST /workout-categories` zwraca `201 Created`. Endpoint jest dostępny wyłącznie dla roli ADMIN; kategorie startowe zakłada `DataInitializer` przy pierwszym uruchomieniu.

### Plany treningowe (`/training-plans`)

| Metoda | Endpoint | Opis | Dostęp |
|--------|----------|------|--------|
| `GET` | `/training-plans?from=&to=` | Plany użytkownika (opcjonalny zakres dat) | USER |
| `GET` | `/training-plans/{id}` | Szczegóły planu | USER |
| `POST` | `/training-plans` | Utworzenie planu | USER |
| `PUT` | `/training-plans/{id}` | Edycja planu | USER |
| `PATCH` | `/training-plans/{id}/status` | Zmiana statusu | USER |
| `DELETE` | `/training-plans/{id}` | Usunięcie planu | USER |

### Dziennik treningów (`/workout-logs`)

| Metoda | Endpoint | Opis | Dostęp |
|--------|----------|------|--------|
| `GET` | `/workout-logs?categoryId=&from=&to=` | Wpisy z filtrowaniem | USER |
| `GET` | `/workout-logs/{id}` | Szczegóły wpisu | USER |
| `POST` | `/workout-logs` | Dodanie wpisu (również ad-hoc) | USER |
| `PUT` | `/workout-logs/{id}` | Edycja wpisu | USER |
| `DELETE` | `/workout-logs/{id}` | Usunięcie wpisu | USER |

> Wpis zawiera opcjonalny `title` oraz `performedTime` — godzinę rozpoczęcia w formacie `HH:mm`.

---

## ⚠️ Obsługa błędów

Wszystkie błędy w jednolitym formacie JSON:

```json
{
  "timestamp": "2026-08-27T10:15:30.123",
  "status": 404,
  "message": "Nie znaleziono zaproszenia o identyfikatorze 42"
}
```

| Kod | Kiedy występuje | Przykład |
|-----|-----------------|----------|
| `400` | Błąd walidacji, niepoprawne dane, nieważny token zaproszenia lub resetu, błędne hasło przy usuwaniu konta | brak tokenu, link wygasły lub wykorzystany |
| `401` | Brak/nieważny token JWT albo błędne dane logowania | żądanie bez nagłówka `Authorization` |
| `403` | Brak uprawnień do zasobu; próba usunięcia ostatniego administratora | USER na endpoincie ADMIN |
| `404` | Zasób nie istnieje | `GET /training-plans/9999` |
| `409` | Konflikt danych | adres ma już konto, unieważnienie wykorzystanego zaproszenia |
| `500` | Nieoczekiwany błąd serwera | — |

Przy błędach walidacji odpowiedź zawiera dodatkowo mapę `errors` z komunikatem per pole:

```json
{
  "timestamp": "2026-08-27T10:15:30.123",
  "status": 400,
  "message": "Błąd walidacji",
  "errors": {
    "email": "Niepoprawny format adresu e-mail",
    "token": "Token zaproszenia jest wymagany"
  }
}
```

### Uwagi

- **Rozróżnienie 401 / 403.** `401` oznacza „nie wiem, kim jesteś" — brak tokenu, token wygasły lub błędne dane logowania. `403` oznacza „wiem, kim jesteś, ale nie wolno ci". Żądanie bez tokenu na chroniony endpoint zwraca `401` z ciałem JSON, nie puste `403`.
- **Nieważny token to `400`, nie `404`** — z komunikatem rozróżniającym przypadki („nie istnieje", „wygasło", „zostało już wykorzystane", „zostało unieważnione"). Frontend wyświetla `message` z odpowiedzi.
- **Komunikaty nie zdradzają istnienia kont.** Logowanie zwraca ten sam tekst dla nieistniejącego adresu i złego hasła; żądanie resetu zwraca `202` niezależnie od tego, czy konto istnieje.
- **Dwa różne `403`.** `Brak uprawnień` to odmowa autoryzacji — komunikat celowo nic nie mówi. `Nie można usunąć konta ostatniego administratora` to reguła biznesowa — tu komunikat pomaga, bo użytkownik może zaprosić drugiego administratora i spróbować ponownie.
- **Komunikaty są po polsku** i nadają się do pokazania użytkownikowi bez tłumaczenia.

---

## 🗄 Model danych

| Tabela | Opis |
|--------|------|
| `users` | Konta użytkowników (rola USER/ADMIN, `birth_date` typu `DATE`) |
| `invitation` | Zaproszenia (`token_hash`, `role`, `expires_at`, `sent_at`, `used_at`, `revoked_at`) |
| `password_reset_token` | Jednorazowe tokeny resetu hasła (`token_hash`, `expires_at`, `used_at`) |
| `workout_category` | Słownik kategorii treningów |
| `training_plan` | Zaplanowane treningi (kalendarz) |
| `workout_log` | Dziennik wykonanych treningów (`title`, `performed_date`, `performed_time`) |
| `flyway_schema_history` | Historia migracji — tabela techniczna Flyway |

### Kluczowe relacje

- Użytkownik ma wiele planów treningowych i wpisów w dzienniku
- Każdy plan i wpis należy do jednej kategorii
- Wpis w dzienniku może być opcjonalnie powiązany z planem (lub dodany ad-hoc)
- Kategoria nie może zostać usunięta, jeśli jest używana przez plany lub wpisy (BR-06)
- Usunięcie planu odpina powiązane wpisy (`plan_id` → `NULL`), nie kasuje ich

### Zachowanie kluczy obcych przy usuwaniu konta

| Relacja | Akcja | Dlaczego |
|---|---|---|
| `training_plan.user_id` | `CASCADE` | dane użytkownika |
| `workout_log.user_id` | `CASCADE` | dane użytkownika |
| `password_reset_token.user_id` | `CASCADE` | efemeryda |
| `invitation.invited_by` | **`SET NULL`** | rekord dokumentuje, jak do systemu trafił **ktoś inny** |

### Status zaproszenia nie jest kolumną

`InvitationStatus` wylicza się ze stanu pól, w tej kolejności:

| Warunek | Status |
|---|---|
| `used_at` niepuste | `ACCEPTED` |
| `revoked_at` niepuste | `REVOKED` |
| `expires_at` w przeszłości | `EXPIRED` |
| pozostałe | `PENDING` |

Kolejność jest istotna: zaproszenie wykorzystane pozostaje `ACCEPTED` nawet po upływie terminu, bo minął on *po* rejestracji. Brak osobnej kolumny oznacza, że status nie ma jak rozjechać się z faktami.

### Indeksy

- `idx_training_plan_user_planned_date` — `training_plan (user_id, planned_date)`
- `idx_workout_log_user_performed_date` — `workout_log (user_id, performed_date)`
- `idx_workout_log_user_category_performed_date` — `workout_log (user_id, category_id, performed_date)`
- `idx_invitation_pending_email` — **częściowy indeks unikalny** na `invitation (email) WHERE used_at IS NULL AND revoked_at IS NULL`; gwarantuje jedno oczekujące zaproszenie na adres
- `idx_password_reset_token_user` — `password_reset_token (user_id)`

> Przy tokenach resetu **nie ma** analogicznego indeksu częściowego. Wygaśnięcie nie zmienia żadnej kolumny, więc `WHERE used_at IS NULL` uznawałby wygasły token za wciąż oczekujący i blokował kolejne żądanie na zawsze. Jedno aktywne żądanie zapewnia serwis, kasując poprzednie tokeny.

### Statusy planu (`PlanStatus`)

`PLANNED` → `COMPLETED` / `SKIPPED` / `CANCELLED`

---

## 🧪 Testy

```bash
./mvnw test
```

Projekt zawiera **190 testów** w trzech warstwach:

**Testy jednostkowe serwisów** (JUnit 5 + Mockito):

- Rejestracja związana z zaproszeniem, logowanie, bootstrap administratora
- Cykl życia zaproszenia: generowanie tokenu, unieważnianie poprzedniego, wygaśnięcie, jednorazowość
- Reset hasła: jednorazowość tokenu, brak zdradzania istnienia konta, awaria wysyłki
- Usuwanie konta: potwierdzenie hasłem, ochrona ostatniego administratora, kolejność operacji
- Zarządzanie profilem i zmiana hasła
- CRUD kategorii, planów i dziennika z walidacją własności

**Testy kontrolerów** (`@WebMvcTest` + MockMvc) — mapowanie ścieżek, kody odpowiedzi, kontrola roli ADMIN, walidacja żądań.

**Test kontekstu** (`@SpringBootTest` + Testcontainers) — podnosi aplikację na realnym PostgreSQL 15 i wykonuje pełny łańcuch migracji od zera. Wymaga uruchomionego Dockera.

> Testy nie używają H2 — baza w testach jest tą samą bazą co na produkcji. Testy nigdy nie wysyłają maili: `app.mail.provider` domyślnie stoi na `log`.

⚠️ Testy jednostkowe **nie sprawdzą transakcyjności ani kaskad w bazie** — atrapy repozytoriów nie mają transakcji, a `verify(repository).delete(...)` nie mówi nic o tym, co zrobił Postgres. Te ścieżki weryfikuje kolekcja Postmana i test kontekstu.

---

## 👤 Autor

**Tomasz Osuch**
🌐 [tomaszosuch.dev](https://tomaszosuch.dev)

---

## 📄 Licencja

Projekt na licencji Apache 2.0.