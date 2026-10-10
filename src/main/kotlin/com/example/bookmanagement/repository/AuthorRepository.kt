package com.example.bookmanagement.repository

import com.example.bookmanagement.jooq.tables.Authors.Companion.AUTHORS
import com.example.bookmanagement.jooq.tables.BookAuthors.Companion.BOOK_AUTHORS
import com.example.bookmanagement.model.Author
import java.time.LocalDate
import org.jooq.DSLContext
import org.jooq.Record
import org.springframework.stereotype.Repository

@Repository
class AuthorRepository(
    private val dslContext: DSLContext,
) {
    fun create(name: String, birthDate: LocalDate): Author {
        val record = dslContext.insertInto(AUTHORS)
            .set(AUTHORS.NAME, name)
            .set(AUTHORS.BIRTH_DATE, birthDate)
            .returning()
            .fetchOne()
            ?: error("Failed to create author")

        return record.toAuthor()
    }

    fun findById(authorId: Long): Author? =
        dslContext.selectFrom(AUTHORS)
            .where(AUTHORS.ID.eq(authorId))
            .fetchOne()
            ?.toAuthor()

    fun update(authorId: Long, name: String, birthDate: LocalDate): Author? =
        dslContext.update(AUTHORS)
            .set(AUTHORS.NAME, name)
            .set(AUTHORS.BIRTH_DATE, birthDate)
            .where(AUTHORS.ID.eq(authorId))
            .returning()
            .fetchOne()
            ?.toAuthor()

    fun findByIds(authorIds: Set<Long>): List<Author> =
        dslContext.selectFrom(AUTHORS)
            .where(AUTHORS.ID.`in`(authorIds))
            .fetch()
            .map { it.toAuthor() }

    fun findByBookId(bookId: Long): List<Author> =
        dslContext.select(AUTHORS.ID, AUTHORS.NAME, AUTHORS.BIRTH_DATE)
            .from(AUTHORS)
            .join(BOOK_AUTHORS).on(BOOK_AUTHORS.AUTHOR_ID.eq(AUTHORS.ID))
            .where(BOOK_AUTHORS.BOOK_ID.eq(bookId))
            .orderBy(AUTHORS.ID)
            .fetch()
            .map { it.toAuthor() }

    private fun Record.toAuthor(): Author = Author(
        id = get(AUTHORS.ID) ?: error("Author id must not be null"),
        name = get(AUTHORS.NAME) ?: error("Author name must not be null"),
        birthDate = get(AUTHORS.BIRTH_DATE) ?: error("Author birthDate must not be null"),
    )
}
