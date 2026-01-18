package com.example.auth.dto

import kotlinx.serialization.Serializable

@Serializable
class RegisterRequest (
    val login: String,
    val password:String

    )