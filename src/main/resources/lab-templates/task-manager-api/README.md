# Task Manager API

Roadmap **Project 1** - Java + Spring Boot + PostgreSQL.

## Build order (the order the Rebuild Lab walks you through)

1. `pom.xml` - dependencies
2. `application.yml` - configuration
3. `V1__create_tasks.sql` - the table
4. `TaskManagerApplication` - entry point
5. `TaskStatus`, `TaskPriority`, `Task` - the entity
6. `TaskRepository` - data access
7. DTOs - `CreateTaskRequest`, `UpdateTaskRequest`, `TaskResponse`
8. Errors - `ResourceNotFoundException`, `GlobalExceptionHandler`
9. `TaskService` - business logic + transactions
10. `TaskController` - HTTP endpoints
11. Tests - `TaskServiceTest` (Mockito), `TaskControllerTest` (MockMvc)
12. `Dockerfile`, `docker-compose.yml`

## Run

```bash
docker compose up -d db
DB_URL=jdbc:postgresql://localhost:5433/tasks ./mvnw spring-boot:run
curl -X POST localhost:8081/api/tasks -H 'Content-Type: application/json' -d '{"title":"Learn Spring"}'
curl 'localhost:8081/api/tasks?status=TODO&page=0&size=10'
```

## Extend it (your homework)

- Add a `Project` entity with `@OneToMany` tasks.
- Add JWT login (Level 6) so every user sees only their own tasks.
- Add Testcontainers integration tests (Level 7).
- Cache `GET /api/tasks/{id}` with Redis (Level 8).
