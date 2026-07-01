# 🏋️ Platforma Treningowa — Backend

REST API dla aplikacji webowej umożliwiającej sportowcom planowanie, rejestrowanie oraz analizowanie treningów tanecznych, gimnastycznych i ogólnorozwojowych. Inspirowane TrainingPeaks, ale skupione na potrzebach zawodnika amatorskiego i półprofesjonalnego.

![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)
![Docker](https://img.shields.io/badge/Docker-ready-blue)

---

## 📋 Spis treści

- [Opis projektu](#-opis-projektu)
- [Stack technologiczny](#-stack-technologiczny)
- [Architektura](#-architektura)
- [Wymagania](#-wymagania)
- [Uruchomienie](#-uruchomienie)
- [Dokumentacja API](#-dokumentacja-api)
- [Endpointy](#-endpointy)
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
- **JWT** (jjwt 0.12.5) — autoryzacja bezstanowa
- **MapStruct 1.6.3** — mapowanie encji na DTO
- **Lombok** — redukcja boilerplate
- **springdoc-openapi 3.x** — dokumentacja Swagger/OpenAPI

### Testy
- **JUnit 5** + **Mockito** — testy jednostkowe

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

---

## ✅ Wymagania

- **Docker** i **Docker Compose** (zalecane — uruchamia całe środowisko)

lub do uruchomienia lokalnego bez Dockera:

- **Java 25**
- **Maven 3.9+**
- **PostgreSQL 15**

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

> Przy pierwszym uruchomieniu automatycznie tworzone są 3 domyślne kategorie treningów: **Taniec**, **Gimnastyka**, **Ogólnorozwojowy**.

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

Do testów manualnych dołączona jest również kolekcja **Postman** (`trainingplatform.postman_collection.json`) z automatycznym zapisem tokenu po zalogowaniu.

---

## 🔌 Endpointy

### Autoryzacja (`/auth`)

| Metoda | Endpoint | Opis | Dostęp |
|--------|----------|------|--------|
| `POST` | `/auth/register` | Rejestracja nowego użytkownika | Publiczny |
| `POST` | `/auth/login` | Logowanie (zwraca token JWT) | Publiczny |

### Profil użytkownika (`/profile`)

| Metoda | Endpoint | Opis | Dostęp |
|--------|----------|------|--------|
| `GET` | `/profile` | Pobranie profilu | USER |
| `PUT` | `/profile` | Aktualizacja danych | USER |
| `POST` | `/profile/change-password` | Zmiana hasła | USER |

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

---

## 🗄 Model danych

| Tabela | Opis |
|--------|------|
| `users` | Konta użytkowników (z rolami USER/ADMIN) |
| `workout_categories` | Słownik kategorii treningów |
| `training_plans` | Zaplanowane treningi (kalendarz) |
| `workout_logs` | Dziennik wykonanych treningów |

### Kluczowe relacje

- Użytkownik ma wiele planów, wpisów w dzienniku i celów
- Każdy plan i wpis należy do jednej kategorii
- Wpis w dzienniku może być opcjonalnie powiązany z planem (lub dodany ad-hoc)
- Kategoria nie może zostać usunięta, jeśli jest używana przez plany lub wpisy (reguła BR-06)

### Statusy planu (`PlanStatus`)

`PLANNED` → `COMPLETED` / `SKIPPED` / `CANCELLED`

---

## 🧪 Testy

Uruchomienie wszystkich testów:

```bash
./mvnw test
```

Projekt zawiera testy jednostkowe warstwy serwisowej (JUnit 5 + Mockito) pokrywające:

- Logikę rejestracji i logowania
- Zarządzanie profilem i zmianę hasła
- CRUD kategorii z walidacją unikalności i ochroną przed usunięciem używanej kategorii
- CRUD planów treningowych z walidacją własności (403/404)
- CRUD dziennika z obsługą wpisów ad-hoc i powiązań z planami

---

## 👤 Autor

**Tomasz Osuch**
🌐 [tomaszosuch.dev](https://tomaszosuch.dev)

---

## 📄 Licencja

Projekt na licencji Apache 2.0.