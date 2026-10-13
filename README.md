# PokeExercise: Pokédex Sync

A full-stack application built around a **Java / Spring Boot REST API** that integrates with the public [PokeAPI](https://pokeapi.co/docs/v2). Users browse and inspect Pokémon, import selected Pokémon into a local PostgreSQL database, and enrich those local copies with their own fields (local name, local region, tags).

The backend follows **Clean Architecture** and is built with **TDD**. A **React** frontend consumes the API.

> ✅ **Status:** all four user stories (catalog, detail, import, update and delete), authentication and delivery (Docker, demo data, CI with a coverage gate) are done. See [PROJECT_PLAN.md](PROJECT_PLAN.md) for the plan.

> ℹ️ **Scope:** everything in this README is about the Pokédex app (`backend/`, `frontend/`, `docker-compose.yml`). The [GenAI exercise](#genai-exercise-separate-project) is a separate project with its own [README](genai-exercise/task-api/README.md).

## Quick start (Docker)

Only Docker is needed:

```bash
docker compose up --build
```

When the three containers are healthy, open **http://localhost:3000** and log in with a demo account:

| Username | Password |
|---|---|
| `ash` | `Pikachu123` |
| `misty` | `Togepi123` |

These are **mock credentials** for the demo only. The demo data also has five imported Pokémon (Bulbasaur, Charmander, Squirtle, Pikachu and Eevee); three of them already have a local name, region and tags.

| URL | What |
|---|---|
| http://localhost:3000 | The app |
| http://localhost:8080/swagger-ui.html | API documentation (Swagger UI); use **Authorize** with a token from `POST /api/auth/login` |
| http://localhost:8080/actuator/health | Backend health check |

What runs:

| Service | Image | Notes |
|---|---|---|
| `db` | PostgreSQL 16 | Data kept in the `pokedex-db` volume. Host port 5433. |
| `backend` | Multi-stage Gradle build → `eclipse-temurin:21-jre`, non-root user | Flyway creates the schema and the demo data at start. Waits for the database to be healthy. |
| `frontend` | Vite build → nginx (non-root) | Serves the app and forwards `/api` to the backend, so the browser only talks to one origin. Waits for the backend to be healthy. |

Good to know:
- Ports and settings can be changed in a `.env` file: copy [.env.example](.env.example).
- Without `JWT_SECRET`, the backend makes a new signing key at each start, so everyone has to log in again after a restart. Set one in `.env` to keep sessions (`openssl rand -base64 32`).
- The demo data only adds what is missing: it never overwrites users or Pokémon that already exist. To start without it, set `FLYWAY_LOCATIONS=classpath:db/migration`.
- Stop with `docker compose down`; add `-v` to also delete the database.

## Documentation

| Document | Content |
|---|---|
| [Project_description.md](Project_description.md) | Actors, features, concurrency scenarios, data model, use cases |
| [Requirements_and_Technologies.md](Requirements_and_Technologies.md) | Functional and technical requirements and technologies, per feature |
| [User_stories_imp.md](User_stories_imp.md) | TDD implementation plan per user story (tests written first) |
| [PROJECT_PLAN.md](PROJECT_PLAN.md) | Overall plan: architecture, API, implementation order, verification |

## GenAI exercise (separate project)

[`genai-exercise/`](genai-exercise/) holds the "Generative AI Tools" part of the exercise: the [prompt](genai-exercise/PROMPT.md), the generated Task Management API ([`task-api/`](genai-exercise/task-api/)) and [how the output was reviewed](genai-exercise/AI_REVIEW.md).

It is **independent of the Pokédex app**: its own Gradle build, in-memory H2 database and port (**8081**), and it is not part of `docker compose`. See its [README](genai-exercise/task-api/README.md) to run it.

## Tech stack

| Area | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Gradle (Kotlin DSL), Spring `RestClient`, virtual threads |
| Data | PostgreSQL, Spring Data JPA, Flyway |
| Cache | Caffeine (async cache, safe on virtual threads) |
| Security | Spring Security, JWT, BCrypt |
| Backend tests | JUnit 5, Mockito, AssertJ, MockMvc, WireMock, Testcontainers, ArchUnit, JaCoCo |
| Frontend | React 19, TypeScript, Vite, React Router, TanStack Query, React Hook Form + Zod, Tailwind CSS |
| Frontend tests | Vitest, React Testing Library, MSW |
| Delivery | Docker, Docker Compose, GitHub Actions |

## Repository layout

```
├── backend/                 Spring Boot API
│   ├── src/main/resources/db/ Flyway: migration/ (schema), seed/ (demo data)
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
├── backend/Dockerfile       Backend image (multi-stage, non-root)
├── frontend/Dockerfile      Frontend image (nginx, non-root) + frontend/nginx.conf
├── docker-compose.yml       Database, backend and frontend, with health checks
├── genai-exercise/          GenAI exercise: prompt, Task API, review notes (separate project)
├── .env.example             Settings for docker compose
└── .github/workflows/ci.yml Build, tests and coverage gate on every push
```

**Architecture rule:** dependencies point inward only (`web` / `infrastructure` → `application` → `domain`). The domain and application layers don't depend on Spring. `ArchitectureTest` enforces this on every build.

## Local development

For working on the Pokédex code, run the database in Docker and the backend and frontend on your machine. (The GenAI Task API has its own steps in [its README](genai-exercise/task-api/README.md).)

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

The demo accounts above work here too: the backend adds the demo data to the local database at start. Stop the Docker `backend` and `frontend` containers first if they are running (`docker compose stop backend frontend`), since they use the same ports.

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
| `FLYWAY_LOCATIONS` | `classpath:db/migration,classpath:db/seed` | Backend: schema migrations plus demo data. Use `classpath:db/migration` to start without the demo data. |
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | `pokedex` / `pokedex` / `pokedex` | docker-compose (local development only) |
| `POSTGRES_PORT` | `5433` | docker-compose: host port of the database (not 5432, so it doesn't clash with a locally installed PostgreSQL) |
| `FRONTEND_PORT` / `BACKEND_PORT` | `3000` / `8080` | docker-compose: host ports of the app and the API |
| `VITE_API_PROXY_TARGET` | `http://localhost:8080` | Frontend dev server proxy |

## Tests

Pokédex tests (the GenAI Task API runs its own with `./gradlew test` from `genai-exercise/task-api`):

```bash
cd backend && ./gradlew check
```

Runs all backend tests, then the **coverage rule**: the `domain` and `application` packages (business rules and use cases) must keep at least 80% line and branch coverage, or the build fails. Currently they are at 92–100%. Docker must be running: the database tests start a real PostgreSQL with Testcontainers. The coverage report is written to `backend/build/reports/jacoco/test/html/index.html`.

What the backend tests cover: domain rules (unit), use cases with fakes (unit), the PokeAPI adapter against WireMock (parallel loading, timeouts, cache), the database adapters against PostgreSQL (unique constraints, optimistic locking, simultaneous imports, updates and deletes, the demo data), the controllers with MockMvc (status codes and the error format), full-stack integration tests, and `ArchitectureTest` (layer rules).

```bash
cd frontend && npm test
```

Other frontend checks: `npm run lint`, `npm run typecheck`, `npm run build`. The frontend tests render the real app against a fake API (MSW): every screen, form validation, the 404 and 409 messages, disabled buttons while a request runs, out-of-order paging, and reloading fresh data.

## API

| Method & path | Access | Description |
|---|---|---|
| `GET /api/pokemon?page=0&size=20` | Public | US-01: catalog page (zero-based `page`, `size` 1–50, default 20). Each item has sprite, category, types, weight (kg) and abilities. `400` for invalid paging, `502` if PokeAPI is slow or down. |
| `POST /api/auth/register` | Public | Create an account: `{ username, email, password }`. `201` with the user (never the password), `400` for invalid data, `409` if the username or email is taken. |
| `POST /api/auth/login` | Public | `{ username, password }` → `200` with `{ token, tokenType: "Bearer", expiresAt, user }`; `401` for wrong credentials. |
| `GET /api/auth/me` | Protected | The logged-in user. `401` without a token or with an expired/invalid one. |
| `GET /api/pokemon/{idOrName}` | Public | US-02: detail by number or name (any case): official artwork, category, types, height, weight, abilities, base stats, description and evolution lineage. With a token, it also says whether the Pokémon is imported: `imported` and, when true, `localId`. Visitors get neither field. `404` for an unknown Pokémon, `400` for an invalid name, `502` if PokeAPI is slow or down. |
| `POST /api/local-pokemon` | Protected | US-03: import `{ idOrName }` from PokeAPI. `201` with the local copy (PokeAPI data, empty `localName`, `localRegion`, `tags`, and its `version`) and a `Location` header; `404` if PokeAPI has no such Pokémon, `409` if it is already imported (also when two imports run at the same moment), `502` if PokeAPI is slow or down. |
| `GET /api/local-pokemon?page=0&size=20` | Protected | The local Pokémon, in Pokédex order. Same paging rules as the catalog. |
| `GET /api/local-pokemon/{id}` | Protected | One local Pokémon. `404` if it doesn't exist (e.g. another user deleted it). |
| `PUT /api/local-pokemon/{id}` | Protected | US-04: `{ localName, localRegion, tags, version }` replaces the proprietary fields (`null` or blank clears a text; tags are stored trimmed and in lower case). `version` is the one you loaded. `200` with the new version; `400` for malformed JSON, invalid values (local name or region over 100 characters, blank, duplicated or over-30-character tags, more than 20 tags), a missing `version` or `tags`, or any other field (read-only PokeAPI fields and unknown fields are listed in `fieldErrors`); `404` if it no longer exists; `409` if someone else changed it since you loaded it. |
| `DELETE /api/local-pokemon/{id}?version=` | Protected | US-04: `204`; `400` without `version`; `404` if it no longer exists (e.g. a second delete); `409` if someone else changed it since you loaded it. |

How US-01 works: PokeAPI's list only returns names, so for each Pokémon of the page the backend fetches its details and its species (where the category lives). The Pokémon of a page are fetched **in parallel on virtual threads**, with timeouts. If any of them fails, the whole page returns `502`. Responses are cached (Caffeine) by Pokémon id. A name in any case ("Pikachu", "pikachu") points to the same entry, errors are never cached, and simultaneous requests for the same uncached Pokémon make a single PokeAPI call.

How US-02 works: the detail combines three PokeAPI resources: the Pokémon (artwork, types, stats), its species (category and the most recent English Pokédex text, cleaned of the games' line breaks) and the species' evolution chain. The chain is a tree, flattened into ordered stages; each stage records what it evolves from, so branching evolutions (Eevee, Wurmple) display correctly. All three resources are cached, so a repeated detail makes no PokeAPI call.

How authentication works: passwords are stored as BCrypt hashes. Login returns a signed JWT (HS256) that expires after `JWT_TTL`. Protected requests send it as `Authorization: Bearer <token>`. The API is stateless (no server session); logging out deletes the token in the browser. Routes are **closed by default**: only registration, login, the catalog, health and the API docs are public. Usernames and emails are unique whatever their letter case, and the database's unique constraints guarantee it even when two people register the same name at the same moment.

How US-03 works: the import first fetches the Pokémon from PokeAPI (through the same cache), **outside any database transaction**, so a slow PokeAPI never holds a database connection; only then is it saved, in one short transaction. The PokeAPI data is copied once (with its types, abilities, stats and evolution in child tables) and never changes afterwards; the proprietary fields start empty. The `pokeapi_id` column is unique, so the database decides when two users import the same Pokémon at the same moment: one gets `201`, the other `409`, never a `500`. On the catalog detail, the "imported" flag is a fresh database lookup on every request, never cached, so a Pokémon shows as imported right after anyone imports it. A token on the public detail route is optional, but if one is sent it must be valid: an expired or forged token gets `401`, and the frontend then asks the user to log in again (it only sends the token when it believes it is logged in).

How US-04 works: only the proprietary fields can change; the PokeAPI data stays as imported. Every record has a `version` that increases on each save, **including a change to the tags only** (every save also updates `updated_at`, a column of the Pokémon itself). Updates and deletes must send the version the user loaded (*optimistic locking*): if it is no longer the current one, someone else changed the record in between, and the request gets `409` instead of overwriting or deleting a change nobody saw. The check is done by the database itself (`UPDATE … WHERE version = ?` through Hibernate's `@Version`, and a single `DELETE … WHERE id = ? AND version = ?`), so it also holds when two requests arrive at the same moment. A record that no longer exists gets `404`, so of two users deleting the same Pokémon, the second gets `404`. In the frontend, the edit form is a snapshot (values and version are taken once, when it opens), so a background reload never changes what the user is typing; after a `409` a **Reload** button shows the latest data and the user applies their change again.

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

## User stories → API → screens

| User story | API | Screen |
|---|---|---|
| US-01 Browse the catalog | `GET /api/pokemon` | **Catalog** (`/`, `?page=`): cards with sprite, category, types, weight and abilities; Previous / Next |
| US-02 See a Pokémon's detail | `GET /api/pokemon/{idOrName}` | **Detail** (`/pokemon/:idOrName`): artwork, category, types, stats, description, evolution; for logged-in users, Import or "Already imported – View local version" |
| US-03 Import a Pokémon | `POST /api/local-pokemon`, `GET /api/local-pokemon`, `GET /api/local-pokemon/{id}` | **Import** button on the detail; **My Pokémon** (`/local`); **local Pokémon** (`/local/:id`) |
| US-04 Change or delete local data | `PUT /api/local-pokemon/{id}`, `DELETE /api/local-pokemon/{id}?version=` | **Edit** and **Delete** on the local Pokémon screen, with the 409 "Reload" flow |
| Authentication | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me` | **Register** (`/register`), **Log in** (`/login`), "Signed in as …" and **Log out** in the header |

## Accepted behaviors

These are deliberate trade-offs, described in [Project_description.md](Project_description.md):

- **Lists are split into pages by position.** If someone imports or deletes a Pokémon while another user moves between pages, one Pokémon can appear on two pages or be skipped once. Nothing is lost; reloading shows the correct list.
- **No live updates.** An open screen doesn't change the instant someone else edits or deletes the Pokémon. It reloads when it is opened again or when the user returns to the browser tab, and any action on an outdated screen gets a clear `404` or `409`.
- **Logout happens in the browser.** The token is deleted there; the server keeps no sessions, so a copied token stays valid until it expires (`JWT_TTL`, 1 hour by default).
- **A retry after a lost answer gets `409`.** If a save succeeds but the answer never arrives, saving again finds a new version. The message asks the user to reload, and they see that their change was saved.
- **The PokeAPI cache belongs to each backend instance.** With several instances, each one has its own cache. All the rules that protect data (unique keys, versions) live in the database, so they hold for any number of instances.
