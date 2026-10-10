package com.example.bookmanagement.service

import com.example.bookmanagement.dto.AuthorRequest
import com.example.bookmanagement.exception.AuthorNotFoundException
import com.example.bookmanagement.exception.FutureBirthDateException
import com.example.bookmanagement.model.Author
import com.example.bookmanagement.repository.AuthorRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.whenever

class AuthorServiceTest {
    private val repository = mock(AuthorRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-10-10T00:00:00Z"), ZoneOffset.UTC)
    private val service = AuthorService(repository, clock)

    @Test
    fun `creates an author when birth date is today`() {
        val request = AuthorRequest(name = "夏目漱石", birthDate = LocalDate.of(2026, 10, 10))
        whenever(repository.create("夏目漱石", request.birthDate)).thenReturn(
            Author(id = 1, name = "夏目漱石", birthDate = request.birthDate),
        )

        val response = service.create(request)

        assertEquals(1, response.id)
        assertEquals(LocalDate.of(2026, 10, 10), response.birthDate)
    }

    @Test
    fun `rejects a future birth date based on the injected clock`() {
        val request = AuthorRequest(name = "夏目漱石", birthDate = LocalDate.of(2026, 10, 11))

        assertThrows(FutureBirthDateException::class.java) {
            service.create(request)
        }

        verifyNoInteractions(repository)
    }

    @Test
    fun `returns not found when an author does not exist`() {
        whenever(repository.findById(999)).thenReturn(null)

        assertThrows(AuthorNotFoundException::class.java) {
            service.get(999)
        }
    }

    @Test
    fun `gets an existing author`() {
        val author = Author(1, "夏目漱石", LocalDate.of(1867, 2, 9))
        whenever(repository.findById(author.id)).thenReturn(author)

        val response = service.get(author.id)

        assertEquals(author.name, response.name)
    }

    @Test
    fun `returns not found when updating a missing author`() {
        val request = AuthorRequest("夏目漱石", LocalDate.of(1867, 2, 9))
        whenever(repository.update(999, request.name, request.birthDate)).thenReturn(null)

        assertThrows(AuthorNotFoundException::class.java) {
            service.update(999, request)
        }
    }
}
