package com.example.routes

import com.example.media.MediaCatalogRepository
import com.example.media.MediaCatalogService
import com.example.media.dto.CreateMediaRequest
import com.example.media.dto.UpdateMediaRequest
import com.example.search.service.MeiliMediaSearchServiceImpl
import com.example.search.service.SearchService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.litote.kmongo.limit

fun Application.GlobalMediaRouting(
    mediaService: MediaCatalogService,
) {
    fun Route.registerCatalogEndpoints() {
        get("/{mediaId}") {
            val mediaId = call.parameters["mediaId"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val item = mediaService.findById(mediaId) ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respond(item)
        }

        get("/items/{title}") {
            val title = call.parameters["title"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val items = mediaService.findAllByTitle(title)
            call.respond(items)
        }

        patch("/admin/{mediaId}") {
            val mediaId = call.parameters["mediaId"] ?: return@patch call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<UpdateMediaRequest>()
            mediaService.updateByAdmin(mediaId, request)
            call.respond(HttpStatusCode.OK)
        }

        post {
            val request = call.receive<CreateMediaRequest>()
            val created = mediaService.create(request)
            call.respond(HttpStatusCode.Created, created)
        }

        delete("/{mediaId}") {
            val mediaId = call.parameters["mediaId"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            mediaService.delete(mediaId)
            call.respond(HttpStatusCode.OK)
        }
    }


    routing {
        route("/mediaCatalog") {
            registerCatalogEndpoints()
        }
        route("/media") {
            registerCatalogEndpoints()
        }
    }
}

//prod-wiring для global

fun Application.GlobalMediaRoutes() {
    val repo = MediaCatalogRepository()
    val service = MediaCatalogService(repo)
    GlobalMediaRouting(service)
}

fun Application.searchRoutes(searchService: SearchService){
    routing {
        route("/media/search") {
            get {
                val query = call.request.queryParameters["query"].orEmpty()
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 12
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0


                val result = searchService.search(query, limit, offset)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
