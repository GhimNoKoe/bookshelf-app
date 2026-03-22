# Bookshelf App — Microservices

A full-stack microservices application for managing a personal book library, shelves, and reviews. Built for private family use.

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                        react-frontend                         │
│               Vite + React 18 + TypeScript                   │
│                    nginx reverse-proxy                        │
└──────┬───────────┬──────────────┬──────────────┬────────────┘
       │ REST      │ REST         │ REST         │ REST
┌──────▼──┐  ┌─────▼──┐   ┌──────▼──┐   ┌──────▼──┐
│  user-  │  │ shelf-  │   │ review- │   │  book-  │
│ service │  │ service │   │ service │   │ service │
│  :8080  │  │  :8081  │   │  :8082  │   │  :8083  │
│  gRPC   │  │  gRPC   │   │  gRPC   │   │  gRPC   │
│  :9090  │  │  :9091  │   │  :9092  │   │  :9093  │
└────┬────┘  └────┬────┘   └────┬────┘   └─────────┘
     │            │  gRPC        │ gRPC
     │            └──────────────┤
     │                           │ (token validation, shelf check)
     └───────────────────────────┘
┌────┴────┐  ┌────────┐   ┌────────┐   ┌────────┐
│postgres │  │postgres│   │postgres│   │postgres│
│  :5433  │  │  :5434 │   │  :5435 │   │  :5436 │
└─────────┘  └────────┘   └────────┘   └────────┘
```

### Services

| Service | Port (HTTP) | Port (gRPC) | Responsibility |
|---------|-------------|-------------|----------------|
| user-service | 8080 | 9090 | Auth (JWT), user CRUD |
| shelf-service | 8081 | 9091 | Shelf + book-list management |
| review-service | 8082 | 9092 | Book reviews (verified-reader badge) |
| book-service | 8083 | 9093 | Book catalog (shared pool, ownership) |
| react-frontend | 3000 / 80 | — | SPA + nginx proxy |

### gRPC contracts

Proto files live in `proto/` (canonical) and are copied into each service's `src/main/proto/`.

| Proto | Served by | Called by |
|-------|-----------|-----------|
| `user.proto` | user-service | shelf-service, review-service, book-service |
| `shelf.proto` | shelf-service | review-service |
| `review.proto` | review-service | — |
| `book.proto` | book-service | — (future: shelf-service, review-service) |

### Inter-service gRPC calls

- `shelf-service` → `user-service` to validate JWT tokens
- `review-service` → `user-service` to validate JWT tokens
- `review-service` → `shelf-service` to check whether a book is on a user's shelf (verified-reader badge)
- `book-service` → `user-service` to validate JWT tokens

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
| http://localhost:8083 | book-service REST |

---

## Local Development

### Prerequisites
- Java 21
- Maven 3.9+
- Node 20+
- Docker (for databases)
- `protoc` — installed automatically by the Maven protobuf plugin

### 1. Start databases

```bash
docker compose up -d postgres-user postgres-shelf postgres-review postgres-book
```

### 2. Run each service

```bash
# Terminal 1
cd user-service && mvn spring-boot:run

# Terminal 2
cd shelf-service && mvn spring-boot:run

# Terminal 3
cd review-service && mvn spring-boot:run

# Terminal 4
cd book-service && mvn spring-boot:run

# Terminal 5
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
| GET  | `/api/shelves` | JWT | List user's shelves (auto-creates defaults on first call) |
| POST | `/api/shelves` | JWT | Create custom shelf |
| GET  | `/api/shelves/{id}` | JWT | Get shelf |
| DELETE | `/api/shelves/{id}` | JWT | Delete shelf (403 for default shelves) |
| POST | `/api/shelves/{id}/books` | JWT | Add book to shelf |
| DELETE | `/api/shelves/{id}/books/{bookId}` | JWT | Remove book from shelf |

### review-service (`/api/reviews`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET  | `/api/reviews/book/{bookId}` | — | Public reviews for a book |
| GET  | `/api/reviews/user/{userId}` | — | Reviews by a user |
| POST | `/api/reviews` | JWT | Submit review |
| DELETE | `/api/reviews/{id}` | JWT | Delete own review |

Reviews are automatically tagged as **verified reader** when the book is present on any of the reviewer's shelves (checked via gRPC to shelf-service at write time).

### book-service (`/api/books`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET  | `/api/books` | JWT | List all books (shared pool) |
| POST | `/api/books` | JWT | Add a book (caller becomes owner) |
| GET  | `/api/books/{id}` | JWT | Get book |
| PUT  | `/api/books/{id}` | JWT | Update book (owner only) |
| DELETE | `/api/books/{id}` | JWT | Delete book (owner only) |

**Book fields:**

| Field | Type | Notes |
|-------|------|-------|
| `title` | string | required |
| `author` | string | required |
| `bookType` | `PAPER` \| `EBOOK` | required |
| `eshopUrl` | string | optional — only allowed when `bookType = EBOOK`, hard 400 otherwise |
| `privateFileKey` | string | optional — blob storage reference, upload API planned |

---

## Project Structure

```
bookshelf-app/
├── proto/                  # Canonical .proto definitions
│   ├── user.proto
│   ├── shelf.proto
│   ├── review.proto
│   └── book.proto
├── user-service/           # Spring Boot 3 · Java 21 · JWT · gRPC server
├── shelf-service/          # Spring Boot 3 · Java 21 · gRPC server+client
├── review-service/         # Spring Boot 3 · Java 21 · gRPC server+client
├── book-service/           # Spring Boot 3 · Java 21 · gRPC server+client
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
cd user-service   && mvn test
cd shelf-service  && mvn test
cd review-service && mvn test
cd book-service   && mvn test
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
| book-service | `UserGrpcClient` (token validation) |

Tests are `@Transactional` — each test rolls back automatically, no manual cleanup needed.

### TDD rules

This project follows strict TDD. See [CLAUDE.md](CLAUDE.md) for the full rules.

---

## Database Migrations

Each service has Flyway migrations under `src/main/resources/db/migration/`.

| Service | Migration | Tables |
|---------|-----------|--------|
| user-service | V1__create_users_table.sql | `users` |
| shelf-service | V1__create_shelves_table.sql | `shelves`, `shelf_books` |
|  | V2__add_is_default_to_shelves.sql | adds `shelf_type` column |
| review-service | V1__create_reviews_table.sql | `reviews` |
| book-service | V1__create_books_table.sql | `books` |

---

## Default Shelves

On first load, shelf-service automatically creates four default shelves for each user:

| Enum value | Display name |
|---|---|
| `READ` | Read |
| `CURRENTLY_READING` | Currently Reading |
| `OWNED` | Owned |
| `WISH_LIST` | Wish List |

Custom shelves have type `CUSTOM` and can be freely deleted. Default shelves cannot be deleted.

---

## Configuration

Key environment variables (docker-compose defaults):

```
# All services
DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASS
GRPC_PORT
SERVER_PORT

# user-service only
JWT_SECRET       # base64-encoded key (≥ 256 bits)
JWT_EXPIRATION   # ms, default 86400000 (24h)

# shelf-service, review-service, book-service
USER_SERVICE_GRPC_HOST, USER_SERVICE_GRPC_PORT

# review-service only
SHELF_SERVICE_GRPC_HOST, SHELF_SERVICE_GRPC_PORT
```

> **Production note:** Replace the default `JWT_SECRET` with a strong random key. All gRPC channels use plaintext by default — add TLS for production deployments.
