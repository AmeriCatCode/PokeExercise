# Task API (GenAI exercise)

Task Management REST API generated from [`../PROMPT.md`](../PROMPT.md).
Standalone project: its own Gradle wrapper, in-memory H2 database and port (**8081**), so it runs
alongside the Pokédex backend (8080) and frontend (3000) without touching them.

Stack: Java 21 · Spring Boot 3.5 · Spring Web / Data JPA / Security (JWT resource server) · Jakarta Validation · H2 · springdoc (Swagger UI) · JUnit 5 · Mockito · Gradle.

## Run

All commands from this folder (`genai-exercise/task-api`).

```bash
./gradlew bootRun
```

Seeded users: `alice`, `bob` (see `src/main/resources/data.sql`). The database is in memory and resets on restart.

## Get a token

Registration, login and token issuing are out of scope for the API. For local use, `tools/DevToken.java` mints an
HS256 JWT (valid 1 hour) with the same dev secret the API checks. It needs only the JDK:

```bash
TOKEN=$(java tools/DevToken.java alice)
```

PowerShell: `$TOKEN = java tools/DevToken.java alice`

If you set `APP_JWT_SECRET` (at least 32 characters) for the API, set it for the helper too.

## Try it in the browser (Swagger UI)

Opening `http://localhost:8081/api/tasks` directly in a browser always gives 401: the browser can't send the
bearer token. Use Swagger UI instead:

1. Open http://localhost:8081/swagger-ui.html
2. Click **Authorize**, paste the token (without the word `Bearer`), click **Authorize**, then **Close**.
3. Pick an endpoint → **Try it out** → **Execute**.

## Try it with Postman

Import `postman/task-api.postman_collection.json`, open the collection's **Variables** tab, paste the token into
`token` (current value) and save. Run the requests top to bottom; **Create** stores the new id in `taskId`.
To test ownership, create a task with an `alice` token, then switch `token` to a `bob` token: Get, Update
and Delete on that id return 404.

## Try it from PowerShell

```powershell
$h = @{ Authorization = "Bearer $TOKEN" }
Invoke-RestMethod -Uri http://localhost:8081/api/tasks -Headers $h
Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/tasks -Headers $h -ContentType "application/json" -Body '{"title":"Prepare demo","status":"TODO","dueDate":"2026-10-20"}'
```

## Endpoints

| Method | Path              | Success | Errors             |
|--------|-------------------|---------|--------------------|
| POST   | `/api/tasks`      | 201 + `Location` | 400, 401  |
| GET    | `/api/tasks`      | 200     | 401                |
| GET    | `/api/tasks/{id}` | 200     | 400, 401, 404      |
| PUT    | `/api/tasks/{id}` | 200     | 400, 401, 404      |
| DELETE | `/api/tasks/{id}` | 204     | 400, 401, 404      |

Request body (POST and PUT):

```json
{ "title": "Prepare demo", "description": "optional", "status": "TODO", "dueDate": "2026-10-20" }
```

- `title` required, max 150 characters (trimmed); `status` required: `TODO`, `IN_PROGRESS` or `DONE`;
  `description` optional, max 2000; `dueDate` optional, ISO date.
- Unknown fields (for example `userId`) are rejected with 400: the owner always comes from the JWT `sub` claim.
- A task that doesn't exist and another user's task both return 404.
- Errors use Spring's `ProblemDetail` (`application/problem+json`); validation errors list fields under `errors`.

Example:

```bash
curl -i -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"title":"Prepare demo","status":"TODO","dueDate":"2026-10-20"}' http://localhost:8081/api/tasks
```

## Test

```bash
./gradlew test
```

- `TaskServiceTest` — Mockito unit tests for the service rules.
- `TaskControllerIntegrationTest` — full HTTP tests (MockMvc, H2, JWT): create, validation and malformed input,
  ownership (404 for other users' tasks), update, delete, missing or unknown-user tokens.
