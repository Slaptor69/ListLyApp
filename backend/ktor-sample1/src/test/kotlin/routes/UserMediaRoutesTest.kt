/*package routes

import com.example.UserMedia.UserMediaService
import com.example.UserMedia.exceptions.UserMediaNotFoundException
import com.example.UserMedia.model.UserMediaItem
import com.example.UserMedia.model.UserMediaStatus
import com.example.auth.AuthService
import com.example.configureSerialization
import com.example.media.model.MediaType
import com.example.plugins.configureStatusPages
import com.example.routes.AuthRouting
import com.example.routes.UserMediaRouting
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.bson.types.ObjectId
import kotlin.test.Test
import kotlin.test.assertEquals

class UserMediaRoutesTest {



    @Test
    fun `throw if cant find item with get`() = testApplication{
        val service = mockk<UserMediaService>()
        val testId = ObjectId().toString()

        every { service.getById(any(), eq(testId)) } throws UserMediaNotFoundException("u", testId)

        application {
            configureSerialization()
            configureStatusPages()
            UserMediaRouting(service)
        }

        val response = client.get("/user-media/$testId")

        assertEquals(HttpStatusCode.NotFound, response.status)

        verify(exactly = 1) { service.getById(any(), testId) }
    }


    @Test
    fun `throw 409 if item already exists`() = testApplication {
        val service = mockk<UserMediaService>()

    }

} */