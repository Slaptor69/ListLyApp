package ru.misterpotz.listly.features.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.misterpotz.listly.appComponent

private enum class AuthMode {
    Login,
    Register,
}

@Composable
fun AuthScreenEntry(
    onAuthorized: () -> Unit,
    onBack: () -> Unit,
) {
    val authRepository = remember { appComponent.authRepository }
    val coroutineScope = rememberCoroutineScope()

    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var authMode by remember { mutableStateOf(AuthMode.Login) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Text(
                        text = "←",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = if (authMode == AuthMode.Login) "Вход" else "Регистрация",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = login,
                onValueChange = { login = it },
                label = { Text("Имя") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )



            if (!errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                enabled = !isLoading,
                onClick = {
                    val preparedInput = prepareAuthInput(login, password)
                    val authInput = preparedInput.getOrElse {
                        errorMessage = it.message ?: "Введите имя и пароль"
                        return@Button
                    }

                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null

                        val result = when (authMode) {
                            AuthMode.Login -> authRepository.login(authInput.login, authInput.password)
                            AuthMode.Register -> authRepository.registerAndLogin(authInput.login, authInput.password)
                        }

                        isLoading = false
                        result.onSuccess {
                            onAuthorized()
                        }.onFailure {
                            errorMessage = it.message ?: "Не удалось выполнить авторизацию"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Text(if (authMode == AuthMode.Login) "Войти" else "Зарегистрироваться")
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                enabled = !isLoading,
                onClick = {
                    errorMessage = null
                    authMode = if (authMode == AuthMode.Login) {
                        AuthMode.Register
                    } else {
                        AuthMode.Login
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (authMode == AuthMode.Login) {
                        "Нет аккаунта? Регистрация"
                    } else {
                        "Уже есть аккаунт? Вход"
                    }
                )
            }
        }
    }
}
