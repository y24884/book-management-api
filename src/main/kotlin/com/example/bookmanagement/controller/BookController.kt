package com.example.bookmanagement.controller

import com.example.bookmanagement.dto.BookRequest
import com.example.bookmanagement.dto.BookResponse
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
@RequestMapping("/books")
class BookController(
    private val bookService: BookService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: BookRequest): BookResponse = bookService.create(request)

    @GetMapping("/{bookId}")
    fun get(@PathVariable bookId: Long): BookResponse = bookService.get(bookId)

    @PutMapping("/{bookId}")
    fun update(
        @PathVariable bookId: Long,
        @Valid @RequestBody request: BookRequest,
    ): BookResponse = bookService.update(bookId, request)
}
