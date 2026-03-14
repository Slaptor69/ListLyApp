package com.example.routes

import com.example.UserMedia.UserMediaRepository
import com.example.UserMedia.UserMediaService
import com.example.UserMedia.dto.CreateUserMediaRequest
import com.example.UserMedia.dto.UpdateUserMediaRequest
import com.example.UserMedia.dto.toResponse
import com.example.UserMedia.model.UserMediaItem
import com.example.UserMedia.model.UserMediaStatus
import com.example.security.JwtUserIdProvider
import com.example.security.UserIdProvider
import com.example.security.requireUserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Application.UserMediaRouting(
    userMediaService: UserMediaService,
    userIdProvider: UserIdProvider = JwtUserIdProvider()
) {
    routing {
        authenticate("auth-jwt") {
            route("/user-media") {

                get("/{userMediaId}") {
                    val userId = call.requireUserId(userIdProvider) ?: return@get
                    val userMediaId = call.parameters["userMediaId"]
                        ?: return@get call.respond(HttpStatusCode.BadRequest)

                    val item = userMediaService.getById(userId, userMediaId)
                    call.respond(item.toResponse())
                }

                get {
                    val userId = call.requireUserId(userIdProvider) ?: return@get
                    val items = userMediaService
                        .getAllMediaItemsByUserId(userId)
                        .map { it.toResponse() }

                    call.respond(items)
                }

                patch("/{userMediaId}") {
                    val userId = call.requireUserId(userIdProvider) ?: return@patch
                    val userMediaId = call.parameters["userMediaId"]
                        ?: return@patch call.respond(HttpStatusCode.BadRequest)

                    val request = call.receive<UpdateUserMediaRequest>()
                    userMediaService.update(userId, userMediaId, request)
                    call.respond(HttpStatusCode.OK)
                }

                post {
                    val userId = call.requireUserId(userIdProvider) ?: return@post

                    val request = call.receive<CreateUserMediaRequest>()
                    val newItem = UserMediaItem(
                        userId = userId,
                        mediaId = request.mediaId,
                        userMediaStatus = request.userMediaStatus ?: UserMediaStatus.PLANNED,
                        userRating = request.userRating,
                        note = request.note
                    )

                    userMediaService.create(userId, newItem)
                    call.respond(HttpStatusCode.Created)
                }

                delete("/{userMediaId}") {
                    val userId = call.requireUserId(userIdProvider) ?: return@delete
                    val userMediaId = call.parameters["userMediaId"]
                        ?: return@delete call.respond(HttpStatusCode.BadRequest)

                    userMediaService.delete(userId, userMediaId)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

// Prod wiring отдельно
fun Application.UserMediaRouting() {
    val repo = UserMediaRepository()
    val service = UserMediaService(repo)
    UserMediaRouting(service, JwtUserIdProvider())
}
