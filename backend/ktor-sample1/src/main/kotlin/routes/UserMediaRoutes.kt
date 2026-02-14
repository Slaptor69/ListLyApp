package com.example.routes

import com.example.UserMedia.UserMediaRepository
import com.example.UserMedia.UserMediaService
import com.example.UserMedia.dto.CreateUserMediaRequest
import com.example.UserMedia.dto.UpdateUserMediaRequest
import com.example.UserMedia.dto.toResponse
import com.example.UserMedia.model.UserMediaItem
import com.example.UserMedia.model.UserMediaStatus
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.UserMediaRouting(userMediaService: UserMediaService) {
    routing {
        authenticate("auth-jwt") {
            route("/user-media") {

                    get("/{userMediaId}") {
                        val principal = call.principal<JWTPrincipal>()!!
                        val userId = principal.payload.getClaim("userId").asString()
                        val userMediaId =
                            call.parameters["userMediaId"] ?: return@get call.respond(HttpStatusCode.BadRequest)


                        val item = userMediaService.getById(userId, userMediaId)
                        call.respond(item.toResponse())
                    }

                get {
                    val principal = call.principal<JWTPrincipal>()!!
                    val userId = principal.payload.getClaim("userId").asString()

                    val items = userMediaService
                        .getAllMediaItemsByUserId(userId)
                        .map { it.toResponse() }

                    call.respond(items)
                }

                    patch("/{userMediaId}") {

                        val request = call.receive<UpdateUserMediaRequest>()
                        val principal = call.principal<JWTPrincipal>()!!
                        val userId = principal.payload.getClaim("userId").asString()
                        val userMediaId =
                            call.parameters["userMediaId"] ?: return@patch call.respond(HttpStatusCode.BadRequest)

                        userMediaService.update(userId, userMediaId, request)

                        call.respond(HttpStatusCode.OK)
                    }

                    post{
                        val principal = call.principal<JWTPrincipal>()!!
                        val userId = principal.payload.getClaim("userId").asString()

                        val request = call.receive<CreateUserMediaRequest>()
                        val newItem = UserMediaItem(
                            userId = userId,
                            title = request.title,
                            mediaType = request.mediaType,
                            userMediaStatus = request.userMediaStatus ?: UserMediaStatus.PLANNED,
                            userRating = request.userRating,
                            note = request.note
                        )
                        userMediaService.create(userId, newItem)
                        call.respond(HttpStatusCode.Created)

                    }

                    delete("/{userMediaId}") {
                        val principal = call.principal<JWTPrincipal>()!!
                        val userId = principal.payload.getClaim("userId").asString()
                        val userMediaId =
                            call.parameters["userMediaId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)

                        userMediaService.delete(userId, userMediaId)
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    }
fun Application.UserMediaRouting() {
    val userMediaRepository = UserMediaRepository()
    val userMediaService = UserMediaService(userMediaRepository)
    UserMediaRouting(userMediaService)
}
