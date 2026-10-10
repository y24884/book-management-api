package com.example.bookmanagement.exception

class BookNotFoundException(bookId: Long) : RuntimeException("Book $bookId was not found")
