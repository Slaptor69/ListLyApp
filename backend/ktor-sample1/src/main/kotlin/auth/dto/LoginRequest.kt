package com.example.auth.dto

import kotlinx.serialization.Serializable


@Serializable
class LoginRequest(val login: String,
    val password:String )