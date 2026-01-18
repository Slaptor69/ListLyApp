package com.example.auth.dto

import kotlinx.serialization.Serializable


@Serializable
class AuthResponse (
    val token: String
    )