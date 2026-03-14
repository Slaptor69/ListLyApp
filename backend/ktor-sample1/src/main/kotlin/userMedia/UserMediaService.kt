package com.example.UserMedia

import com.example.UserMedia.dto.UpdateUserMediaRequest
import com.example.UserMedia.exceptions.InvalidUserMediaRequestException
import com.example.UserMedia.exceptions.UserMediaAlreadyExistsException
import com.example.UserMedia.exceptions.UserMediaNotFoundException
import com.example.UserMedia.model.UserMediaItem


class UserMediaService (private val userMediaRepository: UserMediaRepository){
    fun getAllMediaItemsByUserId(userId:String) : List<UserMediaItem> {
        return userMediaRepository.findAllByUser(userId)
    }

    fun getById(userId: String,userMediaId:String): UserMediaItem{
        return userMediaRepository.findById(userId,userMediaId) ?: throw UserMediaNotFoundException(userId,userMediaId)
    }

    fun create(userId: String, item: UserMediaItem) {
        val existing = userMediaRepository.findByMediaIdAndUserId(userId, item.mediaId)
        if (existing != null) throw UserMediaAlreadyExistsException(userId, item.mediaId)

        val safeItem = UserMediaItem(
            id = item.id,
            userId = userId,
            mediaId = item.mediaId,
            userMediaStatus = item.userMediaStatus,
            userRating = item.userRating,
            note = item.note,
            createdAt = item.createdAt,
            updatedAt = item.updatedAt
        )

        userMediaRepository.save(safeItem)
    }


    fun update(userId:String,
               userMediaId:String,
               request: UpdateUserMediaRequest){
        val existing = userMediaRepository.findById(userId,userMediaId) ?: throw UserMediaNotFoundException(userId,userMediaId)

        if (request.userRating !=null && request.userRating !in 0.0..10.0){
            throw InvalidUserMediaRequestException("Your Rate must be from 0.0 to 10.0, not ${request.userRating}")
        }

        if (request.note!=null && request.note.length > 400){
            throw InvalidUserMediaRequestException("The note must be less than 400 characters long. ${request.note.length} is too much")
        }

        userMediaRepository.update(userId,userMediaId,request)


    }
    fun delete(userId: String, userMediaId: String) {
        val existing = userMediaRepository.findById(userId, userMediaId)
            ?: throw UserMediaNotFoundException(userId, userMediaId)

        userMediaRepository.delete(userId, userMediaId)
    }
}
