# TODO

## Up next

### File upload for ebooks
`book-service` — the `private_file_key` field exists on the `books` table but there is no upload/download API yet.

- Decide on storage backend (Azure Blob Storage / S3 / local volume)
- Add `POST /api/books/{id}/file` — upload ebook file (owner only)
- Add `GET  /api/books/{id}/file` — download ebook file (owner only)
- Store the blob key in `private_file_key`
- Add a file-service or integrate storage directly into book-service

### Kindle delivery
- Add `kindle_email` to the user profile (user-service migration + endpoint)
- Implement send-to-Kindle from a book entry (book-service or new delivery-service)
- Only the book owner (or a family admin) can trigger a send

### Family / user management
- Remove open self-registration — admin creates accounts manually
- Add admin role to user-service
- Invite flow or simple admin UI for adding family members

---

## Done

- **gRPC test gaps** — `ShelfGrpcServiceTest`, `ReviewGrpcServiceTest`, `UserGrpcClientTest` (review-service) all implemented and passing
- **book-service** — full catalog service: Book entity (PAPER/EBOOK, eshopUrl, privateFileKey), shared pool, ownership enforcement, REST CRUD, gRPC GetBook/ListBooks, 28 tests passing
