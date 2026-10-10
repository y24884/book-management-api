package com.example.bookmanagement.service

import com.example.bookmanagement.dto.AuthorRequest
import com.example.bookmanagement.dto.AuthorResponse
import com.example.bookmanagement.exception.AuthorNotFoundException
import com.example.bookmanagement.exception.FutureBirthDateException
import com.example.bookmanagement.model.Author
import com.example.bookmanagement.repository.AuthorRepository
import java.time.Clock
import java.time.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
/**
 * 著者の業務操作を扱うService。
 *
 * 生年月日が現在日より未来でないことを、注入された[Clock]を基準に検証する。
 */
class AuthorService(
    private val authorRepository: AuthorRepository,
    private val clock: Clock,
) {
    /** 入力を検証し、新しい著者を登録する。 */
    fun create(request: AuthorRequest): AuthorResponse {
        validateBirthDate(request.birthDate)
        val author = authorRepository.create(request.name.trim(), request.birthDate)
        logger.info("Created author: authorId={}", author.id)
        return author.toResponse()
    }

    /** 指定IDの著者を取得する。存在しない場合は[AuthorNotFoundException]を送出する。 */
    fun get(authorId: Long): AuthorResponse {
        val author = authorRepository.findById(authorId) ?: throw AuthorNotFoundException(authorId)
        return author.toResponse()
    }

    /** 入力を検証し、指定IDの著者を更新する。 */
    fun update(authorId: Long, request: AuthorRequest): AuthorResponse {
        validateBirthDate(request.birthDate)
        val author = authorRepository.update(authorId, request.name.trim(), request.birthDate)
            ?: throw AuthorNotFoundException(authorId)
        logger.info("Updated author: authorId={}", author.id)
        return author.toResponse()
    }

    /** 生年月日が現在日より未来の場合に[FutureBirthDateException]を送出する。 */
    private fun validateBirthDate(birthDate: LocalDate) {
        if (birthDate.isAfter(LocalDate.now(clock))) {
            throw FutureBirthDateException()
        }
    }

    private fun Author.toResponse(): AuthorResponse = AuthorResponse(
        id = id,
        name = name,
        birthDate = birthDate,
    )

    private companion object {
        val logger = LoggerFactory.getLogger(AuthorService::class.java)
    }
}
