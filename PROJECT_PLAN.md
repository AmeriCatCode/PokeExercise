# Project Plan: Pokédex Sync API (Java / Spring Boot) + React Frontend

## Context

This plan is based on three project documents:

- `Project_description.md` covers actors, features, the concurrency scenarios, the data model, use cases UC-01–UC-14 and the non-functional requirements.
- `Requirements_and_Technologies.md` gives functional and technical requirements plus the technologies for each feature.
- `User_stories_imp.md` sets the story-by-story TDD implementation order, the red tests to write first for each step, and the Definition of Done.

It adds presentation prep, CI (GitHub Actions), ArchUnit boundary tests, MSW and a frontend Dockerfile. Section 6b maps every concurrency and fresh-data scenario to a mechanism and its tests.

---

## 1. Technology Stack

| Layer | Technologies |
|---|---|
| Language / framework | Java 21, Spring Boot 4.1, **Gradle (Kotlin DSL)** |
| External API | Spring `RestClient` with connect/read timeouts; **virtual threads** for parallel calls |
| Cache | Spring Cache + Caffeine (max 1,000 entries, 24h TTL, `@Cacheable(sync = true)`) |
| Database | PostgreSQL, Spring Data JPA (Hibernate), Flyway (schema + seed) |
| Security | Spring Security, **Spring OAuth2 Resource Server (JWT)**, BCrypt, token expiry 1h |
| Validation / errors | Jakarta Bean Validation, `@RestControllerAdvice` |
| API docs | springdoc-openapi (Swagger UI) |
| Backend tests | JUnit 5, Mockito, AssertJ, MockMvc, WireMock, Testcontainers, JaCoCo (+ ArchUnit, an addition beyond the docs) |
| Frontend | React 19 + TypeScript, Vite, React Router, TanStack Query, React Hook Form + Zod, Tailwind CSS |
| Frontend tests | Vitest, React Testing Library (+ MSW for API mocking, an addition beyond the docs) |
| Delivery | Docker, Docker Compose (db + backend + frontend), GitHub, GitHub Actions CI (addition) |

---

## 2. Architecture (Clean Architecture, package by layer)

The app is a single Gradle project with packages by layer, as the docs specify. ArchUnit tests enforce the boundaries, so the dependency rule is checked automatically without the overhead of multiple modules.

```
com.example.pokedex
├── domain/          Pure Java: PokemonSummary, PokemonDetail, EvolutionChain/Stage, Pokemon (local),
│                    User, Page<T>; business rules (hg→kg, chain flattening, proprietary-field rules);
│                    domain exceptions (NotFound, AlreadyExists, VersionConflict, CatalogUnavailable)
├── application/     Use cases + ports
│   ├── usecase/     ListPokemon, GetPokemonDetail, ImportPokemon, ListLocalPokemon, GetLocalPokemon,
│   │                UpdateLocalPokemon, DeleteLocalPokemon, RegisterUser, Login
│   └── port/        PokemonCatalogPort, LocalPokemonRepositoryPort, UserRepositoryPort,
│                    PasswordHasherPort, TokenIssuerPort
├── infrastructure/
│   ├── pokeapi/     RestClient adapter, PokeAPI DTOs, mappers, cache config, key normalization
│   ├── persistence/ JPA entities (@Version), Spring Data repos, adapters (DB exceptions → domain), Flyway
│   └── security/    SecurityFilterChain, JWT encoder/decoder (Resource Server), BCrypt adapter
└── web/             Controllers, request/response DTOs, Bean Validation, GlobalExceptionHandler, OpenAPI
```

Key rules:
- Use cases don't depend on Spring. They are registered as beans in a `@Configuration` class.
- **The PokeAPI call happens before the DB transaction.** `ImportPokemon` fetches from the catalog first, then calls a short `@Transactional` persistence method.
- **Database conflicts never return 500.** Unique-constraint violations become `AlreadyExists` (409). `OptimisticLockException` / stale version becomes `VersionConflict` (409).

---

## 3. Data Model

**pokemon** (only imported Pokémon + seed data)
| Group | Fields | Editable |
|---|---|---|
| Identification | `id` (PK), `pokeapi_id` (UNIQUE) | No |
| From PokeAPI (copied once at import) | name, sprite_url, image_url, category (genus), weight_kg, description; child tables `pokemon_type`, `pokemon_ability`, `pokemon_stat`, `pokemon_evolution_stage` | No |
| Proprietary | `local_name`, `local_region`, `pokemon_tag` (child table, multiple tags) | **Yes** |
| Control | `version` (`@Version`, incremented on any change **including tags-only**), `created_at`, `updated_at` | No |

To make a tags-only change bump the version, the update forces a version increment on the aggregate root, for example with `OPTIMISTIC_FORCE_INCREMENT` or by touching `updated_at`. A Testcontainers test covers this.

**app_user**: `id` (PK), `username` (UNIQUE), `email` (UNIQUE), `password_hash` (BCrypt), `role`.

**Seed (Flyway):** a few pre-imported Pokémon and demo users with mock credentials, documented in the README.

---

## 4. API Design

Base path `/api`. The error body has the same shape everywhere: `{ status, message, fieldErrors[], timestamp }`.

| Method & path | Access | Success | Errors |
|---|---|---|---|
| `POST /api/auth/register` | Public | 201 | 400 invalid · 409 duplicate (incl. simultaneous) |
| `POST /api/auth/login` | Public | 200 `{ token, expiresAt }` | 401 bad credentials |
| `GET /api/pokemon?page=&size=` | Public | 200 page (default size 20, max 50) | 400 invalid paging · 502 PokeAPI slow/down/any item fails |
| `GET /api/pokemon/{idOrName}` | Public (optional auth) | 200 image, stats, description, evolution; **if logged in, also `imported` + `localId` (fresh DB lookup, not cached)** | 404 unknown · 502 |
| `POST /api/local-pokemon` `{ idOrName }` | Protected | 201 (import) | 401 · 404 not in PokeAPI · 409 already imported (incl. simultaneous) |
| `GET /api/local-pokemon?page=&size=` | Protected | 200 page | 400 · 401 |
| `GET /api/local-pokemon/{id}` | Protected | 200 | 401 · 404 (e.g. deleted by another user) |
| `PUT /api/local-pokemon/{id}` `{ localName, localRegion, tags, version }` | Protected | 200 with new version | 400 malformed/invalid/read-only field/missing version · 401 · 404 · 409 stale version |
| `DELETE /api/local-pokemon/{id}?version=` | Protected | 204 | 400 missing version · 401 · 404 (incl. second delete) · 409 stale version |

Notes:
- Read-only fields are rejected rather than ignored. Unknown or read-only properties in the PUT body return 400 via `FAIL_ON_UNKNOWN_PROPERTIES` on a dedicated DTO.
- Optional auth on the detail endpoint: the route is `permitAll`, and if a valid token is present the controller adds the import status.
- Both the list and the detail show `category` (the English genus, e.g. "Seed Pokémon", from `/pokemon-species` → `genera`) and `types` (e.g. `["grass","poison"]`, from `/pokemon` → `types`). A missing English genus leaves the category empty.

---

## 5. Cache Rules (US-01 / US-02)

- Caffeine with max 1,000 entries and a 24h TTL. Separate caches for Pokémon details, species and the combined detail. Species responses follow the same rules.
- **Normalized key:** "Pikachu", "pikachu" and "25" share one entry. Name and id are resolved to a canonical id, for example with a small name→id map cached once the first lookup returns.
- **Errors and 404s are never cached.** Exceptions propagate, and `unless` covers null results.
- `sync = true`, so two simultaneous requests for an uncached Pokémon make a single PokeAPI call.
- The cache is per instance (accepted). The "imported" flag never goes through the cache.

---

## 6. Implementation Order: vertical slices with TDD

Each story goes domain → use case → adapters → controller → frontend, with 🔴 tests written first at every step. The full test lists are in `User_stories_imp.md`, which is the authoritative checklist.

**Step 0: Setup (≈0.5 d)**
- Gradle (Kotlin DSL) Spring Boot project, Vite React-TS project, layer packages, and the test stack.
- Global error handler. 🔴 First test: "unexpected error → JSON with status, message, timestamp".
- Git repo + GitHub Actions CI, plus docker-compose with Postgres for local development.

**Step 1: US-01 Enumeration (≈1.5 d)**
- Domain: `PokemonSummary` (sprite, category, types, weight, abilities; hg→kg; English genus or empty), `Page` (total pages, e.g. 1302/20 = 66).
- Use case: page < 0 and size outside 1–50 are rejected.
- PokeAPI adapter:
  - page/size → offset/limit; item order preserved.
  - Per item: `/pokemon/{name}` → `/pokemon-species` (via the `species.url` in the response, since alternate forms have a different species id). The items of a page run in parallel (up to 41 calls per page of 20 when nothing is cached).
  - **Parallel test:** 20 items × 300 ms delay finish well under 6 s, using a virtual-thread executor.
  - Timeout or any item failing → `CatalogUnavailable`.
- Cache tests for each rule in §5.
- Controller tests: 200, default size 20, 400, 502, public.
- Frontend:
  - **Global QueryClient freshness config + query-key factory** (see §6b).
  - Catalog cards (sprite, category, type badges, weight, abilities) + pagination, a disabled Next button on the last page, loading and error states.
  - **Out-of-order safety:** the TanStack Query key includes the page, so only the current page's data is shown.

**Step 2: US-02 Detailed View (≈1 d)**
- Domain: flatten the evolution chain into ordered stages, **including branching (Eevee)**.
- Adapter: combine `/pokemon`, `/pokemon-species` and `/evolution-chain`; pick the English flavor text and clean its control characters; 404 → NotFound; slow or down → Unavailable.
- Cache tests: no second call, shared name/id key, NotFound not cached.
- Controller: 200 / 404 / 502, public.
- Frontend: detail screen with artwork, category, type badges, stat bars, description, clickable evolution stages, and a not-found state.

**Step 3: Authentication (≈1 d)**
- Domain `User` validation. Use cases: password stored hashed, duplicate → AlreadyExists, wrong password → InvalidCredentials, login returns a token.
- Adapters (Testcontainers):
  - Unique username/email.
  - **Simultaneous registration** test: two threads register the same username → exactly one succeeds and the other gets AlreadyExists.
  - JWT valid / expired / tampered.
- Security tests: register 201/409/400, login 200/401, protected route with no token or an expired token → 401, public route → 200.
- Frontend:
  - Login and register forms (RHF + Zod), with the token stored and sent on requests.
  - A global 401 handler shows "Your session expired, please log in again" and opens the login screen.
  - Logout clears the token.

**Step 4: US-03 Import (≈1.5 d)**
- Domain: a new import has empty proprietary fields and keeps the PokeAPI data as received.
- Use case: fetch → save; already imported → 409; not in PokeAPI → 404. Test that **the PokeAPI call happens outside the transaction**, for example by verifying call order with mocks or checking `TransactionSynchronizationManager.isActualTransactionActive()` inside the fake catalog.
- DB adapter (Testcontainers): save and read with tags, duplicate `pokeapi_id` rejected, **simultaneous import** → exactly one row.
- Controllers: import 201/404/409/401; local list paginated; local view 404.
- **Imported indicator** on `GET /api/pokemon/{id}`: logged in + imported → `imported:true, localId`; logged in + not imported → false; visitor → absent; right after an import it already reads true, despite the cache.
- Frontend:
  - Import button, disabled while pending. On 409, invalidate and show "Already imported – View local version".
  - "My local Pokémon" list (with the same out-of-order paging safety as the catalog) and the local detail view.
  - A 404 shows "This Pokémon was deleted by another user" with a link back to the list.
  - `refetchOnWindowFocus` and refetch on mount.
  - After an import, the related queries are invalidated.

**Step 5: US-04 Update & Delete (≈1.5 d)**
- Domain: only local name, region and tags can change; local name length is validated; tags can't be blank or duplicated.
- Use cases: update/delete with version; not found → 404; stale version on update **and delete** → 409.
- DB adapter (Testcontainers): saving with an old version fails, deleting with an old version fails, and a **tags-only change increments the version**.
- Controllers:
  - PUT 200 with the new version.
  - 404, including a record deleted meanwhile.
  - 400 for malformed JSON, invalid values, read-only fields or a missing version (also on DELETE).
  - 409 for a stale version.
  - DELETE 204, a second DELETE 404, and 401 without login.
- Frontend:
  - The edit form validates before submitting.
  - Save and Delete buttons are disabled while pending, and Delete asks for confirmation.
  - On 409 the screen shows "changed by someone else, please reload", with a Reload button that re-initializes the form with the latest data and version.
  - A 404 on save shows "no longer exists". A 404 on delete is treated as already deleted: go to the list.
  - After save or delete, the list and detail queries are invalidated. After a delete, the catalog detail's imported flag is invalidated too.
  - **The edit form keeps its initial data and version**: it is initialized once and never reset by background refetches.

**Step 6: Delivery (≈1 d)**
- Flyway seed with a few pre-imported Pokémon and demo users.
- Backend Dockerfile: multi-stage Gradle build → `eclipse-temurin:21-jre`, non-root user.
- Frontend Dockerfile: Vite → nginx, which proxies `/api`.
- `docker-compose.yml` with db, backend and frontend, plus health checks.
- README covering setup (Docker and local), env vars (`DB_URL`, `JWT_SECRET`, `POKEAPI_BASE_URL`, `JWT_TTL`, cache size/TTL), demo credentials, architecture, the API table, the user story → endpoint → screen mapping, accepted behaviors, testing and coverage, and Swagger.
- Final check: all tests green, JaCoCo report, no warnings in the browser console.

**Step 7: Presentation prep (≈0.5 d)**
- Walk through the user stories, architecture and layer boundaries (ArchUnit), and the TDD evidence (test-first commits, coverage).
- Demo the concurrency scenarios live with two browsers: Ana/Ben edit → 409, edit vs. delete → 409, simultaneous import → 409 → "Already imported".
- Explain the accepted behaviors and trade-offs.

Total estimate: about 8.5 working days.

---

## 6b. Concurrency & Fresh-Data Coverage

Every scenario in `Project_description.md` §4.4 ("Concurrency scenarios" + "Keeping screens up to date") and in `Requirements_and_Technologies.md` Feature 6 maps to a mechanism and at least one test.

**Frontend foundation (set up in Step 1, not later):** a single `QueryClient` config with `staleTime: 0`, `refetchOnMount: true` and `refetchOnWindowFocus: true`, plus a central query-key factory (`catalogKeys`, `detailKeys`, `localKeys`) so invalidation is consistent. Each mutation hook owns its invalidation list.

### Concurrency scenarios

| # | Scenario | Backend mechanism + test | Frontend behavior + test |
|---|---|---|---|
| C1 | Two users edit the same Pokémon | `@Version` check; Testcontainers "save with old version fails"; MockMvc PUT stale → 409 | 409 shows "changed by someone else, please reload". A **Reload** button refetches the record and re-initializes the form with the new data and version, so the user sees Ana's change and re-applies theirs. Test: 409 → message → reload → form shows new values |
| C2 | Delete after someone else edited | DELETE requires `?version=`; Testcontainers "delete with old version fails"; MockMvc → 409 | Same 409 message and reload flow on the delete action. Test |
| C3 | Edit a Pokémon that was just deleted | Use case → NotFound; MockMvc PUT on missing id → 404 | **Save returns 404 → "This Pokémon no longer exists (deleted by another user)" with a link to the local list.** Test |
| C4 | Two users delete the same Pokémon | MockMvc: first DELETE 204, second 404 | **Delete returns 404 → treated as already deleted: invalidate queries, go to the list, show an info message.** Test |
| C5 | Double click / two tabs | Two tabs are covered by C1/C2 | Save, Import and Delete disabled while `isPending`. Tests for each button |
| C6 | Rapid "Next" clicks | — | Page number in the query key, so only the current page's data renders. Use `placeholderData: keepPreviousData` while showing the requested page number. Test with MSW delays (later response for an earlier page must not win), **for both the catalog and the local list** |
| C7 | Open a Pokémon another user deleted | MockMvc GET local/{id} missing → 404 | "This Pokémon was deleted by another user" + link back. Test |
| C8 | Import race (button shown, someone imports first) | Unique `pokeapi_id`; Testcontainers simultaneous import → exactly one row; MockMvc → 409 never 500 | On 409, invalidate the detail query, which then shows "Already imported – View local version". Test |
| C9 | Simultaneous registrations | Unique username/email; Testcontainers two-thread test → one succeeds, other gets AlreadyExists → 409 | Register form shows "username already taken". Test |
| C10 | Simultaneous uncached PokeAPI requests | `@Cacheable(sync = true)`; test: two concurrent calls → one WireMock hit | — |
| C11 | Slow PokeAPI holding DB connections | PokeAPI called before the transaction; test asserts no active transaction during the catalog call | — |
| C12 | Multiple backend instances | All guarantees live in the DB (version, unique constraints); cache per instance (accepted) | — |
| C13 | Lost response, then retry | Retry → 409 (accepted, no dedicated test) | Covered by the C1 message + reload |

### Fresh-data scenarios

| # | Requirement | Mechanism | Test |
|---|---|---|---|
| F1 | Fresh data on every screen opening (open, back navigation, page change) | `staleTime: 0` + `refetchOnMount` (global config) | Navigate away and back with MSW returning changed data → the screen shows new data |
| F2 | Reload on returning to the browser tab | `refetchOnWindowFocus: true` | Fire a `visibilitychange`/focus event → refetch happens |
| F3 | Own changes visible everywhere | After **import**: invalidate `localKeys.list`, `detailKeys.byId` (imported flag). After **save**: invalidate `localKeys.list` and `localKeys.byId`. After **delete**: remove `localKeys.byId`, invalidate `localKeys.list` **and the catalog detail's imported flag**, so the catalog detail shows Import again | One test per mutation: go back → new values / item gone / Import button back |
| F4 | "Imported" flag always fresh | Backend: DB lookup outside the cache. MockMvc: right after import → `imported:true` | Covered by F3 |
| F5 | Open edit form never changed by reload | Form initialized once from a snapshot (`defaultValues` set on first load, no `reset` on refetch); the version is held in the form state | Background refetch with changed data → typed values and version unchanged |
| F6 | Session expired | JWT expiry → 401 (backend test with an expired token) | A global response interceptor on 401 for protected calls clears the token, shows "Your session expired, please log in again" and redirects to login. Test |
| F7 | No live updates (accepted) | — | Documented in the README; mitigated by F1/F2 and the clear 404/409 messages |

Steps 1, 4 and 5 implement these items. The freshness config is set up in Step 1, so every screen built later inherits it.

---

## 7. Definition of Done (each story)

- Tests were written before the code, and all of them pass.
- Every functional and technical requirement is covered by at least one test.
- Correct status codes and the common error format are used.
- The frontend works end-to-end with the backend.
- The code is refactored and readable.

---

## 8. Verification

- `./gradlew test jacocoTestReport` passes, including Testcontainers, WireMock and ArchUnit tests, with coverage of at least 80% on domain and application.
- `npm run test` and `npm run lint` pass in `frontend/`.
- `docker compose up --build` brings the app up with seed data. Swagger is at `:8080/swagger-ui.html` and the UI at `:3000`.
- Manual demo:
  1. Browse, then open the detail view as a visitor (no Import button).
  2. Log in and import a Pokémon. The detail view now shows "Already imported".
  3. Edit it in two tabs. The second save gets 409.
  4. Delete with a stale version. It gets 409.
  5. Delete twice. The second delete gets 404.
  6. Let the token expire. The next protected action redirects to login.
  7. Throughout, the browser console shows no warnings.
