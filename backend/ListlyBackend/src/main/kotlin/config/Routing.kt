package com.example.config


import com.example.routes.AuthRouting
import com.example.routes.GlobalMediaRoutes
import com.example.routes.GlobalMediaRouting
import com.example.routes.UserMediaRouting
import io.ktor.server.application.Application

fun Application.configureRouting(){
    AuthRouting()
    UserMediaRouting()
    GlobalMediaRoutes()
}