# Requirements & Technologies by Feature

> Related documents: [Project_description.md](Project_description.md) · [Java_BLA_Technical_Interview_Exercise_V2.md](Java_BLA_Technical_Interview_Exercise_V2.md)

This document lists, for each feature of the project:
- **Functional requirements** — what the feature must do for the user.
- **Technical requirements** — how the feature must be built (quality, rules, constraints).
- **Recommended technologies** — the tools to build it.

---

## Feature 1 — Pokémon Enumeration (US-01)

**Functional requirements**
- Browse the Pokémon catalog page by page.
- Each entry shows: sprite, category (official Pokédex category, e.g. "Seed Pokémon"), types (e.g. grass, poison), mass (weight) and skills (abilities).
- Public: available without logging in.

**Technical requirements**
- Get the data from PokeAPI.
- Paginated responses with a default page size (20) and a maximum (e.g. 50).
- Invalid paging values return `400`.
- Each entry needs two PokeAPI calls: the Pokémon details, then its species (for the category, using the species URL given in the details, because alternate forms have a different species id).
- Fetch the Pokémon of a page in parallel to keep pages fast.
- Category is the English entry of the species `genera`; if there is none, it is left empty (no error).
- Timeouts on every PokeAPI call. If PokeAPI is down, too slow, or fails for any Pokémon of the page, the whole request returns `502`.
- Cache PokeAPI responses, both Pokémon details and species (a "nice to have" in the exercise; included):
  - Limited size and expiry time (e.g. 1,000 entries, 24 hours).
  - Errors and "not found" results are never cached.
  - One entry per Pokémon: the cache key is normalized (e.g. "Pikachu", "pikachu" and "25" share one entry).
  - Two simultaneous requests for an uncached Pokémon make only one call to PokeAPI.

**Recommended technologies**

| Technology | Purpose |
|---|---|
| Spring Web | REST endpoint |
| Spring `RestClient` (with timeouts) | Calls to PokeAPI |
| Java 21 virtual threads | Parallel calls to PokeAPI |
| Spring Cache + Caffeine | In-memory cache with size limit and expiry; `@Cacheable(sync = true)` for simultaneous requests |
| Jakarta Bean Validation | Validate `page` and `size` |

---

## Feature 2 — Detailed View (US-02)

**Functional requirements**
- Select a Pokémon and see its image, category, types, core statistics, description and evolution lineage.
- Public: available without logging in.
- The detail always shows PokeAPI data. For logged-in users, it also shows whether the Pokémon is already imported:
  - Not imported → **Import** button.
  - Imported → "Already imported" label and a **View local version** link instead of the Import button.

**Technical requirements**
- Combine information from several PokeAPI sources into one response.
- Cache the results, with the same cache rules as Feature 1.
- Unknown Pokémon returns `404`; PokeAPI down or too slow returns `502`.
- For logged-in users, the response includes `imported` (true/false) and the local id. This check is a fresh database lookup, **never cached**, so it is correct right after an import. Visitors don't get this information.

**Recommended technologies**

| Technology | Purpose |
|---|---|
| Spring Web | REST endpoint |
| Spring `RestClient` | Calls to PokeAPI |
| Spring Cache + Caffeine | Cache detail responses |
| Spring Data JPA | "Is it imported?" lookup in the local database |

---

## Feature 3 — Data Synchronization / Import (US-03)

**Functional requirements**
- An authenticated user imports a specific Pokémon from PokeAPI into the local database.
- The imported Pokémon gets extra, empty proprietary fields: local name, local region and tags.
- Authenticated users can list and view the Pokémon stored locally.

**Technical requirements**
- Relational database with a primary entity (Pokemon) that has a unique key and descriptive attributes.
- The same Pokémon cannot be imported twice (`409`).
- Concurrency: if two users import the same Pokémon at the same moment, the database rejects the second one, and the API returns `409` (never `500`).
- Viewing a local Pokémon that no longer exists (e.g. deleted by another user) returns `404`.
- PokeAPI is called **before** opening the database transaction, so a slow PokeAPI never keeps database connections busy.
- Pokémon not found in PokeAPI returns `404`.
- Protected route: requires login (`401` otherwise).
- Separate data access layer (repositories).
- Local list is paginated from our own database.
  - Accepted behavior: pages are split by position, so if another user adds or deletes a Pokémon while someone moves between pages, one Pokémon may appear on two pages or be skipped once. Nothing is lost; reloading shows the correct list.

**Recommended technologies**

| Technology | Purpose |
|---|---|
| PostgreSQL | Relational database |
| Spring Data JPA (Hibernate) | Data access layer / repositories |
| Flyway | Database schema creation and seed data |
| Spring `RestClient` | Get the Pokémon from PokeAPI |

---

## Feature 4 — Local Data Modification (US-04)

**Functional requirements**
- An authenticated user updates the proprietary fields (local name, local region, tags) of a local Pokémon.
- An authenticated user deletes a local Pokémon.

**Technical requirements**
- Only proprietary fields are editable; PokeAPI data is read-only.
- Validation:
  - `404` if the Pokémon is not in the local database.
  - `400` for malformed or invalid data.
  - `400` if the version is missing (it is required on both update and delete).
- Concurrency:
  - Two users editing the same Pokémon: the second save is rejected (`409`) instead of overwriting the first user's changes.
  - Deleting a Pokémon that someone else changed after it was loaded: the delete is rejected (`409`).
  - Editing a Pokémon that was just deleted returns `404`.
  - Two users deleting the same Pokémon: the second delete returns `404`.
  - Any change increases the version, including a change to tags only.
- Protected route: requires login.

**Recommended technologies**

| Technology | Purpose |
|---|---|
| Jakarta Bean Validation | Validate the request data (including the required version) |
| JPA optimistic locking (`@Version`) | Detect simultaneous edits and deletes |
| Spring `@RestControllerAdvice` | Turn errors into clear `400` / `404` / `409` responses |

---

## Feature 5 — User Registration & Authentication

**Functional requirements**
- A visitor can register an account.
- A registered user can log in and log out.
- Browsing and detail pages are public; import, update and delete require login.

**Technical requirements**
- Secondary entity for user management (User) with a unique key.
- Passwords are never stored in plain text.
- Duplicate username/email returns `409`, also when two visitors register the same username at the same moment (the database rejects the second one, and the API returns `409`, never `500`); invalid data returns `400`; wrong credentials return `401`.
- Token-based authentication for protected routes.
- Tokens expire after a set time (e.g. 1 hour); a protected request with an expired token returns `401`.
- Accepted: logout deletes the token in the browser only; the server does not cancel it, so a copied token works until it expires. Standard for JWT; the short expiry limits the risk.

**Recommended technologies**

| Technology | Purpose |
|---|---|
| Spring Security | Public vs. protected routes |
| JWT (Spring OAuth2 Resource Server) | Issue and validate login tokens |
| BCrypt | Password hashing |
| Spring Data JPA | User storage |

---

## Feature 6 — Frontend

**Functional requirements**
- Screens for: browsing the catalog, Pokémon detail, importing, local Pokémon list, editing, deleting, registering and logging in.
- On the detail screen, logged-in users see **Import** or, if already imported, **"Already imported – View local version"**.
- Show clear messages for errors (e.g. "this record was changed by someone else, please reload" on edit or delete).
- Disable the Save, Import and Delete buttons while their request is in progress (prevents double submits).
- Fast page changes (e.g. clicking "Next" several times quickly) always show the page the user ended on, even if answers arrive out of order.
- Opening a local Pokémon that was deleted by another user (`404`) shows "This Pokémon was deleted by another user" with a link back to the local list.
- If Import returns `409` (someone else imported it first), the screen refreshes to show "Already imported – View local version".
- Data is reloaded when the user returns to the browser tab, so open screens don't stay outdated for long. Accepted: there are no live updates; an open screen changes on its next load, and any action on outdated data gets a clear `404` or `409`.
- Every screen loads fresh data when it opens, including when the user goes back to it or changes page.
- After a successful save, import or delete, the frontend discards its saved copies of the affected data (local list, local view, "imported" indicator), so the user sees their own change everywhere straight away.
- An open edit form keeps the data and version it was opened with; automatic reloads never change it (so the user's typing is kept and the version check still works).
- Session expired: a `401` on a protected action shows "Your session expired, please log in again" and opens the login screen (unsaved changes are lost).
- Accepted: if a save succeeds but the answer is lost (network drop), a retry gets `409`; after reloading, the user sees the change was saved.

**Technical requirements**
- Responsive, user-friendly design.
- Clean component organization and efficient state management.
- Ideally, no warnings in the browser console.

**Recommended technologies**

| Technology | Purpose |
|---|---|
| React + TypeScript | UI framework |
| Vite | Build and dev server |
| React Router | Navigation between pages |
| TanStack Query | Calling the API, caching and pagination on the client; reloads data when a screen opens or the user returns to the tab; discards outdated copies after save, import or delete |
| React Hook Form + Zod | Forms and validation |
| Tailwind CSS | Responsive styling |

---

## Cross-cutting requirements (apply to all features)

| Area | Technical requirement | Recommended technologies |
|---|---|---|
| **Architecture** | Clean Architecture: business logic independent of the API and the data access | Package structure by layer (domain, application, infrastructure, web) |
| **API design** | Standard HTTP verbs, required parameters, consistent response structures | Spring Web, springdoc-openapi (Swagger UI for documentation) |
| **Error handling** | One consistent error format across the API; database conflicts (duplicate key, old version) become `409`, never `500` | Spring `@RestControllerAdvice` |
| **Concurrency** | Version checks and unique constraints live in the database, so they also work with several backend instances; the in-memory cache is per instance (only a few extra PokeAPI calls) | JPA optimistic locking, PostgreSQL unique constraints |
| **Testing (backend)** | Unit tests for every core component; TDD preferred | JUnit 5, Mockito, AssertJ, Spring Boot Test (MockMvc), WireMock (fake PokeAPI), Testcontainers (real PostgreSQL), JaCoCo (coverage) |
| **Testing (frontend)** | Tests for main components | Vitest, React Testing Library |
| **Demo data** | Pre-populated data and mock credentials | Flyway seed scripts |
| **Containerization** | Dockerfile to run the application | Docker, Docker Compose (app + database) |
| **Documentation** | README with environment setup and technical documentation | Markdown README, Swagger UI |
| **Source control** | Public Git repository | Git, GitHub |

---

## Technology stack summary

| Layer | Technologies |
|---|---|
| Language & framework | Java 21, Spring Boot 3, Gradle (Kotlin DSL) |
| External API | Spring `RestClient` (with timeouts), virtual threads |
| Cache | Spring Cache + Caffeine (size limit, expiry, `sync = true`) |
| Database | PostgreSQL, Spring Data JPA, Flyway |
| Security | Spring Security, JWT, BCrypt |
| Validation & errors | Jakarta Bean Validation, `@RestControllerAdvice` |
| Backend testing | JUnit 5, Mockito, AssertJ, MockMvc, WireMock, Testcontainers, JaCoCo |
| Frontend | React, TypeScript, Vite, React Router, TanStack Query, React Hook Form, Zod, Tailwind CSS |
| Frontend testing | Vitest, React Testing Library |
| Delivery | Docker, Docker Compose, GitHub, Swagger UI |
