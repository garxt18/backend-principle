# Codebase Tour: where each roadmap concept lives

This repository is itself a study resource. Open it in the Rebuild Lab (**Backend Playground** template) or
in IntelliJ and use this map to find working, tested examples of each roadmap level.

| Level | Concept | Where to look |
|---|---|---|
| L0/L2 | Records, enums, streams, `Optional`, switch expressions | `roadmap/RoadmapDtos.java`, `progress/ProgressService.java`, `mentor/MentorService.java#systemPrompt` |
| L2 | Thread safety, `synchronized`, concurrent caches | `common/ratelimit/TokenBucket.java`, `common/ratelimit/RateLimiter.java` |
| L2 | Pure algorithms (greedy scheduling, topological sort, Kahn's algorithm) | `planner/PlanGenerator.java`, `lab/BuildOrderPlanner.java` |
| L3 | Dependency injection (constructor injection everywhere), `@Configuration` + `@Bean` | `common/config/AppConfig.java`, `auth/SecurityConfig.java` |
| L3 | Type-safe config with validation | `auth/JwtProperties.java`, `lab/LabProperties.java`, `application.yml` |
| L3 | REST controllers, `ResponseEntity`, status codes, `Cache-Control` | `roadmap/RoadmapController.java`, `planner/PlannerController.java`, `auth/AuthController.java` |
| L3 | Entities, relationships, `@Version`, lifecycle callbacks | `user/User.java`, `roadmap/RoadmapLevel.java`, `planner/StudyPlan.java` |
| L3 | Spring Data: derived queries, JPQL, fetch joins, DTO projections, `@Modifying` | `roadmap/RoadmapLevelRepository.java`, `lab/LabFileRepository.java`, `auth/RefreshTokenRepository.java` |
| L3 | Transactions: `readOnly`, `noRollbackFor`, dirty checking | `auth/RefreshTokenService.java#rotate`, `user/UserService.java` |
| L3 | Validation + global error handling (RFC 9457) | `common/error/GlobalExceptionHandler.java`, `*Controller` request records |
| L3 | Actuator health probes | `application.yml` (`management.*`) |
| L4 | Pagination, idempotent PUT, 201/204/404/409/429 | `admin/AdminController.java`, `progress/ProgressController.java` |
| L4 | Rate limiting (token bucket, `Retry-After`) | `common/ratelimit/RateLimitFilter.java` |
| L4 | OpenAPI / Swagger | `common/config/AppConfig.java` -> http://localhost:8080/swagger-ui.html |
| L5 | Schema design, constraints, partial unique index, cascades | `src/main/resources/db/migration/V1__init_schema.sql` |
| L5 | Upserts with `ON CONFLICT`, plain JDBC where JPA doesn't fit | `mentor/AiUsageRepository.java`, `mentor/AiExplanationCache.java` |
| L6 | JWT (OAuth2 Resource Server), BCrypt, refresh-token rotation and reuse detection | `auth/*` |
| L6 | HttpOnly/SameSite cookies, CORS, CSP headers, timing-safe login | `auth/AuthController.java`, `auth/SecurityConfig.java`, `auth/AuthService.java#login` |
| L6 | Authorization: roles, ownership checks, 404-not-403 | `lab/LabService.java#visibleProject`, `planner/StudySessionService.java#delete` |
| L6 | Hostile-upload handling (zip slip, zip bomb) | `lab/ZipProjectImporter.java` |
| L7 | Unit tests (pure functions), parameterized tests | `src/test/.../planner`, `.../lab`, `.../common` |
| L7 | Integration tests with Testcontainers + MockMvc | `AbstractIntegrationTest.java`, `ApiFlowIntegrationTest.java` |
| L7 | Test doubles for external services | `mentor/MentorIntegrationTest.java` (stub `ChatModel`) |
| L8 | Cache-aside with `@Cacheable` / `@CacheEvict` | `roadmap/RoadmapService.java` (Caffeine; swap to Redis as an exercise) |
| L12 | Multi-stage Dockerfile, non-root user, compose with health checks | `Dockerfile`, `docker-compose.yml` |
| L13 | CI pipeline | `.github/workflows/ci.yml` |
| L14 | Correlation ids, structured logs, Prometheus metrics | `common/web/CorrelationIdFilter.java`, `application.yml` (`logging.structured`, `management`) |
| Bonus | Spring AI: `ChatClient`, system prompts, structured output, caching, quotas, provider switch | `mentor/MentorService.java`, `application.yml` (`spring.ai.*`) |

## Exercises that extend this codebase

1. **L8** - Replace Caffeine with Redis (`spring-boot-starter-data-redis`, `spring.cache.type=redis`) and move
   the rate limiter's buckets into Redis so several app instances share limits.
2. **L9** - Publish a `TopicCompleted` event when progress changes to DONE and consume it to send a weekly summary.
3. **L10** - Extract the AI mentor into its own service behind an API gateway.
4. **L13** - Write Kubernetes manifests using `/actuator/health/readiness` and `/liveness` as probes.
5. **L14** - Add Grafana dashboards for `http_server_requests_seconds` (p95 latency, error rate).
6. **Spring AI** - Add RAG: embed topic notes with pgvector and let the mentor answer from your own notes.
