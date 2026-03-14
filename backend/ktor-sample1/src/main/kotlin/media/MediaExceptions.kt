package com.example.media

open class MediaException(
    message: String,
    cause: Throwable? = null

): RuntimeException(message,cause)

//class UserMediaNotFoundException(userId: String?=null,
//                                 userMediaId:String?=null): MediaException(
//    message = if (userId!=null && userMediaId!=null) "userMedia not found for userId=$userId and mediaId=$userMediaId" else "UserMedia not found"
//)

class MediaAlreadyExistsException(mediaId:String?=null): MediaException("mediaItem $mediaId already exists")

//class InvalidUserMediaRequestException(
//    message:String
//) : UserMediaException(
//    message
//)