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
    return Result.success(
        AuthInput(
            login = trimmedLogin,
            password = trimmedPassword,
        )
    )
}
