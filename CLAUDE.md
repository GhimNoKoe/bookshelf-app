# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## TDD Rules — STRICTLY FOLLOW

- NEVER write implementation and tests in the same step
- NEVER modify tests to make them pass
- NEVER write implementation code before tests exist and are confirmed failing
- Tests are the specification — if tests fail, fix the implementation, not the tests
- Always run tests after writing them and confirm they fail before implementing
- If you think a test is wrong, ask me first — never change it silently

## Architecture Overview

This is a microservices bookshelf application with three Java Spring Boot backend services, a React TypeScript frontend, and three separate PostgreSQL databases.

```
React Frontend (Vite/TS) — port 3000
  └─> REST APIs
        ├─> user-service   (REST :8080, gRPC :9090)  ─> postgres-user  (:5433)
        ├─> shelf-service  (REST :8081, gRPC :9091)  ─> postgres-shelf (:5434)
        └─> review-service (REST :8082, gRPC :9092)  ─> postgres-review(:5435)
```

**Inter-service gRPC calls:**
- `shelf-service` → calls `user-service` to validate JWT tokens
- `review-service` → calls both `user-service` (JWT validation) and `shelf-service` (verify book on user's shelf for "verified reader" badge)

**Proto contracts** live in `/proto/` and are compiled into each service during Maven build via `protobuf-maven-plugin`.

## Running the Full Stack

```bash
docker compose up --build
```

For local development with hot-reload, start databases via Docker then run services individually:

```bash
# Start only databases
docker compose up -d postgres-user postgres-shelf postgres-review

# Each in its own terminal
cd user-service   && mvn spring-boot:run
cd shelf-service  && mvn spring-boot:run
cd review-service && mvn spring-boot:run
cd react-frontend && npm install && npm run dev
```

## Java Services (user-service, shelf-service, review-service)

All three are Spring Boot 3.2.3 / Java 21 / Maven projects with identical build commands.

```bash
mvn clean package -DskipTests   # build JAR
mvn clean test                   # run tests
mvn spring-boot:run              # run locally
```

Proto stubs are auto-generated during `mvn compile` — no manual `protoc` invocation needed.

## React Frontend

```bash
npm run dev      # dev server at :3000 with proxy to backend services
npm run build    # production build to dist/
npx tsc          # type-check only
```

The Vite dev server proxies `/api/auth` → `:8080`, `/api/shelves` → `:8081`, `/api/reviews` → `:8082`. In Docker, Nginx handles this routing.

## Key Patterns

**Authentication flow:** user-service issues JWT tokens (24h expiry). All protected endpoints in shelf-service and review-service validate tokens by making a gRPC call to user-service's `ValidateToken` RPC — there is no shared secret between services; validation is always delegated to user-service.

**Verified reader badge:** When creating a review, review-service calls shelf-service via gRPC to check whether the book exists on any of the user's shelves. The `verified_reader` boolean on the `reviews` table records this at write time.

**Database migrations:** Flyway manages schema in each service under `src/main/resources/db/migration/`. Each service has exactly one migration file (`V1__create_*.sql`) at the time of writing.

**IDs:** All primary keys are `VARCHAR(36)` (UUID strings), generated in Java before persistence.

## Environment Variables (docker-compose defaults)

| Variable | Default | Notes |
|---|---|---|
| `JWT_SECRET` | base64 key in compose file | Replace in production |
| `JWT_EXPIRATION` | `86400000` | 24 hours in ms |
| `GRPC_PORT` | 9090 / 9091 / 9092 | per service |
| `DB_*` | per service | host, port, name, user, pass |
| `USER_SERVICE_GRPC_HOST/PORT` | `user-service` / `9090` | used by shelf & review |
| `SHELF_SERVICE_GRPC_HOST/PORT` | `shelf-service` / `9091` | used by review only |

gRPC connections use plaintext (no TLS).
