# TODO

## Next session — gRPC test gaps

Three gRPC test files are missing. Follow the same pattern as `UserGrpcServiceTest`.

### 1. ShelfGrpcServiceTest
`shelf-service/src/test/java/com/bookshelf/shelf/grpc/ShelfGrpcServiceTest.java`

RPCs to cover:
- `getShelf` — success (shelf found), error (shelf not found → onError)
- `getShelvesByUser` — success, error (service throws)
- `addBookToShelf` — success, error

### 2. ReviewGrpcServiceTest
`review-service/src/test/java/com/bookshelf/review/grpc/ReviewGrpcServiceTest.java`

RPCs to cover:
- `getReviewsByBook` — success
- `getReviewsByUser` — success
- `createReview` — success

### 3. UserGrpcClientTest (review-service)
`review-service/src/test/java/com/bookshelf/review/grpc/UserGrpcClientTest.java`

Methods to cover:
- `validateToken` — valid response, gRPC error returns invalid response
- `getUser` — success

Use `ReflectionTestUtils.setField` to inject the mock stub, same approach as `ShelfGrpcClientTest`.
