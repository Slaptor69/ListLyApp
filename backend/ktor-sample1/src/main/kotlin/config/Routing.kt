package com.example.config


import com.example.routes.AuthRouting
import com.example.routes.MediaRoutes
import io.ktor.server.application.Application

fun Application.configureRouting(){
    AuthRouting()
    MediaRoutes()
}