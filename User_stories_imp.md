# User Stories — Implementation Plan with TDD

> Related documents: [Project_description.md](Project_description.md) · [Requirements_and_Technologies.md](Requirements_and_Technologies.md)

This document explains, step by step, how each user story is built and how TDD is used in it. In every step, the tests listed in the 🔴 column are written **before** the code.

---

## 1. Implementation order

| Order | Story | Why in this order |
|---|---|---|
| 0 | Project setup | Base structure, build and test tools ready |
| 1 | US-01 Pokémon Enumeration | Public, no database needed — simplest starting point |
| 2 | US-02 Detailed View | Reuses the PokeAPI client from US-01 |
| 3 | Authentication | US-03 and US-04 need protected routes |
| 4 | US-03 Data Synchronization | Creates the local data |
| 5 | US-04 Local Data Modification | Needs imported data to modify |
| 6 | Delivery | Seed data, Docker, README |

The frontend screen of each story is built right after its backend, so every story is finished end-to-end.

Each story is built in the same order of steps: **domain → use case → adapters (PokeAPI / database) → controller → frontend**.

---

## 2. Step 0 — Project setup

1. Create the Spring Boot project (Java 21, Gradle with Kotlin DSL) and the React project (Vite + TypeScript).
2. Create the package structure by layer: `domain`, `application`, `infrastructure`, `web`.
3. Add the test tools (JUnit 5, Mockito, AssertJ, WireMock, Testcontainers, JaCoCo, Vitest, React Testing Library).
4. Add the global error handler with a consistent error format.
   - 🔴 Test first: *"an unexpected error returns a JSON body with status, message and timestamp"*.
5. Set up the Git repository.

---

## 3. US-01 — Pokémon Enumeration

**Goal:** browse the catalog page by page, showing sprite, category, types, mass and skills.

**How TDD is used in this story**
- We start with small **unit tests** for the business rules (weight in kg, page calculation), with no Spring and no PokeAPI.
- Before writing the PokeAPI client, we write tests against a **fake PokeAPI (WireMock)**. They define exactly what we expect: which calls are made, what is mapped, that calls run in parallel, and what happens when PokeAPI fails.
- The **cache rules** are each written as a test first (no second call, no cached errors, one entry per Pokémon).

| Step | What we build | 🔴 Tests written first |
|---|---|---|
| 1. Domain | `PokemonSummary` (name, sprite, category, types, weight, abilities) and the `Page` result | • Weight is converted from hectograms to kg<br>• Category is the English genus (e.g. "Seed Pokémon"); if there is no English genus, it is empty<br>• Total pages is calculated correctly (e.g. 1302 items / 20 = 66 pages) |
| 2. Use case | `ListPokemonUseCase` — receives page and size, asks the catalog for the data | • Returns the page given by the catalog<br>• Rejects page < 0 or size outside 1–50 |
| 3. PokeAPI adapter | `PokeApiClient` — calls the list, then each Pokémon's details **and species** (Pokémon of the page **in parallel**), with timeouts; maps only the needed fields | *(WireMock)*<br>• Converts page/size into offset/limit<br>• Maps sprite, category, types, weight and abilities correctly<br>• Uses the species URL from the Pokémon details (alternate forms have a different species id)<br>• Keeps the original order of the items<br>• **Calls really run in parallel:** with each fake response (details and species) delayed 300 ms, a page of 20 finishes in well under 20 × 2 × 300 ms<br>• PokeAPI too slow → stops at the timeout with a "PokeAPI unavailable" error<br>• PokeAPI down, or the details or species of one Pokémon of the page fail → "PokeAPI unavailable" error |
| 4. Cache | Cache each Pokémon's details and species (Caffeine: size limit, expiry, `sync = true`) | • A second request for the same Pokémon does **not** call PokeAPI again (neither details nor species)<br>• "Pikachu", "pikachu" and "25" use the **same** cache entry<br>• A failed call or a "not found" is **not** cached (the next request tries PokeAPI again)<br>• Entries expire after the configured time<br>• Two simultaneous requests for an uncached Pokémon make only **one** call to PokeAPI |
| 5. Controller | `GET /api/pokemon?page=&size=` | *(MockMvc)*<br>• Returns `200` with items and page info<br>• Default size is 20 when not sent<br>• Invalid page/size → `400`<br>• PokeAPI unavailable → `502` with the common error format<br>• Accessible without login |
| 6. Frontend | Catalog screen with cards and pagination | • Renders one card per Pokémon<br>• Each card shows the category and the type badges<br>• "Next" is disabled on the last page<br>• Shows a loading and an error state<br>• Clicking "Next" several times quickly shows the page the user ended on, even if answers arrive out of order |

**Requirements covered:** paginated browsing, the 5 fields (sprite, category, types, mass, skills), caching (limits, no cached errors, normalized keys, simultaneous requests), parallel calls with timeouts, `400` on invalid input, `502` when PokeAPI fails, public access.

---

## 4. US-02 — Detailed View

**Goal:** see a Pokémon's image, category, types, statistics, description and evolution lineage. The detail always shows PokeAPI data.

**How TDD is used in this story**
- The trickiest rule, **turning the evolution chain into ordered stages**, is driven by unit tests first, including the branching case (Eevee).
- Fake PokeAPI responses (WireMock) define how the three PokeAPI sources are combined into one result before the client is written.
- The "imported" indicator for logged-in users is added later, in US-03, because it needs the local database (see US-03, step 6).

| Step | What we build | 🔴 Tests written first |
|---|---|---|
| 1. Domain | `PokemonDetail` and `EvolutionChain` | • Evolution chain is flattened into ordered stages (e.g. Bulbasaur → Ivysaur → Venusaur)<br>• Branching evolutions are handled (e.g. Eevee) |
| 2. Use case | `GetPokemonDetailUseCase` | • Returns the detail given by the catalog<br>• Unknown Pokémon → "not found" error |
| 3. PokeAPI adapter | Combines the Pokémon, species and evolution data | *(WireMock)*<br>• Picks the **English** description and the **English** category (genus)<br>• Maps the types<br>• Combines the three sources into one result<br>• PokeAPI returns 404 → our "not found" error<br>• PokeAPI too slow or down → "PokeAPI unavailable" error |
| 4. Cache | Cache the combined detail (same cache rules as US-01) | • A second request for the same Pokémon does **not** call PokeAPI again<br>• Name and number share the same cache entry<br>• "Not found" and failures are **not** cached |
| 5. Controller | `GET /api/pokemon/{idOrName}` | *(MockMvc)*<br>• Returns `200` with image, category, types, stats, description, evolution<br>• Unknown Pokémon → `404`<br>• PokeAPI unavailable → `502`<br>• Accessible without login |
| 6. Frontend | Detail screen (opened by clicking a card) | • Shows category, type badges, stats, description and evolution stages<br>• Shows a "not found" message |

**Requirements covered:** image, category, types, core stats, description, evolution lineage, caching, `404`, `502`.

---

## 5. Authentication (needed by US-03 and US-04)

**Goal:** register, log in, and protect the write routes.

**How TDD is used in this story**
- Security rules are written as tests first: which routes are public, which need a token, and what each error returns.
- A test checks that the stored password is **hashed** before the registration code is written.
- The simultaneous-registration case is written as a database test first, so the duplicate error is turned into `409` and never reaches the user as `500`.

| Step | What we build | 🔴 Tests written first |
|---|---|---|
| 1. Domain | `User` | • Username/email and password are required and valid |
| 2. Use cases | `RegisterUserUseCase`, `LoginUseCase` | • Password is stored **hashed**, never in plain text<br>• Duplicate username → "already exists" error<br>• Wrong password → "invalid credentials" error<br>• Successful login returns a token |
| 3. Adapters | `UserRepository` (JPA), BCrypt hasher, JWT provider | *(Testcontainers)*<br>• Username is unique in the database<br>• **Simultaneous registration:** when the database rejects a duplicate username, the adapter reports "already exists" (not a generic database error)<br>• A generated token can be validated; an expired/tampered one is rejected |
| 4. Security & controllers | `POST /api/auth/register`, `POST /api/auth/login`, security rules | *(MockMvc)*<br>• Register → `201`; duplicate (including the simultaneous case) → `409`, never `500`; invalid → `400`<br>• Login OK → `200` + token; wrong credentials → `401`<br>• Protected route without token, or with an expired token → `401`; public route → `200` |
| 5. Frontend | Register and login screens; token stored and sent on requests | • Form shows validation errors<br>• After login, protected actions become available<br>• **Session expired:** a `401` on a protected action shows "Your session expired, please log in again" and opens the login screen<br>• Logout deletes the token in the browser, and protected actions are no longer available |

**Requirements covered:** registration, authentication, public vs. protected routes, secondary entity for users, simultaneous registrations, session expiry.

> **Accepted behavior (no test needed):** logout deletes the token in the browser only; the server does not cancel it, so a copied token keeps working until it expires. This is standard for JWT, and the short expiry limits the risk.

---

## 6. US-03 — Data Synchronization (Import)

**Goal:** an authenticated user imports a Pokémon into the local database. The detail view (US-02) shows logged-in users whether a Pokémon is already imported.

**How TDD is used in this story**
- The import rules (empty proprietary fields, no duplicates) are unit-tested first with a fake repository and a fake catalog.
- Tests against a **real PostgreSQL (Testcontainers)** are written before the repository, including **two imports running at the same time**.
- The "imported" indicator is driven by web tests for each type of user (visitor / logged-in, imported / not imported).

| Step | What we build | 🔴 Tests written first |
|---|---|---|
| 1. Domain | `Pokemon` entity: PokeAPI data + proprietary fields (local name, local region, tags) | • A newly imported Pokémon has empty proprietary fields<br>• PokeAPI data (including category and types) is kept as received |
| 2. Use case | `ImportPokemonUseCase` — calls PokeAPI **first**, then saves in a short database transaction | • Fetches the Pokémon from the catalog and saves it<br>• Already imported → "already exists" error<br>• Not found in PokeAPI → "not found" error<br>• PokeAPI is not called inside the database transaction |
| 3. Database adapter | `PokemonRepository` (JPA) + Flyway table | *(Testcontainers)*<br>• Saves and reads a Pokémon with its tags<br>• Database rejects a duplicate `pokeapiId`<br>• **Simultaneous import:** two imports of the same Pokémon run at the same time → exactly one is saved, the other gets "already exists" (not a generic database error) |
| 4. Controllers | `POST /api/local-pokemon` (import), `GET /api/local-pokemon` (list), `GET /api/local-pokemon/{id}` (view) | *(MockMvc)*<br>• Import → `201`; duplicate (including the simultaneous case) → `409`, never `500`; not in PokeAPI → `404`<br>• Local list is paginated<br>• Viewing a local Pokémon that doesn't exist (e.g. deleted by another user) → `404`<br>• Without login → `401` |
| 5. Frontend | "Import" button on the detail screen; "My local Pokémon" screen and local detail screen | • Import button calls the API and shows success<br>• **Import button is disabled while importing** (no double submit)<br>• Import returns `409` (someone imported it first) → the screen refreshes to "Already imported – View local version"<br>• Opening a local Pokémon that returns `404` → shows "This Pokémon was deleted by another user" with a link back to the local list<br>• Data is reloaded when the user returns to the browser tab<br>• Screens load fresh data when opened, including when going back to them<br>• After an import, going back to the catalog detail shows "Already imported" (not the Import button) |
| 6. "Imported" indicator on the detail view | `GET /api/pokemon/{idOrName}` adds, for logged-in users, whether the Pokémon is imported and its local id. The check is a fresh database lookup, **not** cached. | *(MockMvc)*<br>• Logged-in user, imported Pokémon → `imported: true` with its local id<br>• Logged-in user, not imported → `imported: false`<br>• Visitor → no import information<br>• Right after an import, the detail shows it as imported (the cache doesn't hide it)<br>*(Frontend)*<br>• Imported → shows "Already imported" and **"View local version"** instead of the Import button<br>• Not imported → shows the Import button<br>• Visitor → no Import button |

**Requirements covered:** persistence in a relational database, proprietary fields, unique key, simultaneous imports, PokeAPI outside the transaction, protected route, imported indicator on the detail view.

> **Accepted behavior (no test needed):** the local list is split into pages by position. If another user adds or deletes a Pokémon while someone is moving between pages, the positions shift, so one Pokémon may appear on two pages or be skipped once. Nothing is lost or damaged; reloading shows the correct list.
>
> **Accepted behavior (no test needed):** there are no live updates. An open screen does not change by itself the instant another user deletes or edits the Pokémon shown; it updates on its next load (or when the user returns to the tab), and any action on outdated data gets a clear `404` or `409` (both tested above and in US-04).

---

## 7. US-04 — Local Data Modification

**Goal:** an authenticated user updates the proprietary fields or deletes a local Pokémon.

**How TDD is used in this story**
- Each validation rule (`400`, `404`) is written as a test before the code that enforces it.
- **Every concurrency scenario is written as a test first:** two users editing, edit and delete overlap, double delete, tags-only change. These tests run against a real database, because the version check happens there.
- The frontend's reaction to `409` and the disabled Save button are also test-first.

| Step | What we build | 🔴 Tests written first |
|---|---|---|
| 1. Domain | Update rules on `Pokemon` | • Only local name, local region and tags can change<br>• Local name has a valid length; tags are not blank and not duplicated |
| 2. Use cases | `UpdateLocalPokemonUseCase`, `DeleteLocalPokemonUseCase` — both receive the version the user loaded | • Updates the proprietary fields<br>• Pokémon not found → "not found" error<br>• Update with a version that doesn't match → "conflict" error<br>• **Delete** with a version that doesn't match → "conflict" error (someone changed it after it was loaded) |
| 3. Database adapter | `@Version` field for optimistic locking | *(Testcontainers)*<br>• Saving with an old version fails (two users editing at the same time)<br>• Deleting with an old version fails (edit and delete overlap)<br>• Changing **only the tags** increases the version too |
| 4. Controllers | `PUT /api/local-pokemon/{id}` (version in the body), `DELETE /api/local-pokemon/{id}?version=` | *(MockMvc)*<br>• Update OK → `200` with the new version<br>• Not found (or deleted meanwhile) → `404`<br>• Malformed JSON, invalid values or read-only fields → `400`<br>• **Missing version** on update or delete → `400`<br>• Old version on update or delete → `409`<br>• Delete → `204`; deleting it again (two users deleting) → `404`<br>• Without login → `401` |
| 5. Frontend | Edit form and delete button | • Form validates before sending<br>• **Save button is disabled while saving** (no double submit)<br>• On `409` (edit or delete), shows "changed by someone else, please reload"<br>• Delete asks for confirmation<br>• **Delete button is disabled while deleting** (no double submit)<br>• After a save, going back to the local list shows the **new** values<br>• After a delete, going back to the local list no longer shows the Pokémon<br>• **A reload while the edit form is open doesn't change the form's version or the user's typing** |

**Requirements covered:** update of local data, `404`, `400`, defensive validation, delete (CRUD), and all concurrency scenarios: simultaneous edits, edit and delete overlap, edit after delete, double delete, tags-only changes, double submit.

> **Accepted behavior (no test needed):** if a save succeeds but the answer never reaches the browser (network drop), a retry gets `409` because the version already changed. The message asks the user to reload, and they see their change was saved.

---

## 8. Delivery

1. **Seed data:** Flyway script with a few imported Pokémon and demo users (mock credentials).
2. **Docker:** Dockerfile for the backend; Docker Compose for app + PostgreSQL (+ frontend).
3. **Documentation:** README with setup steps and technical notes; Swagger UI for the API.
4. **Final check:** all tests green, coverage report (JaCoCo), no browser console warnings.

---

## 9. Definition of Done (for every story)

- ✅ Tests were written **before** the code, and all pass.
- ✅ Functional and technical requirements of the story are covered by at least one test.
- ✅ Errors return the correct status code and the common error format.
- ✅ The frontend screen works end-to-end with the backend.
- ✅ Code is refactored and readable.
