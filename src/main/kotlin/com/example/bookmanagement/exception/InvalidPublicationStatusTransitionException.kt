package com.example.bookmanagement.exception

class InvalidPublicationStatusTransitionException :
    RuntimeException("PUBLISHED books cannot be changed to UNPUBLISHED")
