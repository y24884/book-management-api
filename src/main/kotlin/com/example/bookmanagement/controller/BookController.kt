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
/**
 * 書籍の登録・取得・更新を公開するRESTコントローラ。
 *
 * 書籍と著者の関連付けや出版状態の遷移はService層に委譲する。
 */
class BookController(
    private val bookService: BookService,
) {
    /** 著者との関連付けを含む書籍を登録する。 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: BookRequest): BookResponse = bookService.create(request)

    /** 指定IDの書籍と関連著者を取得する。 */
    @GetMapping("/{bookId}")
    fun get(@PathVariable bookId: Long): BookResponse = bookService.get(bookId)

    /** 書籍情報と著者の関連付けを全体更新する。 */
    @PutMapping("/{bookId}")
    fun update(
        @PathVariable bookId: Long,
        @Valid @RequestBody request: BookRequest,
    ): BookResponse = bookService.update(bookId, request)
}
