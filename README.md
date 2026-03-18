# Bookshelf App — Microservices

A full-stack microservices application for managing personal book shelves and reviews.

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   react-frontend                     │
│          Vite + React 18 + TypeScript               │
│               nginx reverse-proxy                    │
└──────────┬───────────┬──────────────┬───────────────┘
           │ REST      │ REST         │ REST
    ┌──────▼──┐  ┌─────▼──┐   ┌──────▼──┐
    │  user-  │  │ shelf-  │   │ review- │
    │ service │  │ service │   │ service │
    │  :8080  │  │  :8081  │   │  :8082  │
    │  gRPC   │  │  gRPC   │   │  gRPC   │
    │  :9090  │  │  :9091  │   │  :9092  │
    └────┬────┘  └────┬────┘   └────┬────┘
         │            │  gRPC        │ gRPC
         │            └──────────────┤
         │                           │ gRPC (token validation)
         └───────────────────────────┘
    ┌────┴────┐  ┌────────┐   ┌────────┐
    │postgres │  │postgres│   │postgres│
    │  :5433  │  │  :5434 │   │  :5435 │
    └─────────┘  └────────┘   └────────┘
```

### Services

| Service | Port (HTTP) | Port (gRPC) | Responsibility |
|---------|-------------|-------------|----------------|
| user-service | 8080 | 9090 | Auth (JWT), user CRUD |
| shelf-service | 8081 | 9091 | Shelf + book-list management |
| review-service | 8082 | 9092 | Book reviews (verified-reader badge) |
| react-frontend | 3000 / 80 | — | SPA + nginx proxy |

### gRPC contracts

Proto files live in `proto/` (canonical) and are copied into each service's `src/main/proto/`.

| Proto | Used by (server) | Used by (client) |
|-------|-----------------|-----------------|
| `user.proto` | user-service | shelf-service, review-service |
| `shelf.proto` | shelf-service | review-service |
| `review.proto` | review-service | — |

---

## Quick Start (Docker Compose)

```bash
docker compose up --build
```

| URL | Description |
|-----|-------------|
| http://localhost:3000 | React frontend |
| http://localhost:8080 | user-service REST |
| http://localhost:8081 | shelf-service REST |
| http://localhost:8082 | review-service REST |

---

## Local Development

### Quick start

**Windows:**
```bat
dev-start.bat
```
**Linux (requires tmux):**
```bash
chmod +x dev-start.sh && ./dev-start.sh
```
Both scripts start the databases via Docker, wait 10 s, then launch each service in a separate window / tmux pane.

### Prerequisites
- Java 21
- Maven 3.9+
- Node 20+
- PostgreSQL 16 (or Docker)
- `protoc` (installed automatically by Maven plugin)

### 1. Start databases

```bash
docker compose up -d postgres-user postgres-shelf postgres-review
```

### 2. Run each service

```bash
# Terminal 1 — user-service
cd user-service && mvn spring-boot:run

# Terminal 2 — shelf-service
cd shelf-service && mvn spring-boot:run

# Terminal 3 — review-service
cd review-service && mvn spring-boot:run

# Terminal 4 — frontend
cd react-frontend && npm install && npm run dev
```

The Vite dev-server proxies `/api/*` to the correct back-end service automatically.

---

## REST API

### user-service (`/api/auth`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | — | Register new user |
| POST | `/api/auth/login` | — | Obtain JWT |
| GET  | `/api/auth/me` | JWT | Current user info |

### shelf-service (`/api/shelves`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET  | `/api/shelves` | JWT | List user's shelves |
| POST | `/api/shelves` | JWT | Create shelf |
| GET  | `/api/shelves/{id}` | JWT | Get shelf |
| DELETE | `/api/shelves/{id}` | JWT | Delete shelf (403 for default shelves) |
| POST | `/api/shelves/{id}/books` | JWT | Add book to shelf |
| DELETE | `/api/shelves/{id}/books/{bookId}` | JWT | Remove book |

### review-service (`/api/reviews`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET  | `/api/reviews/book/{bookId}` | — | Public reviews for a book |
| GET  | `/api/reviews/user/{userId}` | — | Reviews by a user |
| POST | `/api/reviews` | JWT | Submit review |
| DELETE | `/api/reviews/{id}` | JWT | Delete own review |

Reviews are automatically tagged as **verified reader** when the book is present on any of the reviewer's shelves (checked via gRPC call to shelf-service).

---

## Project Structure

```
bookshelf-app/
├── proto/                  # Canonical .proto definitions
│   ├── user.proto
│   ├── shelf.proto
│   └── review.proto
├── user-service/           # Spring Boot 3 · Java 21 · JWT · gRPC server
├── shelf-service/          # Spring Boot 3 · Java 21 · gRPC server+client
├── review-service/         # Spring Boot 3 · Java 21 · gRPC server+client
├── react-frontend/         # Vite · React 18 · TypeScript
└── docker-compose.yml
```

### Key libraries

| Library | Version | Purpose |
|---------|---------|---------|
| Spring Boot | 3.2.3 | Application framework |
| grpc-spring-boot-starter | 3.1.0.RELEASE | gRPC integration |
| protobuf-java | 3.25.2 | Protocol Buffers |
| jjwt | 0.12.5 | JWT (user-service) |
| Flyway | (managed) | DB migrations |
| H2 | (managed) | In-memory DB for tests |
| TanStack Query | 5 | Data fetching (frontend) |

---

## Testing

### Running tests

```bash
# single service
cd user-service   && mvn test
cd shelf-service  && mvn test
cd review-service && mvn test
```

### Test stack

| Tool | Role |
|------|------|
| JUnit 5 | Test runner |
| Mockito | Mocking for unit tests |
| Spring Boot Test (`@SpringBootTest`) | Integration / controller tests |
| H2 (in-memory) | Replaces PostgreSQL in test scope |
| Flyway | Runs real migrations against H2 on every test run |

### Test layers

Each service has two layers of tests:

**Unit tests** — pure Mockito, no Spring context, no database. Cover service business logic and gRPC service/client implementations.

**Controller / integration tests** — `@SpringBootTest` + `@AutoConfigureMockMvc` + H2. The full Spring context loads (security, filters, real service + repository layers). Only external gRPC calls are mocked:

| Service | Mocked in controller tests |
|---------|---------------------------|
| user-service | nothing — JWT is validated locally |
| shelf-service | `UserGrpcClient` (token validation) |
| review-service | `UserGrpcClient` (token validation), `ShelfGrpcClient` (verified-reader check) |

Tests are `@Transactional` — each test rolls back automatically, no manual cleanup needed.

### Test configuration

Test-specific properties live in each service's `src/test/resources/application.yml`. They override the main config with H2 datasource, `ddl-auto: validate`, `show-sql: true`, and gRPC server disabled (`port: -1`).

### TDD rules

This project follows strict TDD. See [CLAUDE.md](CLAUDE.md) for the full rules.

---

## Database Migrations

Each service has Flyway migrations under `src/main/resources/db/migration/`.

| Service | Migration | Tables created |
|---------|-----------|----------------|
| user-service | V1__create_users_table.sql | `users` |
| shelf-service | V1__create_shelves_table.sql | `shelves`, `shelf_books` |
|               | V2__add_is_default_to_shelves.sql | adds `shelf_type` column |
| review-service | V1__create_reviews_table.sql | `reviews` |

---

## Default Shelves

On first load, shelf-service automatically creates four default shelves for each user:

| Enum value | Display name | Purpose |
|---|---|---|
| `READ` | Read | Books finished |
| `CURRENTLY_READING` | Currently Reading | Books in progress |
| `OWNED` | Owned | Books owned but not yet started |
| `WISH_LIST` | Wish List | Books to buy |

Custom shelves created by the user have type `CUSTOM` and can be freely deleted.
Default shelves are permanent and will drive future workflow features.

---

## Configuration

Key environment variables (with defaults used by Docker Compose):

```
# Common
DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASS
GRPC_PORT

# user-service only
JWT_SECRET   # base64-encoded key (≥ 256 bits)
JWT_EXPIRATION  # ms, default 86400000 (24h)

# shelf-service / review-service
USER_SERVICE_GRPC_HOST, USER_SERVICE_GRPC_PORT

# review-service only
SHELF_SERVICE_GRPC_HOST, SHELF_SERVICE_GRPC_PORT
```

> **Production note:** Replace the default `JWT_SECRET` with a strong random key. All gRPC channels use plaintext by default — add TLS for production deployments.
