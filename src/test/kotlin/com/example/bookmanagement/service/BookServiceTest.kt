package com.example.bookmanagement.service

import com.example.bookmanagement.dto.BookRequest
import com.example.bookmanagement.exception.AuthorsNotFoundException
import com.example.bookmanagement.exception.BookAuthorsRequiredException
import com.example.bookmanagement.exception.InvalidPublicationStatusTransitionException
import com.example.bookmanagement.model.Author
import com.example.bookmanagement.model.Book
import com.example.bookmanagement.model.PublicationStatus
import com.example.bookmanagement.repository.AuthorRepository
import com.example.bookmanagement.repository.BookRepository
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class BookServiceTest {
    private val bookRepository = mock(BookRepository::class.java)
    private val authorRepository = mock(AuthorRepository::class.java)
    private val service = BookService(bookRepository, authorRepository)

    @Test
    fun `creates a book and replaces its author associations`() {
        val request = request(authorIds = setOf(1, 2))
        whenever(authorRepository.findByIds(request.authorIds)).thenReturn(authors())
        whenever(bookRepository.create(request.title, request.price, request.publicationStatus)).thenReturn(
            Book(1, request.title, request.price, request.publicationStatus),
        )

        val response = service.create(request)

        assertEquals(listOf(1L, 2L), response.authors.map { it.id })
        verify(bookRepository).replaceAuthors(1, setOf(1, 2))
    }

    @Test
    fun `rejects a missing author before writing a book`() {
        val request = request(authorIds = setOf(1, 999))
        whenever(authorRepository.findByIds(request.authorIds)).thenReturn(listOf(authors().first()))

        assertThrows(AuthorsNotFoundException::class.java) {
            service.create(request)
        }

        verifyNoInteractions(bookRepository)
    }

    @Test
    fun `rejects an empty author list before writing a book`() {
        assertThrows(BookAuthorsRequiredException::class.java) {
            service.create(request(authorIds = emptySet()))
        }

        verifyNoInteractions(bookRepository, authorRepository)
    }

    @Test
    fun `replaces author associations when updating a book`() {
        val request = request(authorIds = setOf(2), publicationStatus = PublicationStatus.PUBLISHED)
        whenever(bookRepository.findByIdForUpdate(1)).thenReturn(
            Book(1, request.title, request.price, PublicationStatus.UNPUBLISHED),
        )
        whenever(authorRepository.findByIds(request.authorIds)).thenReturn(listOf(authors()[1]))
        whenever(bookRepository.update(1, request.title, request.price, PublicationStatus.PUBLISHED)).thenReturn(
            Book(1, request.title, request.price, PublicationStatus.PUBLISHED),
        )

        val response = service.update(1, request)

        assertEquals(listOf(2L), response.authors.map { it.id })
        verify(bookRepository).replaceAuthors(1, setOf(2))
    }

    @Test
    fun `rejects published to unpublished before changing associations`() {
        val request = request(publicationStatus = PublicationStatus.UNPUBLISHED)
        whenever(bookRepository.findByIdForUpdate(1)).thenReturn(
            Book(1, "吾輩は猫である", BigDecimal("1000"), PublicationStatus.PUBLISHED),
        )

        assertThrows(InvalidPublicationStatusTransitionException::class.java) {
            service.update(1, request)
        }

        verifyNoInteractions(authorRepository)
    }

    private fun request(
        authorIds: Set<Long> = setOf(1),
        publicationStatus: PublicationStatus = PublicationStatus.UNPUBLISHED,
    ): BookRequest = BookRequest(
        title = "吾輩は猫である",
        price = BigDecimal("1000"),
        publicationStatus = publicationStatus,
        authorIds = authorIds,
    )

    private fun authors(): List<Author> = listOf(
        Author(1, "夏目漱石", LocalDate.of(1867, 2, 9)),
        Author(2, "共同著者", LocalDate.of(1870, 1, 1)),
    )
}
