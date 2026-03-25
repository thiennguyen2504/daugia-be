# Greenify Backend - Quick Start

## Prerequisites

- Docker Desktop (or Docker Engine + Compose plugin)

## Run With Docker

1. Create env file from template:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

2. Update values in `.env` (especially `DB_PASSWORD`, `JWT_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, mail credentials).

3. Start all services:

```bash
docker compose up --build
```

Services started:

- App: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI docs: `http://localhost:8080/v3/api-docs`
- PostgreSQL (local only): `127.0.0.1:5432`

## Useful Commands

- Rebuild only app container:

```bash
docker compose up --build app
```

- Start in detached mode:

```bash
docker compose up -d --build
```

- Stop containers:

```bash
docker compose down
```

- Stop containers and remove DB data volume:

```bash
docker compose down -v
```

