package userMedia

import com.example.UserMedia.UserMediaRepository
import com.example.UserMedia.UserMediaService
import com.example.UserMedia.dto.UpdateUserMediaRequest
import com.example.UserMedia.exceptions.InvalidUserMediaRequestException
import com.example.UserMedia.exceptions.UserMediaAlreadyExistsException
import com.example.UserMedia.exceptions.UserMediaNotFoundException
import com.example.UserMedia.model.UserMediaItem
import com.example.UserMedia.model.UserMediaStatus
import com.example.media.model.MediaType
import io.mockk.Runs
import io.mockk.mockk
import io.mockk.every
import io.mockk.just
import io.mockk.runs
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
    fun `update successfully updates fields`() {
        val userId = "9492"
        val id = ObjectId().toString()

        val item = UserMediaItem(
            id = id,
            title = "old",
            userId = userId,
            mediaType = MediaType.MOVIE,
            userMediaStatus = UserMediaStatus.PLANNED,
            userRating = 5.0,
            note = "old note",
            createdAt = 1L,
            updatedAt = 1L
        )

        every { repository.findById(userId, id) } returns item
        every { repository.update(userId, id, any()) } just Runs

        val request = UpdateUserMediaRequest(
            note = "new note",
            userRating = 8.0,
            userMediaStatus = UserMediaStatus.COMPLETED
        )

        val result = service.update(userId, id, request)

        verify(exactly = 1) { repository.update(userId, id, any()) }
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

    @Test
    fun `getById throws if not found`() {
        every { repository.findById("u", "id") } returns null
        assertFailsWith<UserMediaNotFoundException> { service.getById("u", "id") }
    }

    @Test
    fun `create throws if item already exists`() {
        val userId = "u"
        val item = UserMediaItem(userId=userId,title="t",mediaType=MediaType.MOVIE,userMediaStatus=UserMediaStatus.PLANNED)
        every { repository.findByItemAndUserId(userId, "t", MediaType.MOVIE) } returns item

        assertFailsWith<UserMediaAlreadyExistsException> { service.create(userId, item) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `delete deletes if exists`() {
        val userId = "u"
        val id = "id"
        every { repository.findById(userId, id) } returns UserMediaItem(userId=userId,title="t",mediaType=MediaType.MOVIE,userMediaStatus=UserMediaStatus.PLANNED)
        every { repository.delete(userId, id) } just Runs

        service.delete(userId, id)

        verify(exactly = 1) { repository.delete(userId, id) }
    }

    @Test
    fun `create saves media item if not exists`() {
        val userId = "u"

        val item = UserMediaItem(
            userId = userId,
            title = "t",
            mediaType = MediaType.MOVIE,
            userMediaStatus = UserMediaStatus.PLANNED
        )

        every {
            repository.findByItemAndUserId(userId, "t", MediaType.MOVIE)
        } returns null

        every { repository.save(any()) } just Runs

        service.create(userId, item)

        verify(exactly = 1) {
            repository.findByItemAndUserId(userId, "t", MediaType.MOVIE)
        }


    }





}