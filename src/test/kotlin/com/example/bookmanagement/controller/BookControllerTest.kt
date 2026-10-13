package com.example.bookmanagement.controller

import com.example.bookmanagement.dto.AuthorResponse
import com.example.bookmanagement.dto.BookResponse
import com.example.bookmanagement.exception.BookNotFoundException
import com.example.bookmanagement.model.PublicationStatus
import com.example.bookmanagement.service.BookService
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(BookController::class)
class BookControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var bookService: BookService

    @Test
    fun `creates a book with multiple authors`() {
        whenever(bookService.create(org.mockito.kotlin.any())).thenReturn(bookResponse())

        mockMvc.perform(
            post("/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"吾輩は猫である\",\"price\":1000,\"publicationStatus\":\"UNPUBLISHED\",\"authorIds\":[1,2]}"),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.authors.length()").value(2))
    }

    @Test
    fun `rejects a negative price`() {
        mockMvc.perform(
            post("/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"吾輩は猫である\",\"price\":-1,\"publicationStatus\":\"UNPUBLISHED\",\"authorIds\":[1]}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("price must be greater than or equal to 0"))
    }

    @Test
    fun `rejects an empty author list`() {
        mockMvc.perform(
            post("/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"吾輩は猫である\",\"price\":0,\"publicationStatus\":\"UNPUBLISHED\",\"authorIds\":[]}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("authorIds must contain at least one author"))
    }

    @Test
    fun `gets a book`() {
        whenever(bookService.get(1)).thenReturn(bookResponse())

        mockMvc.perform(get("/books/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.publicationStatus").value("UNPUBLISHED"))
    }

    @Test
    fun `returns 400 when book ID is not numeric`() {
        mockMvc.perform(get("/books/abc"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Invalid request"))
    }

    @Test
    fun `updates a book`() {
        whenever(bookService.update(org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(
            bookResponse(publicationStatus = PublicationStatus.PUBLISHED),
        )

        mockMvc.perform(
            put("/books/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"吾輩は猫である\",\"price\":1200,\"publicationStatus\":\"PUBLISHED\",\"authorIds\":[1,2]}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.publicationStatus").value("PUBLISHED"))
    }

    @Test
    fun `returns 404 when book does not exist`() {
        whenever(bookService.get(999)).thenThrow(BookNotFoundException(999))

        mockMvc.perform(get("/books/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
    }

    private fun bookResponse(
        publicationStatus: PublicationStatus = PublicationStatus.UNPUBLISHED,
    ): BookResponse = BookResponse(
        id = 1,
        title = "吾輩は猫である",
        price = BigDecimal("1000"),
        publicationStatus = publicationStatus,
        authors = listOf(
            AuthorResponse(1, "夏目漱石", LocalDate.of(1867, 2, 9)),
            AuthorResponse(2, "共同著者", LocalDate.of(1870, 1, 1)),
        ),
    )
}
