# Ideas & Future Directions

## Domain-Driven Design refactor

The codebase is currently a straightforward layered architecture (controller → service → repository). As business logic grows more complex (family sharing, book delivery, store integrations) it will need proper domain modelling.

- Introduce rich domain objects — move business rules out of services and onto entities/aggregates (we already started this with `Shelf.addBook()`)
- Define clear bounded contexts — `Library` (books, shelves), `Social` (reviews, sharing), `Delivery` (Kindle sending), `Identity` (users, family)
- Value objects for things like `BookId`, `Rating`, `StoreReference`
- Domain events for cross-context communication (e.g. `BookAddedToShelf` triggering verified-reader recalculation)
- This refactor should happen before the new features above are implemented — easier to add complexity on a solid domain model than retrofit it later

Not planned for implementation yet — captured here to revisit later.

## Book acquisition tracking

- Track which ebook store a book was purchased from (Amazon, Kobo, Google Play, etc.)
- Each book entry could carry a `source` field and a store-specific identifier

## Personal file storage

- Keep a copy of ebook files in blob storage (e.g. Azure Blob Storage)
- Would need a file-service or storage integration layer

## Kindle delivery

- Send books to a Kindle device directly from the app
- Amazon offers a "Send to Kindle" API / email delivery mechanism
- Could extend to sending to family members' Kindles (see family sharing below)

## Family sharing

- The app is private — used by a small group (wife, brother, mother)
- Remove self-registration; admin creates accounts manually
- Ability to send a book from your library to another family member's Kindle
- Each family member has their own shelves and reviews but shares the book pool

## User management

- Replace open registration with invite-only or admin-created accounts
- Admin role for managing users
