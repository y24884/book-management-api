package com.example.bookmanagement.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.slf4j.LoggerFactory
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(AuthorNotFoundException::class)
    fun handleAuthorNotFound(exception: AuthorNotFoundException): ResponseEntity<ApiError> =
        errorResponse(HttpStatus.NOT_FOUND, exception.message ?: "Author was not found")

    @ExceptionHandler(FutureBirthDateException::class)
    fun handleFutureBirthDate(exception: FutureBirthDateException): ResponseEntity<ApiError> =
        errorResponse(HttpStatus.BAD_REQUEST, exception.message ?: "Invalid birthDate")

    @ExceptionHandler(BookNotFoundException::class)
    fun handleBookNotFound(exception: BookNotFoundException): ResponseEntity<ApiError> =
        errorResponse(HttpStatus.NOT_FOUND, exception.message ?: "Book was not found")

    @ExceptionHandler(
        AuthorsNotFoundException::class,
        BookAuthorsRequiredException::class,
        InvalidPublicationStatusTransitionException::class,
    )
    fun handleBookValidation(exception: RuntimeException): ResponseEntity<ApiError> =
        errorResponse(HttpStatus.BAD_REQUEST, exception.message ?: "Invalid book request")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(exception: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val message = exception.bindingResult.fieldErrors.firstOrNull()?.defaultMessage ?: "Invalid request"
        return errorResponse(HttpStatus.BAD_REQUEST, message)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadableRequest(): ResponseEntity<ApiError> =
        errorResponse(HttpStatus.BAD_REQUEST, "Invalid request")

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleArgumentTypeMismatch(): ResponseEntity<ApiError> =
        errorResponse(HttpStatus.BAD_REQUEST, "Invalid request")

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(exception: Exception): ResponseEntity<ApiError> {
        logger.error("Unexpected API error", exception)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error"))
    }

    private fun errorResponse(status: HttpStatus, message: String): ResponseEntity<ApiError> {
        logger.warn("API request rejected: status={}, message={}", status.value(), message)
        return ResponseEntity.status(status).body(ApiError(status.value(), message))
    }

    private companion object {
        val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)
    }
}
