package com.example.bookmanagement.exception

data class ApiError(
    val status: Int,
    val message: String,
)
