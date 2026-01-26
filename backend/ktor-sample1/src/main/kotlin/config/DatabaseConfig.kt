package com.example.config

import com.example.UserMedia.model.UserMediaItem
import com.example.media.model.MediaItem
import com.example.user.User
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection
import com.mongodb.client.MongoClient
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.log

object DatabaseConfig {

    private lateinit var client: MongoClient

    val database by lazy {
        client.getDatabase("ListlyDB")
    }

    val users by lazy {
        database.getCollection<User>()
    }

    val userMediaItems by lazy {
        database.getCollection<UserMediaItem>()
    }

    fun init(uri: String) {
        client = KMongo.createClient(uri)
    }

    fun close() {
        client.close()
    }
}

fun Application.configureDatabase() {
    DatabaseConfig.init("mongodb://localhost:27017")

    environment.monitor.subscribe(ApplicationStopping) {
        DatabaseConfig.close()
    }

    log.info("Database connected successfully with KMongo")
}
