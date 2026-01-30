package routes

import com.example.config.configureDatabase
import com.example.config.configureRouting
import com.example.plugins.configureStatusPages
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.Application
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.litote.kmongo.json
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthRoutesTest {


    /* @Test
    fun  `register route should return 201`() = testApplication {
        application{
            configureRouting()
            configureStatusPages()
            configureDatabase()
        }

        val response = client.post("/auth/register") {
            contentType(ContentType.Any)
            setBody("""{"login":"testuser","password":"1234"} """)
        }


        assertEquals(HttpStatusCode.Created,response.status)
    }*/
}