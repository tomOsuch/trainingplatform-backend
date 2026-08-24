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
- **Zarządzanie profilem** użytkownika

Projekt realizowany w architekturze warstwowej z naciskiem na czysty, testowalny kod i standardy branżowe.

---

## 🛠 Stack technologiczny

### Backend
- **Java 25** (LTS)
- **Spring Boot 4.0.6** — Spring Web, Spring Data JPA, Spring Security, Validation
- **PostgreSQL 15** — baza danych
- **Flyway** — wersjonowane migracje schematu bazy
- **JWT** (jjwt 0.12.5) — autoryzacja bezstanowa
- **MapStruct 1.6.3** — mapowanie encji na DTO
- **Lombok** — redukcja boilerplate
- **springdoc-openapi 3.x** — dokumentacja Swagger/OpenAPI

### Testy
- **JUnit 5** + **Mockito** — testy jednostkowe warstwy serwisowej
- **MockMvc** (`@WebMvcTest`) — testy warstwy kontrolerów
- **Testcontainers 1.21.4** — test kontekstu na realnym PostgreSQL w kontenerze

### Infrastruktura
- **Docker** + **Docker Compose** — konteneryzacja (aplikacja, baza, pgAdmin)
- **Maven** — zarządzanie zależnościami

---

## 🏗 Architektura

Projekt korzysta z klasycznej architektury warstwowej:

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
- **Dedykowane wyjątki** per encja (`UserNotFoundException`, `TrainingPlanNotFoundException`)
- **Globalna obsługa błędów** przez `@RestControllerAdvice`
- **Walidacja własności zasobów** — użytkownik operuje wyłącznie na swoich danych (403 przy próbie dostępu do cudzych)
- **Autoryzacja oparta na rolach** (`USER`, `ADMIN`) z `@PreAuthorize`
- **Transakcyjność na poziomie serwisu** — `@Transactional` na klasach implementacji, `open-in-view: false`

### Struktura pakietów

```
pl.tomaszosuch.trainingplatform_backend
├── config          # Konfiguracja (Security, OpenAPI, DataInitializer)
├── controller      # Kontrolery REST
├── dto
│   ├── request     # DTO żądań (z walidacją)
│   └── response    # DTO odpowiedzi
├── entity          # Encje JPA
├── enums           # Enumy (Role, PlanStatus)
├── exception       # Wyjątki + GlobalExceptionHandler
├── mapper          # Mappery MapStruct
├── repository      # Repozytoria Spring Data JPA
├── security        # JWT (Provider, Filter, UserDetailsService)
└── service         # Logika biznesowa (interfejsy + impl/)
```

Migracje bazy znajdują się w `src/main/resources/db/migration/`.

---

## ✅ Wymagania

- **Docker** i **Docker Compose** (zalecane — uruchamia całe środowisko)

lub do uruchomienia lokalnego bez Dockera:

- **Java 25**
- **Maven 3.9+**
- **PostgreSQL 15**

> Docker jest wymagany również do uruchomienia pełnego zestawu testów — test kontekstu aplikacji startuje bazę w kontenerze (Testcontainers).

---

## 🚀 Uruchomienie

### Z Dockerem (zalecane)

1. Sklonuj repozytorium:
   ```bash
   git clone https://github.com/<twoj-username>/trainingplatform-backend.git
   cd trainingplatform-backend
   ```

2. Skopiuj plik ze zmiennymi środowiskowymi:
   ```bash
   cp .env.example .env
   ```

3. Uruchom środowisko:
   ```bash
   docker compose up -d --build
   ```

4. Aplikacja będzie dostępna pod:
   - API: `http://localhost:8080/api`
   - Swagger UI: `http://localhost:8080/api/swagger-ui.html`
   - pgAdmin: `http://localhost:5050`

### Lokalnie (bez Dockera)

1. Uruchom PostgreSQL i utwórz bazę `training_platform`
2. Ustaw zmienne środowiskowe lub edytuj `application.yml`
3. Uruchom aplikację:
   ```bash
   ./mvnw spring-boot:run
   ```

> Przy pierwszym uruchomieniu Flyway tworzy schemat bazy, a `DataInitializer` dodaje 3 domyślne kategorie treningów: **Taniec**, **Gimnastyka**, **Ogólnorozwojowy**.

---

## 🗃 Migracje bazy danych

Schemat bazy jest wersjonowany przez **Flyway**. Hibernate pracuje w trybie `ddl-auto: validate` — **nie tworzy ani nie zmienia tabel**, tylko sprawdza przy starcie, czy encje zgadzają się z tym, co jest w bazie. Każda zmiana w encji wymaga dopisania migracji, inaczej aplikacja nie wstanie.

### Pliki migracji

```
src/main/resources/db/migration/
├── V1__baseline.sql                               # snapshot schematu po Fazie 1
└── V2__composite_indexes_and_birth_date_type.sql  # indeksy złożone + birth_date → DATE
```

### Jak dodać nową migrację

1. Utwórz plik `V<numer>__krotki_opis.sql` w `src/main/resources/db/migration/`
2. Numer musi być większy od ostatniego; po podwójnym podkreśleniu opis w `snake_case`
3. Uruchom aplikację — Flyway wykona migrację i dopisze wpis do `flyway_schema_history`

> **Plików już zastosowanych się nie edytuje.** Flyway trzyma sumę kontrolną każdej migracji i przerwie start aplikacji, gdy zawartość pliku zmieni się po wykonaniu. Poprawki wprowadza się zawsze kolejną migracją.

### Konfiguracja

| Ustawienie | Wartość | Znaczenie |
|------------|---------|-----------|
| `spring.flyway.enabled` | `true` | Migracje wykonują się przy starcie aplikacji |
| `spring.flyway.baseline-on-migrate` | `true` | Baza istniejąca sprzed wdrożenia Flyway dostaje wpis BASELINE zamiast wykonywać `V1` |
| `spring.flyway.baseline-version` | `1` | `V1` jest punktem wyjścia, nie jest odtwarzane na istniejących bazach |
| `spring.flyway.locations` | `classpath:db/migration` | Katalog z plikami migracji |
| `spring.jpa.hibernate.ddl-auto` | `validate` | Hibernate tylko weryfikuje zgodność encji ze schematem |

### Podgląd stanu migracji

```sql
SELECT version, description, success, execution_time
FROM flyway_schema_history
ORDER BY installed_rank;
```

---

## 📖 Dokumentacja API

Interaktywna dokumentacja Swagger/OpenAPI dostępna pod:

```
http://localhost:8080/api/swagger-ui.html
```

### Jak autoryzować żądania

1. Zarejestruj konto przez `POST /auth/register`
2. Zaloguj się przez `POST /auth/login` — otrzymasz token JWT
3. W Swagger UI kliknij **Authorize** i wklej token
4. Wszystkie żądania będą teraz autoryzowane

Do testów manualnych dołączona jest również kolekcja **Postman** (`trainingplatform.postman_collection.json`) z automatycznym zapisem tokenu po zalogowaniu — 10 folderów, 62 requesty, 128 asercji.

---

## 🔌 Endpointy

### Autoryzacja (`/auth`)

| Metoda | Endpoint | Opis | Dostęp |
|--------|----------|------|--------|
| `POST` | `/auth/register` | Rejestracja nowego użytkownika | Publiczny |
| `POST` | `/auth/login` | Logowanie (zwraca token JWT) | Publiczny |

> `POST /auth/register` zwraca **`201 Created`** wraz z `UserResponse` — tak samo jak `POST /training-plans` i `POST /workout-logs`.

### Profil użytkownika (`/profile`)

| Metoda | Endpoint | Opis | Dostęp |
|--------|----------|------|--------|
| `GET` | `/profile` | Pobranie profilu | USER |
| `PUT` | `/profile` | Aktualizacja danych | USER |
| `POST` | `/profile/change-password` | Zmiana hasła | USER |

> `UserResponse` zawiera pole `birthDate` w formacie `yyyy-MM-dd` (typ `LocalDate`, bez części czasowej).

### Kategorie treningów (`/workout-categories`)

| Metoda | Endpoint | Opis | Dostęp |
|--------|----------|------|--------|
| `GET` | `/workout-categories` | Lista wszystkich kategorii | USER |
| `GET` | `/workout-categories/{id}` | Szczegóły kategorii | USER |
| `POST` | `/workout-categories` | Utworzenie kategorii | ADMIN |
| `PUT` | `/workout-categories/{id}` | Edycja kategorii | ADMIN |
| `DELETE` | `/workout-categories/{id}` | Usunięcie kategorii | ADMIN |

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

> Wpis dziennika zawiera opcjonalny `title` (nazwa własna treningu) oraz `performedTime` — godzinę rozpoczęcia w formacie `HH:mm`.

---

## ⚠️ Obsługa błędów

Wszystkie błędy zwracane są w jednolitym formacie JSON:

```json
{
  "timestamp": "2026-08-24T10:15:30.123",
  "status": 404,
  "message": "Training plan with id 42 not found."
}
```

| Kod | Kiedy występuje | Przykład |
|-----|-----------------|----------|
| `400` | Błąd walidacji lub niepoprawne dane wejściowe | data planu z przeszłości, pusty `email` |
| `401` | Brak/nieważny token JWT albo błędne dane logowania | żądanie bez nagłówka `Authorization` |
| `403` | Użytkownik zalogowany, ale bez uprawnień do zasobu | USER na endpoincie ADMIN, cudzy plan treningowy |
| `404` | Zasób nie istnieje | `GET /training-plans/9999` |
| `409` | Operacja narusza spójność danych | e-mail już zajęty, usunięcie używanej kategorii |
| `500` | Nieoczekiwany błąd serwera | — |

Przy błędach walidacji odpowiedź zawiera dodatkowo mapę `errors` z komunikatem per pole:

```json
{
  "timestamp": "2026-08-24T10:15:30.123",
  "status": 400,
  "message": "Błąd walidacji",
  "errors": {
    "email": "Niepoprawny format adresu e-mail",
    "birthDate": "Data urodzenia musi być z przeszłości"
  }
}
```

### Uwagi

- **Rozróżnienie 401 / 403.** `401` oznacza „nie wiem, kim jesteś" — brak tokenu, token wygasły lub błędne dane logowania. `403` oznacza „wiem, kim jesteś, ale nie wolno ci" — np. próba odczytu cudzego planu. Żądanie bez tokenu na chroniony endpoint zwraca `401` z ciałem JSON (`AuthenticationEntryPoint` w `SecurityConfig`), nie puste `403`.
- **Logowanie nie zdradza, co poszło nie tak.** Nieistniejące konto i błędne hasło zwracają identyczny komunikat *„Nieprawidłowy e-mail lub hasło"* — zapobiega to enumeracji kont.
- **Komunikaty są po polsku** dla błędów generowanych przez aplikację (walidacja, uprawnienia, konflikty).

---

## 🗄 Model danych

| Tabela | Opis |
|--------|------|
| `users` | Konta użytkowników (rola USER/ADMIN, `birth_date` typu `DATE`) |
| `workout_category` | Słownik kategorii treningów |
| `training_plan` | Zaplanowane treningi (kalendarz) |
| `workout_log` | Dziennik wykonanych treningów (`title`, `performed_date`, `performed_time`) |
| `flyway_schema_history` | Historia migracji — tabela techniczna Flyway |

### Kluczowe relacje

- Użytkownik ma wiele planów treningowych i wpisów w dzienniku
- Każdy plan i wpis należy do jednej kategorii
- Wpis w dzienniku może być opcjonalnie powiązany z planem (lub dodany ad-hoc)
- Kategoria nie może zostać usunięta, jeśli jest używana przez plany lub wpisy (reguła BR-06)
- Usunięcie planu odpina powiązane wpisy dziennika (`plan_id` → `NULL`), nie kasuje ich

### Indeksy złożone

- `idx_training_plan_user_planned_date` — `training_plan (user_id, planned_date)`
- `idx_workout_log_user_performed_date` — `workout_log (user_id, performed_date)`
- `idx_workout_log_user_category_performed_date` — `workout_log (user_id, category_id, performed_date)`

Pokrywają dwa najczęstsze zapytania: kalendarz użytkownika w zakresie dat oraz dziennik filtrowany po kategorii.

### Statusy planu (`PlanStatus`)

`PLANNED` → `COMPLETED` / `SKIPPED` / `CANCELLED`

---

## 🧪 Testy

Uruchomienie wszystkich testów:

```bash
./mvnw test
```

Projekt zawiera 119 testów w trzech warstwach:

**Testy jednostkowe serwisów** (JUnit 5 + Mockito):

- Logikę rejestracji i logowania
- Zarządzanie profilem i zmianę hasła
- CRUD kategorii z walidacją unikalności i ochroną przed usunięciem używanej kategorii
- CRUD planów treningowych z walidacją własności (403/404)
- CRUD dziennika z obsługą wpisów ad-hoc i powiązań z planami

**Testy kontrolerów** (`@WebMvcTest` + MockMvc) — mapowanie ścieżek, kody odpowiedzi, walidacja żądań.

**Test kontekstu** (`@SpringBootTest` + Testcontainers) — podnosi aplikację na realnym PostgreSQL 15 w kontenerze Dockera i wykonuje pełny łańcuch migracji Flyway od zera. Wymaga uruchomionego Dockera.

> Testy nie używają H2 — baza w testach jest tą samą bazą co na produkcji, więc różnice w dialekcie SQL czy typach kolumn wychodzą na etapie testu, a nie na wdrożeniu.

---

## 👤 Autor

**Tomasz Osuch**
🌐 [tomaszosuch.dev](https://tomaszosuch.dev)

---

## 📄 Licencja

Projekt na licencji Apache 2.0.