package com.example.bookmanagement.repository

import com.example.bookmanagement.jooq.tables.BookAuthors.Companion.BOOK_AUTHORS
import com.example.bookmanagement.jooq.tables.Books.Companion.BOOKS
import com.example.bookmanagement.model.Book
import com.example.bookmanagement.model.PublicationStatus
import java.math.BigDecimal
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository

@Repository
class BookRepository(
    private val dslContext: DSLContext,
) {
    fun create(title: String, price: BigDecimal, publicationStatus: PublicationStatus): Book {
        val record = dslContext.insertInto(BOOKS)
            .set(BOOKS.TITLE, title)
            .set(BOOKS.PRICE, price)
            .set(BOOKS.PUBLICATION_STATUS, publicationStatus.name)
            .returning()
            .fetchOne()
            ?: error("Failed to create book")

        return record.toBook()
    }

    fun findById(bookId: Long): Book? =
        dslContext.selectFrom(BOOKS)
            .where(BOOKS.ID.eq(bookId))
            .fetchOne()
            ?.toBook()

    fun findByIdForUpdate(bookId: Long): Book? =
        dslContext.selectFrom(BOOKS)
            .where(BOOKS.ID.eq(bookId))
            .forUpdate()
            .fetchOne()
            ?.toBook()

    fun findByAuthorId(authorId: Long): List<Book> =
        dslContext.select(BOOKS.ID, BOOKS.TITLE, BOOKS.PRICE, BOOKS.PUBLICATION_STATUS)
            .from(BOOKS)
            .join(BOOK_AUTHORS).on(BOOK_AUTHORS.BOOK_ID.eq(BOOKS.ID))
            .where(BOOK_AUTHORS.AUTHOR_ID.eq(authorId))
            .orderBy(BOOKS.ID)
            .fetch()
            .map { it.toBook() }

    fun update(bookId: Long, title: String, price: BigDecimal, publicationStatus: PublicationStatus): Book? =
        dslContext.update(BOOKS)
            .set(BOOKS.TITLE, title)
            .set(BOOKS.PRICE, price)
            .set(BOOKS.PUBLICATION_STATUS, publicationStatus.name)
            .where(BOOKS.ID.eq(bookId))
            .returning()
            .fetchOne()
            ?.toBook()

    fun replaceAuthors(bookId: Long, authorIds: Set<Long>) {
        dslContext.deleteFrom(BOOK_AUTHORS)
            .where(BOOK_AUTHORS.BOOK_ID.eq(bookId))
            .execute()

        dslContext.batch(
            authorIds.map { authorId ->
                dslContext.insertInto(BOOK_AUTHORS)
                    .set(BOOK_AUTHORS.BOOK_ID, bookId)
                    .set(BOOK_AUTHORS.AUTHOR_ID, authorId)
            },
        ).execute()
    }

    private fun Record.toBook(): Book = Book(
        id = get(BOOKS.ID) ?: error("Book id must not be null"),
        title = get(BOOKS.TITLE) ?: error("Book title must not be null"),
        price = get(BOOKS.PRICE) ?: error("Book price must not be null"),
        publicationStatus = PublicationStatus.valueOf(
            get(BOOKS.PUBLICATION_STATUS) ?: error("Book publicationStatus must not be null"),
        ),
    )
}
