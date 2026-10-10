package com.example.bookmanagement.controller

import com.example.bookmanagement.dto.AuthorRequest
import com.example.bookmanagement.dto.AuthorResponse
import com.example.bookmanagement.dto.BookResponse
import com.example.bookmanagement.service.AuthorService
import com.example.bookmanagement.service.BookService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/authors")
class AuthorController(
    private val authorService: AuthorService,
    private val bookService: BookService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: AuthorRequest): AuthorResponse = authorService.create(request)

    @GetMapping("/{authorId}")
    fun get(@PathVariable authorId: Long): AuthorResponse = authorService.get(authorId)

    @PutMapping("/{authorId}")
    fun update(
        @PathVariable authorId: Long,
        @Valid @RequestBody request: AuthorRequest,
    ): AuthorResponse = authorService.update(authorId, request)

    @GetMapping("/{authorId}/books")
    fun getBooks(@PathVariable authorId: Long): List<BookResponse> = bookService.findByAuthorId(authorId)
}
