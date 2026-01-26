package com.example.auth.exceptions


open class AuthException(message: String) : RuntimeException(message)

class InvalidCredentialsException :
    AuthException("Invalid login or password")

class UserAlreadyExistsException :
    AuthException("User already exists")
