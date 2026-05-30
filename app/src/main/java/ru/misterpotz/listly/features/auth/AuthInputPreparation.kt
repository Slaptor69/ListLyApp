package ru.misterpotz.listly.features.auth

internal data class AuthInput(
    val login: String,
    val password: String,
)

internal fun prepareAuthInput(login: String, password: String): Result<AuthInput> {
    val trimmedLogin = login.trim()
    val trimmedPassword = password.trim()
    if (trimmedLogin.isBlank() || trimmedPassword.isBlank()) {
        return Result.failure(IllegalArgumentException("Введите имя и пароль"))
    }
    if (trimmedLogin.length !in LOGIN_LENGTH_RANGE) {
        return Result.failure(IllegalArgumentException("Имя должно быть от 3 до 20 символов"))
    }
    if (trimmedPassword.length !in PASSWORD_LENGTH_RANGE) {
        return Result.failure(IllegalArgumentException("Пароль должен быть от 6 до 25 символов"))
    }
    return Result.success(
        AuthInput(
            login = trimmedLogin,
            password = trimmedPassword,
        )
    )
}

private val LOGIN_LENGTH_RANGE = 3..20
private val PASSWORD_LENGTH_RANGE = 6..25
