package com.example

import com.example.config.configureDatabase
import com.example.config.configureHTTP
import com.example.config.configureRouting
import com.example.config.configureSecurity
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureHTTP()
    configureSecurity()
    configureSerialization()
    configureDatabase()
    configureRouting()
}
