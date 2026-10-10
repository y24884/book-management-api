package com.example.bookmanagement.controller

import com.example.bookmanagement.dto.AuthorResponse
import com.example.bookmanagement.dto.BookResponse
import com.example.bookmanagement.exception.AuthorNotFoundException
import com.example.bookmanagement.service.AuthorService
import com.example.bookmanagement.service.BookService
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

@WebMvcTest(AuthorController::class)
class AuthorControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var authorService: AuthorService

    @MockitoBean
    private lateinit var bookService: BookService

    @Test
    fun `creates an author`() {
        whenever(authorService.create(org.mockito.kotlin.any())).thenReturn(
            AuthorResponse(1, "夏目漱石", LocalDate.of(1867, 2, 9)),
        )

        mockMvc.perform(
            post("/authors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"夏目漱石\",\"birthDate\":\"1867-02-09\"}"),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("夏目漱石"))
    }

    @Test
    fun `rejects a blank name`() {
        mockMvc.perform(
            post("/authors")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"   \",\"birthDate\":\"1867-02-09\"}"),
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("name must not be blank"))
    }

    @Test
    fun `gets an author`() {
        whenever(authorService.get(1)).thenReturn(
            AuthorResponse(1, "夏目漱石", LocalDate.of(1867, 2, 9)),
        )

        mockMvc.perform(get("/authors/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.birthDate").value("1867-02-09"))
    }

    @Test
    fun `updates an author`() {
        whenever(authorService.update(org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(
            AuthorResponse(1, "夏目漱石（改名後）", LocalDate.of(1867, 2, 9)),
        )

        mockMvc.perform(
            put("/authors/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"夏目漱石（改名後）\",\"birthDate\":\"1867-02-09\"}"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("夏目漱石（改名後）"))
    }

    @Test
    fun `returns 404 when author does not exist`() {
        whenever(authorService.get(999)).thenThrow(AuthorNotFoundException(999))

        mockMvc.perform(get("/authors/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
    }

    @Test
    fun `returns 404 when updating an author that does not exist`() {
        whenever(authorService.update(org.mockito.kotlin.any(), org.mockito.kotlin.any()))
            .thenThrow(AuthorNotFoundException(999))

        mockMvc.perform(
            put("/authors/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"夏目漱石\",\"birthDate\":\"1867-02-09\"}"),
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
    }

    @Test
    fun `gets books for an author`() {
        whenever(bookService.findByAuthorId(1)).thenReturn(emptyList<BookResponse>())

        mockMvc.perform(get("/authors/1/books"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }
}
