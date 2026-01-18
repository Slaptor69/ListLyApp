package com.example.routes

import com.example.auth.dto.AuthResponse
import com.example.auth.dto.LoginRequest
import com.example.auth.dto.RegisterRequest
import com.example.security.JwtService
import com.example.security.PasswordHasher
import com.example.user.User
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import com.example.user.*
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal


fun Application.AuthRouting() {

    val userRepository = UserRepository()
    routing {
        route("/auth") {
            post("/login") {
                val request= call.receive<LoginRequest>()
                val user = userRepository.findByLogin(request.login)?: return@post call.respond(HttpStatusCode.Unauthorized,"Invalid Credentials")

                if (user == null || !PasswordHasher.verify(request.password, user.passwordHash)) {
                    return@post call.respond(HttpStatusCode.Unauthorized, "Invalid login or password")
                }

                val token = JwtService.generateToken(user)

                call.respond(AuthResponse(token))


            }
            post("/register") {
                val request = call.receive<RegisterRequest>()

                if (userRepository.findByLogin(request.login) != null){
                    return@post call.respond(HttpStatusCode.Conflict, "User Already Exists")
                }

                val password = PasswordHasher.hash(request.password)

                val user = User(
                    login = request.login,
                    passwordHash = password
                )

                userRepository.save(user)

                call.respond(HttpStatusCode.Created)

            }
        }
    }



}


