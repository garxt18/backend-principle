# Setup Guide

This guide takes you from a fresh laptop to Backend Playground running on `http://localhost:8080`.
Pick **Path A (Docker)** if you just want it running, or **Path B (IDE)** if you want to change code.

---

## 1. What is Docker, and why do we use it?

Your app needs a **PostgreSQL database** to run. Without Docker you would have to install PostgreSQL on
Windows/Mac/Linux, create a user, create a database, remember which version you installed... and your friend
would have to do exactly the same on their machine, where it might behave differently.

**Docker packages software together with everything it needs into a *container*** - a small, isolated
box that runs the same way on every computer.

| Word | Meaning | In this project |
|---|---|---|
| **Image** | A read-only template (like a class in Java) | `postgres:16-alpine`, or the image built from our `Dockerfile` |
| **Container** | A running instance of an image (like an object) | the `db` and `app` containers |
| **Dockerfile** | The recipe to build an image | builds the React app + Spring Boot jar, then copies the jar onto a small Java runtime |
| **docker compose** | Starts several containers together with one command | `docker-compose.yml` starts Postgres + the app and connects them |
| **Volume** | Storage that survives when a container is deleted | `pgdata` keeps your database data |

Why this matters for you as a backend engineer (roadmap Level 12):

- **"Works on my machine" disappears.** Same Postgres version, same Java version, everywhere.
- **One command** starts the whole system: `docker compose up`.
- **Nothing to uninstall.** `docker compose down -v` removes everything.
- **It is how production works.** The same image you run locally is what you deploy to the cloud.

> Hinglish mein: Docker ek dabba hai jisme app aur uski saari zaroorat ki cheezein (Java, Postgres) band hoti
> hain. Dabba har computer pe same chalta hai - isliye "mere laptop pe toh chal raha tha" wali problem khatam.

---

## 2. Install the tools

| Tool | Needed for | Download |
|---|---|---|
| **Git** | cloning the code | https://git-scm.com/downloads |
| **Docker Desktop** (Windows/Mac) or Docker Engine (Linux) | Path A, and the database in Path B | https://www.docker.com/products/docker-desktop/ |
| **JDK 21** (Temurin) | Path B | https://adoptium.net/ |
| **IntelliJ IDEA Community** | Path B (recommended IDE) | https://www.jetbrains.com/idea/download/ |
| **Node.js 22** | only if you want to edit the React UI with live reload | https://nodejs.org/ |

You do **not** need to install Maven: the project ships the Maven wrapper (`./mvnw`, or `mvnw.cmd` on Windows).
You do **not** need Node.js to build: Maven downloads its own copy for the React build.

Windows tip: Docker Desktop needs WSL 2 - its installer offers to enable it. Restart after installing.

Check everything:

```bash
git --version
docker --version          # Docker version 2x.x
docker compose version    # Docker Compose version v2.x
java -version             # openjdk 21 (Path B only)
```

---

## 3. Get the code

```bash
git clone https://github.com/garxt18/backend-principle.git
cd backend-principle
```

---

## Path A - run everything with Docker (easiest)

```bash
cp .env.example .env        # Windows PowerShell: copy .env.example .env
```

Open `.env` and set at least `APP_JWT_SECRET` to a long random string and your admin email/password.

```bash
docker compose up --build
```

The first build takes a few minutes (it downloads Java, Maven dependencies and Node). When you see
`Started PlaygroundApplication`, open **http://localhost:8080** and sign up.

Useful commands:

```bash
docker compose ps                 # what is running
docker compose logs -f app        # follow the app's logs
docker compose down               # stop (data is kept in the pgdata volume)
docker compose down -v            # stop AND delete the database
docker compose up --build         # rebuild after you change code
```

---

## Path B - run from IntelliJ (for development)

1. Start only the database in Docker:
   ```bash
   docker compose up -d db
   ```
   (No Docker? Install PostgreSQL 16 yourself and create user `playground` / password `playground` and a
   database `playground`.)
2. Open the folder in IntelliJ -> it detects Maven automatically -> wait for indexing.
3. Run `PlaygroundApplication` (green play button), or in a terminal:
   ```bash
   ./mvnw spring-boot:run            # Windows: mvnw.cmd spring-boot:run
   ```
   The first run builds the React UI too (about a minute). Later, to skip it while you only change Java:
   ```bash
   ./mvnw spring-boot:run -Dskip.frontend=true
   ```
4. Open http://localhost:8080. API docs: http://localhost:8080/swagger-ui.html

Flyway creates all tables automatically; the roadmap (with the Java and Spring Boot lecture lists) and the Rebuild Lab templates are loaded on startup.

### Editing the React UI with live reload

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173 - API calls are proxied to the Spring Boot app on :8080
```

Keep the Spring Boot app running in IntelliJ at the same time. Save a `.tsx` file and the browser updates instantly.

### Make yourself admin

Set these before starting the app (IntelliJ: Run -> Edit Configurations -> Environment variables):

```
APP_ADMIN_EMAIL=you@example.com
APP_ADMIN_PASSWORD=a-long-password-12+chars
```

Admins can manage users and edit the roadmap resources at `/admin`.

---

## Run the tests

```bash
./mvnw verify
```

Integration tests start a throw-away PostgreSQL with **Testcontainers** (needs Docker running). No Docker?
Point them at any Postgres database:

```bash
TEST_DB_URL=jdbc:postgresql://localhost:5432/playground_test ./mvnw verify
```

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `Connection to localhost:5432 refused` | The database is not running: `docker compose up -d db` |
| `port 5432 is already allocated` | You already have Postgres installed locally. Stop it, or change `"5432:5432"` to `"5433:5432"` and use `DB_URL=jdbc:postgresql://localhost:5433/playground` |
| `port 8080 is already in use` | Another app uses 8080. Run with `PORT=8081` |
| Docker says "Cannot connect to the Docker daemon" | Start Docker Desktop and wait until it says "running" |
| `JAVA_HOME is not set` / wrong Java version | Install JDK 21 and point IntelliJ (File -> Project Structure -> SDK) at it |
| Build fails in the `frontend` step behind a proxy | Build only the backend with `-Dskip.frontend=true`, or configure npm's proxy |
| Page shows `HTTP Status 400 – Bad Request` after logging in | Old cookies from other apps you ran on `localhost` made the request too big. Fixed in the code (limit raised to 64 KB); on an older copy, clear cookies for `localhost` (browser settings → Cookies → localhost → Delete) or use a private window |
| Logged out after restarting the app | Normal in dev if `APP_JWT_SECRET` changed; just log in again |
