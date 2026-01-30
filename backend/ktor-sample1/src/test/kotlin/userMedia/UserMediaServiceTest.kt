package userMedia

import com.example.UserMedia.UserMediaRepository
import com.example.UserMedia.UserMediaService
import com.example.UserMedia.dto.UpdateUserMediaRequest
import com.example.UserMedia.exceptions.InvalidUserMediaRequestException
import com.example.UserMedia.exceptions.UserMediaNotFoundException
import com.example.UserMedia.model.UserMediaItem
import com.example.UserMedia.model.UserMediaStatus
import com.example.media.model.MediaType
import io.mockk.mockk
import io.mockk.every
import io.mockk.verify
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserMediaServiceTest {
    private val repository = mockk<UserMediaRepository>()
    private val service = UserMediaService(repository)


    @Test
    fun `Get UserMediaItem if it exists`(){
        val testId = ObjectId().toString()
        val testUserId = "9492"
        val item = UserMediaItem(
            id = testId, title = "testItem",
            userId = testUserId,
            mediaType = MediaType.MOVIE,
            userMediaStatus = UserMediaStatus.PLANNED,
            userRating = 0.0,
            note = "test",
            createdAt =System.currentTimeMillis(),
            updatedAt =System.currentTimeMillis()
        )

        every { repository.findById(testUserId,testId) } returns item

        val result = service.getById(testUserId,testId)

        assertEquals(item,result)

        verify(exactly = 1) { repository.findById(testUserId,testId) }



    }

    @Test
    fun `throw exception if note in update is more than 400`(){
        val testId = ObjectId().toString()
        val testUserId = "9492"

        val item = UserMediaItem(
            id = testId,
            title = "testItem",
            userId = testUserId,
            mediaType = MediaType.MOVIE,
            userMediaStatus = UserMediaStatus.PLANNED,
            userRating = 0.0,
            note = "old",
            createdAt = 1L,
            updatedAt = 1L
        )

        every { repository.findById(testUserId,testId) } returns item

        val request = UpdateUserMediaRequest(
            note = "a".repeat(401)
        )

        assertFailsWith<InvalidUserMediaRequestException> {
            service.update(testUserId,testId,request)
        }

        verify(exactly = 1) { repository.findById(testUserId, testId) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `throw exception if userRating in update is not in range`(){
        val testId = ObjectId().toString()
        val testUserId = "9492"

        val item = UserMediaItem(
            id = testId,
            title = "testItem",
            userId = testUserId,
            mediaType = MediaType.MOVIE,
            userMediaStatus = UserMediaStatus.PLANNED,
            userRating = 0.0,
            note = "note",
            createdAt = 1L,
            updatedAt = 1L
        )

        every { repository.findById(testUserId,testId) } returns item

        val request = UpdateUserMediaRequest(
            userRating = 11.0
        )

        assertFailsWith<InvalidUserMediaRequestException> {
            service.update(testUserId,testId,request)
        }

        verify(exactly = 1) { repository.findById(testUserId, testId) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `delete throws exception if not exists`() {
        val testId = ObjectId().toString()
        val testUserId = "9492"

        every { repository.findById(testUserId, testId) } returns null

        assertFailsWith<UserMediaNotFoundException> {
            service.delete(testUserId, testId)
        }

        verify(exactly = 0) { repository.delete(testUserId, testId) }
    }


}