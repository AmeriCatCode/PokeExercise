# PokeExercise: Pokédex Sync

A full-stack application built around a **Java / Spring Boot REST API** that integrates with the public [PokeAPI](https://pokeapi.co/docs/v2). Users browse and inspect Pokémon, import selected Pokémon into a local PostgreSQL database, and enrich those local copies with their own fields (local name, local region, tags).

The backend follows **Clean Architecture** and is built with **TDD**. A **React** frontend consumes the API.

> 🚧 **Status:** Step 0 (project setup), Step 1 (US-01, Pokémon catalog), Step 2 (US-02, detail view) and Step 3 (authentication) are done. The other user stories are implemented step by step; see [PROJECT_PLAN.md](PROJECT_PLAN.md).

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
| `DB_URL` | `jdbc:postgresql://localhost:5433/pokedex` | Backend |
| `DB_USERNAME` / `DB_PASSWORD` | `pokedex` / `pokedex` | Backend (local development values) |
| `JWT_SECRET` | *(empty)* | Backend: base64 key of at least 32 bytes that signs login tokens. When empty, a random key is generated at each start (fine locally: tokens just stop working after a restart). **Set it in any shared environment.** |
| `JWT_TTL` | `1h` | Backend: how long a login token is valid |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000` | Backend: browser origins allowed to call the API directly |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | `pokedex` / `pokedex` / `pokedex` | docker-compose (local development only) |
| `POSTGRES_PORT` | `5433` | docker-compose: host port of the database (not 5432, so it doesn't clash with a locally installed PostgreSQL) |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | Frontend dev server proxy |

## Tests

```bash
cd backend && ./gradlew test
```

Docker must be running: the database tests start a real PostgreSQL with Testcontainers. The coverage report is written to `backend/build/reports/jacoco/test/html/index.html`.

```bash
cd frontend && npm test
```

Other frontend checks: `npm run lint`, `npm run typecheck`, `npm run build`.

## API

| Method & path | Access | Description |
|---|---|---|
| `GET /api/pokemon?page=0&size=20` | Public | US-01: catalog page (zero-based `page`, `size` 1–50, default 20). Each item has sprite, category, types, weight (kg) and abilities. `400` for invalid paging, `502` if PokeAPI is slow or down. |
| `POST /api/auth/register` | Public | Create an account: `{ username, email, password }`. `201` with the user (never the password), `400` for invalid data, `409` if the username or email is taken. |
| `POST /api/auth/login` | Public | `{ username, password }` → `200` with `{ token, tokenType: "Bearer", expiresAt, user }`; `401` for wrong credentials. |
| `GET /api/auth/me` | Protected | The logged-in user. `401` without a token or with an expired/invalid one. |
| `GET /api/pokemon/{idOrName}` | Public | US-02: detail by number or name (any case): official artwork, category, types, height, weight, abilities, base stats, description and evolution lineage. `404` for an unknown Pokémon, `400` for an invalid name, `502` if PokeAPI is slow or down. |

How US-01 works: PokeAPI's list only returns names, so for each Pokémon of the page the backend fetches its details and its species (where the category lives). The Pokémon of a page are fetched **in parallel on virtual threads**, with timeouts. If any of them fails, the whole page returns `502`. Responses are cached (Caffeine) by Pokémon id. A name in any case ("Pikachu", "pikachu") points to the same entry, errors are never cached, and simultaneous requests for the same uncached Pokémon make a single PokeAPI call.

How US-02 works: the detail combines three PokeAPI resources: the Pokémon (artwork, types, stats), its species (category and the most recent English Pokédex text, cleaned of the games' line breaks) and the species' evolution chain. The chain is a tree, flattened into ordered stages; each stage records what it evolves from, so branching evolutions (Eevee, Wurmple) display correctly. All three resources are cached, so a repeated detail makes no PokeAPI call.

How authentication works: passwords are stored as BCrypt hashes. Login returns a signed JWT (HS256) that expires after `JWT_TTL`. Protected requests send it as `Authorization: Bearer <token>`. The API is stateless (no server session); logging out deletes the token in the browser. Routes are **closed by default**: only registration, login, the catalog, health and the API docs are public. Usernames and emails are unique whatever their letter case, and the database's unique constraints guarantee it even when two people register the same name at the same moment.

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
