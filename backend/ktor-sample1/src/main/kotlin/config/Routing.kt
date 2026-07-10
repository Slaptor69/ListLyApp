package com.example.config

import com.example.routes.AuthRoutes
import com.example.routes.MediaRoutes
import io.ktor.server.application.Application

fun Application.configureRouting(){
    AuthRoutes()
    MediaRoutes()
}