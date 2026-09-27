# Backend Playground

A free, multi-user platform for becoming a production-ready Java backend engineer - share it with your friends.
Built **with** the stack it teaches: Java 21, Spring Boot 4, PostgreSQL, Spring Security (JWT), Flyway, React +
TypeScript, Docker and GitHub Actions.

| Feature | What it does |
|---|---|
| **Roadmap** | The *Backend Engineering Roadmap 2026*: 17 levels, 170 topics with hour estimates, practice tasks and level projects. **Java Basics, Advanced Java and Spring Boot follow Coder Army's two playlists lecture by lecture** (what each lecture teaches, a practice task and a direct video link). Every other level has curated Hindi + English resources: mark the one you **follow**, or attach **your own links** to any level or lecture. |
| **Dashboard** | "Continue with" your next lecture/topic and the resource you follow, streaks, weekly hours, level progress, and a direct link to **Striver's A2Z sheet** for DSA (no duplicate DSA sheet in the app). |
| **Planly** | Day-by-day study plans for any levels or lectures: **finish by a date** ("Java Basics in 7 days", "Spring Boot in 30 days") or **at your pace** (hours per day, chosen weekdays). Live preview, Today / Week / full schedule, catch-up re-planning, one-click time logging. Ticks are shared with the Roadmap and Dashboard - it is one record. |
| **Rebuild Lab** | Upload **any** Spring Boot / Java project as a `.zip` and retype it **line by line**, in the order a senior engineer would build it (`pom.xml` -> config -> SQL -> entities -> repositories -> DTOs -> services -> controllers -> tests -> Docker). Backend + database files are the main track; frontend files (TSX/HTML/CSS/JS) sit in a separate optional section. Every line shows **what it does and why you type it** right under the line you are typing, and each file starts with an outline of what you are about to build. **Download your progress** as a zip, keep typing in your IDE, and **sync it back**. Built-in templates: a small **Task Manager API** and **this app's own source**. |

Dark and light themes, responsive down to phone width, no API keys or paid services needed.

## Quick start

```bash
git clone https://github.com/garxt18/backend-principle.git && cd backend-principle
cp .env.example .env
docker compose up --build
```

Open http://localhost:8080 and sign up. New to Docker or want to run it from IntelliJ? Read
**[docs/SETUP.md](docs/SETUP.md)** - it explains what Docker is, why we use it, and every step.

## Deploy it for your friends

**[docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)** covers: **Render free + Neon Postgres + a free pinger** so it
never sleeps (all in the browser), an **Oracle Cloud Always Free VM** (free, never sleeps), your own server with
**Docker Compose + Caddy (automatic HTTPS)** (`docker-compose.prod.yml`), or any container platform.
Note: a free Render Postgres is deleted after 30 days - the Blueprint uses Neon instead.

## Architecture

```
React + TypeScript SPA (Vite)                     frontend/  -> built into the jar by Maven
   |  JSON over HTTPS; access token in memory + HttpOnly refresh cookie
   v
Filters: CorrelationId -> RateLimit (token bucket) -> Spring Security (JWT resource server)
   v
Controllers (HTTP, validation, status codes)
   v
Services (business rules, @Transactional, caching)
   v
Repositories (Spring Data JPA) -> PostgreSQL (Flyway migrations)
```

Backend packages (`com.backendprinciple.playground`, package-by-feature):

| Package | Responsibility |
|---|---|
| `auth` | Register/login, JWT issuing, refresh-token rotation with reuse detection, security config |
| `user`, `admin` | Profiles, password change, admin bootstrap, user management |
| `roadmap` | Levels/topics/resources (playlist levels with per-lecture links), JSON seeder that keeps the DB in sync, cached read API, admin resource CRUD, per-user followed resource and personal links |
| `progress` | Per-user topic status, summary, streaks |
| `planly` | Plan generation algorithm, weekly view, study sessions, heatmap |
| `lab` | Zip import (zip-slip & zip-bomb safe), file classification, build ordering, backend/frontend tracks, line explainer (what + why, file outline), line notes, progress export/sync, templates |
| `common` | Error handling (RFC 9457), correlation ids, rate limiting, `@CurrentUser`, SPA routing, `DATABASE_URL` support |

Frontend (`frontend/src`): React 19, React Router, TanStack Query (caching + optimistic updates), hand-written
design system in plain CSS with dark/light tokens, lazy-loaded pages. See
**[docs/CODEBASE_TOUR.md](docs/CODEBASE_TOUR.md)** for where each roadmap concept lives in the code.

## Production notes

- **Multi-user by design:** every user-owned row carries `user_id`; queries always filter by it and other users'
  data answers 404 (not 403), so ids cannot be probed.
- **Auth:** 15-minute HS256 access tokens kept in memory; 14-day refresh tokens in an `HttpOnly; SameSite=Strict`
  cookie, stored hashed, rotated on every use - a reused token revokes the whole session. Passwords use BCrypt.
- **Hardening:** strict CSP (no inline scripts), per-IP rate limits (strict on login), upload limits, safe zip
  handling, no stack traces in responses, optimistic locking.
- **Operations:** `/actuator/health/liveness` and `/readiness`, Prometheus metrics (admin only), JSON logs with
  `LOG_FORMAT=ecs`, correlation ids, graceful shutdown, virtual threads, year-long caching for hashed assets.
- **Client IPs:** `FORWARD_HEADERS_STRATEGY=native` (default) trusts `X-Forwarded-For` only from private-network
  proxies such as your load balancer; set it to `none` if the app is reachable directly through a private hop.
- **Configuration:** environment variables only - see `application.yml` and `.env.example`.

## Development

```bash
./mvnw spring-boot:run                         # backend + UI on :8080 (builds the UI on first run)
./mvnw spring-boot:run -Dskip.frontend=true    # faster when you only changed Java
cd frontend && npm install && npm run dev      # UI with live reload on :5173 (proxies /api to :8080)
./mvnw verify                                  # all tests (integration tests use Testcontainers / Docker)
```

## Learning path

Your study guide - the Coder Army Java and Spring Boot playlists plus Hindi and English resources for every
other level and a daily routine - is in **[docs/LEARNING_PATH.md](docs/LEARNING_PATH.md)**.
