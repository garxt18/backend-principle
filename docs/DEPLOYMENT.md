# Deployment Guide

Put Backend Playground on the internet so your friends can sign up. Three options, easiest first.
All of them use the same `Dockerfile`, so what you tested locally is exactly what runs online.

| Option | Cost | Effort | Good for |
|---|---|---|---|
| **A. Render (Blueprint)** | Free tier available | ~10 minutes, all in the browser | Sharing with friends quickly |
| **B. Your own server (VPS) + Docker Compose + Caddy** | ~$4-6/month (or a free-tier VM) | ~30 minutes | Always-on, full control, great learning |
| **C. Any container platform** (Railway, Fly.io, Koyeb, AWS App Runner...) | varies | ~15 minutes | If you already use one |

Before any option: push your latest code to GitHub (`git push`).

---

## Option A - Render (one-click Blueprint)

The repository contains `render.yaml`, which describes the web service **and** a PostgreSQL database.

1. Create an account at https://render.com (sign in with GitHub).
2. Dashboard -> **New** -> **Blueprint** -> select the `backend-principle` repository.
3. Render reads `render.yaml` and asks for the two values marked `sync: false`:
   - `APP_ADMIN_EMAIL` - your email (becomes the admin account)
   - `APP_ADMIN_PASSWORD` - at least 12 characters
4. Click **Apply**. Render builds the Docker image (~5-8 minutes the first time) and starts it.
5. Open the URL Render shows (e.g. `https://backend-playground.onrender.com`) and share it.

How it connects: Render gives the app one `DATABASE_URL` (`postgres://user:pass@host/db`). The app converts it
to Spring's JDBC settings automatically (`DatabaseUrlEnvironmentPostProcessor`). `APP_JWT_SECRET` is generated
by Render, and `APP_SECURE_COOKIE=true` because Render serves HTTPS.

Free-tier caveats (check Render's current pricing page, these change):
- Free web services **sleep after ~15 minutes without traffic**; the next visit takes up to a minute to wake up.
- The free instance has little memory (512 MB). If you see restarts with out-of-memory errors, move the web
  service to the smallest paid instance.
- Free PostgreSQL databases on Render **expire after a limited time**. For long-term free hosting, create a free
  Postgres at https://neon.tech, copy its connection string, and set it as `DATABASE_URL` on the web service
  (remove the `playground-db` database from the Blueprint).

Every `git push` to your main branch redeploys automatically.

---

## Option B - your own server with Docker Compose + HTTPS

You rent a small Linux VM, install Docker, and run `docker-compose.prod.yml`. **Caddy** sits in front and gets
free HTTPS certificates from Let's Encrypt automatically. Postgres and the app are not exposed to the internet;
only Caddy (ports 80/443) is.

```
Internet --HTTPS--> Caddy (:443) --> app (:8080) --> db (:5432, private)
```

1. **Create a server**: Ubuntu 24.04, 1 GB RAM minimum (2 GB is comfortable). Any provider works
   (DigitalOcean, Hetzner, AWS Lightsail, Oracle Cloud free tier...).
2. **Point a domain** at it: create an `A` record like `learn.yourdomain.com -> <server IP>`.
   (No domain? Free subdomains exist, e.g. DuckDNS.)
3. **Install Docker** on the server:
   ```bash
   ssh root@<server-ip>
   curl -fsSL https://get.docker.com | sh
   ```
4. **Get the code and configure it**:
   ```bash
   git clone https://github.com/garxt18/backend-principle.git
   cd backend-principle
   cp .env.example .env
   nano .env      # set DOMAIN, DB_PASSWORD, APP_JWT_SECRET (openssl rand -base64 48), admin email/password
   ```
5. **Start it**:
   ```bash
   docker compose -f docker-compose.prod.yml up -d --build
   docker compose -f docker-compose.prod.yml logs -f app     # wait for "Started PlaygroundApplication"
   ```
6. Open `https://learn.yourdomain.com`. Caddy fetches the certificate on the first request.

Updating after you push new code:

```bash
cd backend-principle && git pull
docker compose -f docker-compose.prod.yml up -d --build
```

Backups (run daily with cron):

```bash
docker compose -f docker-compose.prod.yml exec -T db pg_dump -U playground playground | gzip > backup-$(date +%F).sql.gz
```

Firewall: allow only SSH (22), HTTP (80) and HTTPS (443) - e.g. `ufw allow OpenSSH && ufw allow 80 && ufw allow 443 && ufw enable`.

---

## Option C - other container platforms

Any platform that runs a Dockerfile works. Give it:

| Variable | Value |
|---|---|
| `DATABASE_URL` | the `postgres://...` connection string of a PostgreSQL database (or `DB_URL` + `DB_USERNAME` + `DB_PASSWORD`) |
| `APP_JWT_SECRET` | 32+ random characters |
| `APP_SECURE_COOKIE` | `true` (the platform serves HTTPS) |
| `PORT` | usually set by the platform automatically; the app listens on it |
| `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD` | optional first admin |

Health check path: `/actuator/health/readiness`.

---

## Production checklist

- [ ] `APP_JWT_SECRET` is long and random, and never committed to Git
- [ ] `APP_SECURE_COOKIE=true` (the site is served over HTTPS)
- [ ] The database is not reachable from the internet
- [ ] Database backups are scheduled (Option B) or provided by the platform
- [ ] An admin account exists (`APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD`), then those two variables can be removed
- [ ] `LOG_FORMAT=ecs` for JSON logs
- [ ] You opened the site on your phone and it works
