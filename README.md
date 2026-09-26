# Backend Playground

A multi-user learning platform for becoming a production-ready Java backend engineer, built **with** the
stack it teaches: Java 21, Spring Boot 4, PostgreSQL, Spring Security (JWT), Flyway, Spring AI, Docker
and GitHub Actions.

It follows the *Backend Engineering Roadmap 2026* (levels 0-15, plus a bonus Spring AI level) and gives you
four tools:

| Feature | What it does |
|---|---|
| **Roadmap** | 17 levels, 101 topics with hour estimates, practice tasks, level projects and curated **Hindi + English** YouTube resources. Track status, confidence and notes per topic. |
| **Planner** | Generates a week-by-week schedule from your hours/week (20 h/week -> about 6 months), shows whether you're ahead or behind, and logs study sessions with a streak and heatmap. |
| **Rebuild Lab** | Upload any project as a `.zip` (e.g. your own Spring Boot + Postgres project) and **retype it line by line**, in the order a senior engineer would build it: `pom.xml` -> config -> SQL -> entities -> repositories -> DTOs -> services -> controllers -> tests -> Docker. Every line has an instant explanation (annotations, keywords, Spring Data queries translated to SQL, YAML keys, Maven artifacts, SQL, Dockerfile). Two templates are built in: a small **Task Manager API** and **this playground's own source code**. |
| **AI Mentor** | Spring AI powered mentor (OpenAI, Anthropic or a free local Ollama model). Ask questions in Hindi/Hinglish/English, get deeper explanations of any line, or generate a quiz for any topic. Per-user quotas and a shared answer cache keep costs down. |

## Quick start

### Option A - Docker (easiest)

```bash
cp .env.example .env          # edit APP_JWT_SECRET and the admin account
docker compose up --build
```

Open http://localhost:8080, sign up, and go to **Rebuild Lab -> Task Manager API -> Start rebuilding**.

### Option B - run from your IDE

1. Start PostgreSQL 16 with a database `playground` (user/password `playground`), or run just the DB:
   `docker compose up -d db`
2. Run `./mvnw spring-boot:run` (or run `PlaygroundApplication` in IntelliJ).
3. Open http://localhost:8080. API docs: http://localhost:8080/swagger-ui.html

Flyway creates the schema automatically; the roadmap and Lab templates are seeded on startup.

### Turn on the AI mentor (optional)

Everything else works without it.

| Provider | Environment variables |
|---|---|
| OpenAI | `AI_PROVIDER=openai`, `OPENAI_API_KEY=...` (optional `SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL`) |
| Anthropic | `AI_PROVIDER=anthropic`, `ANTHROPIC_API_KEY=...` (optional `ANTHROPIC_MODEL`, default `claude-sonnet-5`) |
| Ollama (free, local) | `AI_PROVIDER=ollama`, `OLLAMA_MODEL=llama3.2`; with Docker: `docker compose --profile ai up`, then `docker compose exec ollama ollama pull llama3.2` |

Fair-use limits: `APP_AI_DAILY_QUOTA` (default 40 requests per user per day) and `APP_AI_PER_MINUTE` (6).

### Rebuild your own project

1. Zip your project folder (or on GitHub: **Code -> Download ZIP**).
2. **Rebuild Lab -> Upload your project**. Build output (`target/`, `build/`, `.git/`, `node_modules/`) is
   skipped automatically; uploads are private to your account.
3. Open it and press **Start rebuilding**. Type each line (indentation is optional) and press Enter.
   **Blind mode** hides the line so you type it from memory; **Read mode** lets you click any line for its explanation.

## Architecture

```
Browser (vanilla JS SPA, no build step)          src/main/resources/static
   |  JSON over HTTPS, Bearer access token (memory) + HttpOnly refresh cookie
   v
Filters: CorrelationId -> RateLimit (token bucket) -> Spring Security (JWT resource server)
   v
Controllers (thin: HTTP, validation, status codes)
   v
Services (business rules, @Transactional, caching, quotas)
   v
Repositories (Spring Data JPA; JdbcClient for upserts) -> PostgreSQL (Flyway migrations)
                                     Spring AI ChatClient -> OpenAI / Anthropic / Ollama
```

Package-by-feature layout (`com.backendprinciple.playground`):

| Package | Responsibility |
|---|---|
| `auth` | Register/login, JWT issuing, refresh-token rotation with reuse detection, security config |
| `user`, `admin` | Profiles, password change, admin bootstrap, user management |
| `roadmap` | Levels/topics/resources, JSON seeder, cached read API, admin resource CRUD |
| `progress` | Per-user topic status, summary, streaks |
| `planner` | Plan generation algorithm, weekly view, study sessions, heatmap |
| `lab` | Zip import (zip-slip & zip-bomb safe), file classification, build ordering, line explainer, templates |
| `mentor` | Spring AI mentor: ask, explain line (cached), quiz (structured output), quotas |
| `common` | Error handling (RFC 9457 ProblemDetail), correlation ids, rate limiting, `@CurrentUser` |

See **[docs/CODEBASE_TOUR.md](docs/CODEBASE_TOUR.md)** for where each roadmap concept lives in this code.

## Production notes

- **Multi-user by design:** every user-owned row carries `user_id`; queries always filter by it and other
  users' resources return 404 (not 403) so ids cannot be probed.
- **Auth:** short-lived HS256 access tokens (15 min) kept in memory; 14-day refresh tokens in an
  `HttpOnly; SameSite=Strict` cookie, stored hashed (SHA-256), rotated on every use, and a reused token
  revokes the whole session family. Passwords use BCrypt via `DelegatingPasswordEncoder`.
- **Hardening:** CSP without inline scripts, per-IP rate limits (strict on auth), request size limits,
  safe zip handling, no stack traces in responses, optimistic locking on users.
- **Operations:** `/actuator/health/liveness` and `/readiness` probes, Prometheus metrics at
  `/actuator/prometheus` (admin only), JSON logs with `LOG_FORMAT=ecs`, correlation id on every log line
  and error response, graceful shutdown, virtual threads.
- **Configuration:** everything via environment variables - see `application.yml`. In production set at least
  `APP_JWT_SECRET` (32+ random chars), `APP_SECURE_COOKIE=true` (behind HTTPS), `DB_*`, and optionally
  `APP_ADMIN_EMAIL`/`APP_ADMIN_PASSWORD` to create the first admin.
- **Client IPs:** `FORWARD_HEADERS_STRATEGY=native` (default) trusts `X-Forwarded-For` only from private-network
  proxies such as your load balancer or ingress. If the app is reachable directly through a private network
  hop, set it to `none` so clients cannot spoof their IP to dodge rate limits.
- **Scaling out:** the rate limiter and caches are in-memory (per instance). Moving them to Redis is the
  Level 8 exercise in the roadmap.

## Tests

```bash
./mvnw verify                                  # unit + integration tests (integration tests need Docker)
TEST_DB_URL=jdbc:postgresql://localhost:5432/playground_test ./mvnw verify   # or use an existing Postgres
```

Integration tests start PostgreSQL with Testcontainers. Without Docker and without `TEST_DB_URL` they are
skipped. The AI mentor is tested with a stub `ChatModel`, so no API key is needed.

## Learning path

Your study guide, with the Coder Army Java and Spring Boot playlists plus Hindi and English resources for
every other level, is in **[docs/LEARNING_PATH.md](docs/LEARNING_PATH.md)**.
