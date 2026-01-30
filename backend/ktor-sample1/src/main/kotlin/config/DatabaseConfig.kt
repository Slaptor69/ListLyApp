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
    private val mongoUri = System.getenv("MONGO_URI") ?: "mongodb://localhost:27017/listlydb"

    fun init() {
        var connected = false
        while (!connected) {
            try {
                client = KMongo.createClient(mongoUri)
                client.listDatabaseNames()
                connected = true
            } catch (e: Exception) {
                println("Mongo not ready, retrying in 2s...")
                Thread.sleep(2000)
            }
        }
    }

    val database by lazy {
        client.getDatabase("ListlyDB")
    }

    val users by lazy {
        database.getCollection<User>()
    }

    val userMediaItems by lazy {
        database.getCollection<UserMediaItem>()
    }

    fun close() {
        client.close()
    }
}


fun Application.configureDatabase() {
    DatabaseConfig.init()

    environment.monitor.subscribe(ApplicationStopping) {
        DatabaseConfig.close()
    }

    log.info("Database connected successfully with KMongo")
}
