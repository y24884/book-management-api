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
/**
 * 著者の登録・取得・更新と、著者に紐づく書籍一覧を公開するRESTコントローラ。
 *
 * リクエスト形式の検証はSpring Validationに委譲し、業務ルールはService層で検証する。
 */
class AuthorController(
    private val authorService: AuthorService,
    private val bookService: BookService,
) {
    /** 著者を登録する。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: AuthorRequest): AuthorResponse = authorService.create(request)

    /** 指定IDの著者を取得する。 */
    @GetMapping("/{authorId}")
    fun get(@PathVariable authorId: Long): AuthorResponse = authorService.get(authorId)

    /** 指定IDの著者情報を全体更新する。 */
    @PutMapping("/{authorId}")
    fun update(
        @PathVariable authorId: Long,
        @Valid @RequestBody request: AuthorRequest,
    ): AuthorResponse = authorService.update(authorId, request)

    /** 指定した著者に関連付けられた書籍を取得する。 */
    @GetMapping("/{authorId}/books")
    fun getBooks(@PathVariable authorId: Long): List<BookResponse> = bookService.findByAuthorId(authorId)
}
