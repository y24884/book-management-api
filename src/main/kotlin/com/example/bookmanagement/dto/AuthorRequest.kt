package com.example.bookmanagement.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class AuthorRequest(
    @field:NotBlank(message = "name must not be blank")
    val name: String,
    @field:NotNull(message = "birthDate is required")
    val birthDate: LocalDate,
)
