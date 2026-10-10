CREATE TABLE authors (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    birth_date DATE NOT NULL
);

CREATE TABLE books (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    price NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    publication_status VARCHAR(20) NOT NULL CHECK (publication_status IN ('UNPUBLISHED', 'PUBLISHED'))
);

CREATE TABLE book_authors (
    book_id BIGINT NOT NULL REFERENCES books (id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES authors (id) ON DELETE RESTRICT,
    PRIMARY KEY (book_id, author_id)
);

CREATE INDEX idx_book_authors_author_id ON book_authors (author_id);
