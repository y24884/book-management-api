package com.example.bookmanagement.exception

class AuthorNotFoundException(authorId: Long) : RuntimeException("Author $authorId was not found")
