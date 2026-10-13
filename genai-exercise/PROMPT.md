# Prompt

You are a Senior Java Backend Engineer. Generate a simple RESTful Task Management API using:

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Spring Security with JWT
- Jakarta Validation
- H2
- JUnit 5 and Mockito
- Gradle

Assume a basic `User` model and authentication already exist.

Create a `Task` entity with:

- `id`: Long
- `title`: required, max 150 characters
- `description`: optional
- `status`: `TODO`, `IN_PROGRESS`, `DONE`
- `dueDate`: LocalDate
- `user`: User

Each task belongs to one user.

## API Rules

The authenticated user must come from the JWT/security context. Never accept `userId` or ownership information from the request body.

Users can only access, update, or delete their own tasks. If a task does not exist or belongs to another user, return `404 Not Found`.

Implement:

- `POST /api/tasks`
- `GET /api/tasks`
- `GET /api/tasks/{id}`
- `PUT /api/tasks/{id}`
- `DELETE /api/tasks/{id}`

Use `201` for creation, `200` for reads/updates, `204` for deletion, `400` for invalid input, and `404` for missing or unauthorized tasks.

## Validation and Structure

Use request/response DTOs, Jakarta Validation, `ProblemDetail` for `400` errors, and centralized exception handling with `@RestControllerAdvice`.

Use a simple layered structure with Controller, Service, Repository, DTOs, Entity, Mapper, exception handling, and security integration. Keep business logic in the service layer.

## Tests and Done Criteria

Add representative tests for creation, invalid input, retrieval, update, delete, and trying to access another user's task.

The solution is complete when the project compiles, tests pass, CRUD works, ownership is enforced from the JWT user, and validation/error handling works correctly.

Do not implement login, JWT generation, roles, frontend, pagination, or notifications.

Start by showing the project structure, then generate the implementation file by file. Keep it simple and avoid unnecessary abstractions.