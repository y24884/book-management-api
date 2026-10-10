package com.example.bookmanagement.exception

class AuthorsNotFoundException(authorIds: Set<Long>) : RuntimeException("Authors not found: ${authorIds.sorted()}")
