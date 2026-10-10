package com.example.bookmanagement.service

import com.example.bookmanagement.dto.AuthorResponse
import com.example.bookmanagement.dto.BookRequest
import com.example.bookmanagement.dto.BookResponse
import com.example.bookmanagement.exception.AuthorNotFoundException
import com.example.bookmanagement.exception.AuthorsNotFoundException
import com.example.bookmanagement.exception.BookAuthorsRequiredException
import com.example.bookmanagement.exception.BookNotFoundException
import com.example.bookmanagement.exception.InvalidPublicationStatusTransitionException
import com.example.bookmanagement.model.Author
import com.example.bookmanagement.model.Book
import com.example.bookmanagement.model.PublicationStatus
import com.example.bookmanagement.repository.AuthorRepository
import com.example.bookmanagement.repository.BookRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookService(
    private val bookRepository: BookRepository,
    private val authorRepository: AuthorRepository,
) {
    @Transactional
    fun create(request: BookRequest): BookResponse {
        val authors = findRequiredAuthors(request.authorIds)
        val book = bookRepository.create(request.title.trim(), request.price, request.publicationStatus)
        bookRepository.replaceAuthors(book.id, request.authorIds)
        logger.info(
            "Created book: bookId={}, authorCount={}, publicationStatus={}",
            book.id,
            authors.size,
            book.publicationStatus,
        )
        return book.toResponse(authors)
    }

    fun get(bookId: Long): BookResponse {
        val book = bookRepository.findById(bookId) ?: throw BookNotFoundException(bookId)
        return book.toResponse(authorRepository.findByBookId(bookId))
    }

    @Transactional
    fun update(bookId: Long, request: BookRequest): BookResponse {
        val currentBook = bookRepository.findByIdForUpdate(bookId) ?: throw BookNotFoundException(bookId)
        validatePublicationStatusTransition(currentBook.publicationStatus, request.publicationStatus)
        val authors = findRequiredAuthors(request.authorIds)
        val updatedBook = bookRepository.update(bookId, request.title.trim(), request.price, request.publicationStatus)
            ?: throw BookNotFoundException(bookId)
        bookRepository.replaceAuthors(bookId, request.authorIds)
        logger.info(
            "Updated book: bookId={}, authorCount={}, publicationStatus={}->{}",
            bookId,
            authors.size,
            currentBook.publicationStatus,
            updatedBook.publicationStatus,
        )
        return updatedBook.toResponse(authors)
    }

    fun findByAuthorId(authorId: Long): List<BookResponse> {
        if (authorRepository.findById(authorId) == null) {
            throw AuthorNotFoundException(authorId)
        }
        return bookRepository.findByAuthorId(authorId).map { book ->
            book.toResponse(authorRepository.findByBookId(book.id))
        }
    }

    private fun findRequiredAuthors(authorIds: Set<Long>): List<Author> {
        if (authorIds.isEmpty()) {
            throw BookAuthorsRequiredException()
        }
        val authors = authorRepository.findByIds(authorIds)
        if (authors.map(Author::id).toSet() != authorIds) {
            throw AuthorsNotFoundException(authorIds - authors.map(Author::id).toSet())
        }
        return authors.sortedBy(Author::id)
    }

    private fun validatePublicationStatusTransition(
        currentStatus: PublicationStatus,
        requestedStatus: PublicationStatus,
    ) {
        if (currentStatus == PublicationStatus.PUBLISHED && requestedStatus == PublicationStatus.UNPUBLISHED) {
            throw InvalidPublicationStatusTransitionException()
        }
    }

    private fun Book.toResponse(authors: List<Author>): BookResponse = BookResponse(
        id = id,
        title = title,
        price = price,
        publicationStatus = publicationStatus,
        authors = authors.map { author ->
            AuthorResponse(author.id, author.name, author.birthDate)
        },
    )

    private companion object {
        val logger = LoggerFactory.getLogger(BookService::class.java)
    }
}
