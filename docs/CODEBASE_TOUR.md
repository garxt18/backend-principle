# Codebase Tour: where each roadmap concept lives

This repository is itself a study resource. Open it in the Rebuild Lab (**Backend Playground** template) or
in IntelliJ and use this map to find working, tested examples of each roadmap level.

| Level | Concept | Where to look |
|---|---|---|
| L0/L2 | Records, enums, streams, `Optional`, `Collectors.groupingBy` | `roadmap/RoadmapDtos.java`, `progress/ProgressService.java`, `lab/JavaFileModel.java` |
| L2 | Thread safety, `synchronized`, concurrent caches | `common/ratelimit/TokenBucket.java`, `common/ratelimit/RateLimiter.java` |
| L2 | Pure algorithms (greedy scheduling, topological sort, Kahn's algorithm) | `planly/PlanGenerator.java`, `lab/BuildOrderPlanner.java` |
| L3 | Dependency injection (constructor injection everywhere), `@Configuration` + `@Bean` | `common/config/AppConfig.java`, `auth/SecurityConfig.java` |
| L3 | Type-safe config with validation | `auth/JwtProperties.java`, `lab/LabProperties.java`, `application.yml` |
| L3 | REST controllers, `ResponseEntity`, status codes, `Cache-Control` | `roadmap/RoadmapController.java`, `planly/PlanlyController.java`, `auth/AuthController.java` |
| L3 | Entities, relationships, `@Version`, lifecycle callbacks | `user/User.java`, `roadmap/RoadmapLevel.java`, `planly/StudyPlan.java` |
| L3 | Spring Data: derived queries, JPQL, fetch joins, DTO projections, `@Modifying` | `roadmap/RoadmapLevelRepository.java`, `lab/LabFileRepository.java`, `auth/RefreshTokenRepository.java` |
| L3 | Transactions: `readOnly`, `noRollbackFor`, dirty checking | `auth/RefreshTokenService.java#rotate`, `user/UserService.java` |
| L3 | Validation + global error handling (RFC 9457) | `common/error/GlobalExceptionHandler.java`, `*Controller` request records |
| L3 | Actuator health probes | `application.yml` (`management.*`) |
| L4 | Pagination, idempotent PUT, 201/204/404/409/429 | `admin/AdminController.java`, `progress/ProgressController.java` |
| L4 | Rate limiting (token bucket, `Retry-After`) | `common/ratelimit/RateLimitFilter.java` |
| L4 | OpenAPI / Swagger | `common/config/AppConfig.java` -> http://localhost:8080/swagger-ui.html |
| L5 | Schema design, constraints, partial unique index, cascades | `src/main/resources/db/migration/V1__init_schema.sql` |
| L5 | Versioned migrations (never edit an applied one), partial indexes, CHECK constraints ("exactly one of two columns") | `db/migration/V2__dsa_sheet_planly_line_notes.sql`, `db/migration/V3__playlist_roadmap_personal_resources.sql` |
| L5 | Keeping seed data in sync with the database (upsert by natural key, delete what left the seed) | `roadmap/RoadmapSeeder.java` |
| L6 | JWT (OAuth2 Resource Server), BCrypt, refresh-token rotation and reuse detection | `auth/*` |
| L6 | HttpOnly/SameSite cookies, CORS, CSP headers, timing-safe login | `auth/AuthController.java`, `auth/SecurityConfig.java`, `auth/AuthService.java#login` |
| L6 | Authorization: roles, ownership checks, 404-not-403 | `lab/LabService.java#visibleProject`, `planly/StudySessionService.java#delete` |
| L6 | Hostile-upload handling (zip slip, zip bomb), only-http(s) user links | `lab/ZipProjectImporter.java`, `roadmap/PersonalResourceController.java` |
| L4 | File download (`Content-Disposition`) and multipart upload endpoints | `lab/LabController.java` (`/progress/export`, `/progress/import`), `lab/LabSyncService.java` |
| L7 | Unit tests (pure functions), parameterized tests | `src/test/.../planly`, `.../lab`, `.../common` |
| L7 | Integration tests with Testcontainers + MockMvc | `AbstractIntegrationTest.java`, `ApiFlowIntegrationTest.java` |
| L7 | Parsing tests for config helpers | `common/config/DatabaseUrlEnvironmentPostProcessorTest.java` |
| L8 | Cache-aside with `@Cacheable` / `@CacheEvict`, and keeping per-user data out of a shared cache | `roadmap/RoadmapService.java` vs `roadmap/PersonalResourceService.java` (Caffeine; swap to Redis as an exercise) |
| L12 | Multi-stage Dockerfile, non-root user, compose with health checks, HTTPS reverse proxy | `Dockerfile`, `docker-compose.yml`, `docker-compose.prod.yml`, `deploy/Caddyfile` |
| L12 | 12-factor config: one `DATABASE_URL` from the platform | `common/config/DatabaseUrlEnvironmentPostProcessor.java`, `render.yaml` |
| L13 | CI pipeline | `.github/workflows/ci.yml` |
| L14 | Correlation ids, structured logs, Prometheus metrics | `common/web/CorrelationIdFilter.java`, `application.yml` (`logging.structured`, `management`) |
| L4 | Serving a single-page app: hashed assets cached for a year, deep links fall back to index.html | `common/config/SpaConfig.java` |
| Frontend | React + TypeScript: routing, auth context, TanStack Query caching and optimistic updates | `frontend/src/lib/*`, `frontend/src/pages/*` |

## Exercises that extend this codebase

1. **L8** - Replace Caffeine with Redis (`spring-boot-starter-data-redis`, `spring.cache.type=redis`) and move
   the rate limiter's buckets into Redis so several app instances share limits.
2. **L9** - Publish a `TopicCompleted` event when progress changes to DONE and consume it to send a weekly summary.
3. **L10** - Extract the Rebuild Lab into its own service behind an API gateway.
4. **L13** - Write Kubernetes manifests using `/actuator/health/readiness` and `/liveness` as probes.
5. **L14** - Add Grafana dashboards for `http_server_requests_seconds` (p95 latency, error rate).
6. **Spring AI (bonus)** - Add an optional "explain this line" button backed by a free local Ollama model.
