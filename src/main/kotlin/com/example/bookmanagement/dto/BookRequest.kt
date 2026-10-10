package com.example.bookmanagement.dto

import com.example.bookmanagement.model.PublicationStatus
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.math.BigDecimal

data class BookRequest(
    @field:NotBlank(message = "title must not be blank")
    val title: String,
    @field:DecimalMin(value = "0", message = "price must be greater than or equal to 0")
    val price: BigDecimal,
    val publicationStatus: PublicationStatus,
    @field:NotEmpty(message = "authorIds must contain at least one author")
    val authorIds: Set<Long>,
)
