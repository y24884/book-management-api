package com.example.bookmanagement.model

import java.math.BigDecimal

data class Book(
    val id: Long,
    val title: String,
    val price: BigDecimal,
    val publicationStatus: PublicationStatus,
)
