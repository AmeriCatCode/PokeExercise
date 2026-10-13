# PokeExercise: Pokédex Sync

A full-stack application built around a **Java / Spring Boot REST API** that integrates with the public [PokeAPI](https://pokeapi.co/docs/v2). Users browse and inspect Pokémon, import selected Pokémon into a local PostgreSQL database, and enrich those local copies with their own fields (local name, local region, tags).

The backend follows **Clean Architecture** and is built with **TDD**. A **React** frontend consumes the API.

> 🚧 **Status:** Step 0 (project setup) and Step 1 (US-01, Pokémon catalog) are done. The other user stories are implemented step by step; see [PROJECT_PLAN.md](PROJECT_PLAN.md).

## Documentation

| Document | Content |
|---|---|
| [Project_description.md](Project_description.md) | Actors, features, concurrency scenarios, data model, use cases |
| [Requirements_and_Technologies.md](Requirements_and_Technologies.md) | Functional and technical requirements and technologies, per feature |
| [User_stories_imp.md](User_stories_imp.md) | TDD implementation plan per user story (tests written first) |
| [PROJECT_PLAN.md](PROJECT_PLAN.md) | Overall plan: architecture, API, implementation order, verification |

## Tech stack

| Area | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Gradle (Kotlin DSL), Spring `RestClient`, virtual threads |
| Data | PostgreSQL, Spring Data JPA, Flyway |
| Cache | Spring Cache + Caffeine |
| Security | Spring Security, JWT, BCrypt |
| Backend tests | JUnit 5, Mockito, AssertJ, MockMvc, WireMock, Testcontainers, ArchUnit, JaCoCo |
| Frontend | React 19, TypeScript, Vite, React Router, TanStack Query, React Hook Form + Zod, Tailwind CSS |
| Frontend tests | Vitest, React Testing Library, MSW |
| Delivery | Docker, Docker Compose, GitHub Actions |

## Repository layout

```
├── backend/                 Spring Boot API
│   └── src/main/java/com/example/pokedex/
│       ├── domain/          Business entities and rules (pure Java)
│       ├── application/     Use cases and ports (pure Java)
│       ├── infrastructure/  Adapters: PokeAPI client, persistence, security
│       └── web/             REST controllers, DTOs, error handling
├── frontend/                React SPA
│   └── src/
│       ├── app/             App shell, routes, layout
│       ├── api/             API client and server-state configuration
│       ├── features/        One folder per feature (catalog, auth, local)
│       └── test/            Test setup (MSW fake API)
├── docker-compose.yml       PostgreSQL for local development
└── .github/workflows/ci.yml Build and tests on every push
```

**Architecture rule:** dependencies point inward only (`web` / `infrastructure` → `application` → `domain`). The domain and application layers don't depend on Spring. `ArchitectureTest` enforces this on every build.

## Getting started

### Prerequisites

- Java 21
- Node.js 24+
- Docker (for PostgreSQL and for the Testcontainers integration tests)

Gradle doesn't need to be installed: use the wrapper (`./gradlew`).

### Run locally

```bash
docker compose up -d db
```

```bash
cd backend && ./gradlew bootRun
```

```bash
cd frontend && npm install && npm run dev
```

| URL | What |
|---|---|
| http://localhost:3000 | Frontend (proxies `/api` to the backend) |
| http://localhost:8080/swagger-ui.html | API documentation (Swagger UI) |
| http://localhost:8080/actuator/health | Backend health check |

### Configuration

| Variable | Default | Used by |
|---|---|---|
| `SERVER_PORT` | `8080` | Backend |
| `POKEAPI_BASE_URL` | `https://pokeapi.co/api/v2` | Backend |
| `POKEAPI_READ_TIMEOUT` | `5s` | Backend: a slower PokeAPI answer returns `502` |
| `POKEAPI_CACHE_MAX_SIZE` / `POKEAPI_CACHE_TTL` | `1000` / `24h` | Backend: PokeAPI response cache |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | `pokedex` / `pokedex` / `pokedex` | docker-compose (local development only) |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | Frontend dev server proxy |

## Tests

```bash
cd backend && ./gradlew test
```

The coverage report is written to `backend/build/reports/jacoco/test/html/index.html`.

```bash
cd frontend && npm test
```

Other frontend checks: `npm run lint`, `npm run typecheck`, `npm run build`.

## API

| Method & path | Access | Description |
|---|---|---|
| `GET /api/pokemon?page=0&size=20` | Public | US-01: catalog page (zero-based `page`, `size` 1–50, default 20). Each item has sprite, category, types, weight (kg) and abilities. `400` for invalid paging, `502` if PokeAPI is slow or down. |

How US-01 works: PokeAPI's list only returns names, so for each Pokémon of the page the backend fetches its details and its species (where the category lives). The Pokémon of a page are fetched **in parallel on virtual threads**, with timeouts. If any of them fails, the whole page returns `502`. Responses are cached (Caffeine) by Pokémon id. A name in any case ("Pikachu", "pikachu") points to the same entry, errors are never cached, and simultaneous requests for the same uncached Pokémon make a single PokeAPI call.

## API conventions

Every error uses the same JSON body:

```json
{
  "status": 404,
  "message": "Not Found",
  "fieldErrors": [],
  "timestamp": "2026-10-12T10:15:30Z"
}
```

`fieldErrors` lists the invalid fields of a `400` response (`{ "field": "...", "message": "..." }`). Unexpected errors return `500` with a generic message; internal details are never exposed.
