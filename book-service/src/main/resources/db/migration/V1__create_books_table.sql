CREATE TABLE books (
    id               VARCHAR(36)  PRIMARY KEY,
    owner_id         VARCHAR(36)  NOT NULL,
    title            VARCHAR(255) NOT NULL,
    author           VARCHAR(255) NOT NULL,
    book_type        VARCHAR(10)  NOT NULL,
    eshop_url        VARCHAR(500),
    private_file_key VARCHAR(500),
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_books_owner_id ON books (owner_id);
