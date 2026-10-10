package com.example.bookmanagement.dto

import com.example.bookmanagement.model.PublicationStatus
import java.math.BigDecimal

data class BookResponse(
    val id: Long,
    val title: String,
    val price: BigDecimal,
    val publicationStatus: PublicationStatus,
    val authors: List<AuthorResponse>,
)
