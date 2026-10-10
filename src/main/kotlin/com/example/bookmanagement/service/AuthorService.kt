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
class AuthorService(
    private val authorRepository: AuthorRepository,
    private val clock: Clock,
) {
    fun create(request: AuthorRequest): AuthorResponse {
        validateBirthDate(request.birthDate)
        val author = authorRepository.create(request.name.trim(), request.birthDate)
        logger.info("Created author: authorId={}", author.id)
        return author.toResponse()
    }

    fun get(authorId: Long): AuthorResponse =
        authorRepository.findById(authorId)?.toResponse() ?: throw AuthorNotFoundException(authorId)

    fun update(authorId: Long, request: AuthorRequest): AuthorResponse {
        validateBirthDate(request.birthDate)
        val author = authorRepository.update(authorId, request.name.trim(), request.birthDate)
            ?: throw AuthorNotFoundException(authorId)
        logger.info("Updated author: authorId={}", author.id)
        return author.toResponse()
    }

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
