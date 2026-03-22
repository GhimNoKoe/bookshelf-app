# Ideas & Future Directions

## Personal file storage

Keep a copy of ebook files associated with a book entry.

- `private_file_key` field already exists on the `books` table (added in book-service v1)
- Next step: file-service or direct blob storage integration (Azure Blob Storage / S3)
- Upload/download endpoints to be added to book-service once storage backend is chosen
- Only the book owner should be able to upload/download their file

## Kindle delivery

Send a book directly to a Kindle device from the app.

- Amazon offers a "Send to Kindle" API and email-based delivery
- Requires a `kindle_email` field on the user profile (user-service change)
- Natural extension of book ownership: only the book owner can send it
- Could also allow sending to a family member's Kindle (see family sharing below)
- Planned as a dedicated feature after file storage is in place

## Family sharing

The app is used privately by a small group (wife, brother, mother).

- Books are already a shared pool with per-owner tracking (`owner_id` on each book)
- Future: explicit family-group concept so the book pool is scoped to the group rather than globally visible
- Replace open registration with invite-only or admin-created accounts (remove self-registration)
- Admin role for managing users
- Each family member keeps their own shelves and reviews
- Sending a book to another family member's Kindle ties into the Kindle delivery feature above

## Domain-Driven Design refactor

The codebase is currently a straightforward layered architecture (controller → service → repository).
As business logic grows — particularly around family sharing, delivery workflows, and store integrations — a richer domain model will be worth introducing.

- Bounded contexts are already implicit in the microservices split (`Identity`, `Library`, `Social`, `Delivery`)
- `Shelf.addBook()` and `ShelfType.isDefault()` are early examples of domain behaviour on entities
- Candidate next steps: move ownership/authorization checks onto entities, introduce value objects for `BookType`, `Rating`, `StoreReference`
- Domain events could decouple cross-context side effects (e.g. `BookAddedToShelf` triggering verified-reader recalculation)
- Revisit when two or more of the above features are in active development simultaneously
