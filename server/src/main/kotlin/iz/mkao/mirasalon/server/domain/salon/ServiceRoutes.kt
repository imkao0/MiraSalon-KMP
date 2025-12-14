package iz.mkao.mirasalon.server.domain.salon

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.fromFilePath
import io.ktor.server.auth.authenticate
import io.ktor.server.http.content.LocalFileContent
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.dto.CreateServiceCategoryRequest
import iz.mkao.mirasalon.core.network.model.dto.CreateServiceRequestDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateServiceCategoryRequest
import iz.mkao.mirasalon.core.network.model.dto.UpdateServiceRequestDto
import iz.mkao.mirasalon.server.data.repository.CategoryDeleteResult
import iz.mkao.mirasalon.server.data.repository.CategoryOperationResult
import iz.mkao.mirasalon.server.data.repository.ServiceCreateResult
import iz.mkao.mirasalon.server.data.repository.ServiceDeleteResult
import iz.mkao.mirasalon.server.data.repository.ServiceFetchResult
import iz.mkao.mirasalon.server.data.repository.ServiceRepository
import iz.mkao.mirasalon.server.data.repository.ServiceUpdateResult
import iz.mkao.mirasalon.server.util.AppConfig
import iz.mkao.mirasalon.server.util.ensureAdmin
import iz.mkao.mirasalon.server.util.getUserId
import iz.mkao.mirasalon.server.util.isAdmin
import iz.mkao.mirasalon.server.util.validate
import org.slf4j.LoggerFactory
import java.io.File

private val log = LoggerFactory.getLogger("ServiceRoutes")

fun Route.serviceRoutes(
    serviceRepository: ServiceRepository,
    appConfig: AppConfig
) {
    get("") {
        val categoryId = call.request.queryParameters["categoryId"]
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

        val pagedServices = serviceRepository.findAll(categoryId, page, pageSize)
        call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = pagedServices))
    }

    get("/{id}/image") {
        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val path = serviceRepository.getImagePath(id) ?: return@get call.respond(HttpStatusCode.NotFound)

        if (path.startsWith("http")) {
            return@get call.respondRedirect(path)
        }

        val cleanPath = path.removePrefix("/uploads/").removePrefix("uploads/").removePrefix("/")
        val file = File(appConfig.uploadDir, cleanPath)
        if (file.exists()) {
            val contentType = ContentType.fromFilePath(file.name).firstOrNull() ?: ContentType.Image.Any
            call.respond(LocalFileContent(file, contentType))
        } else {
            log.warn("Service image file not found: ${file.absolutePath} (from path: $path)")
            call.respond(HttpStatusCode.NotFound)
        }
    }

    get("/categories") {
        val categories = serviceRepository.findAllCategories()
        call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = categories))
    }

    get("/categories/{id}/image") {
        val categoryId = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val path = serviceRepository.getCategoryImagePath(categoryId) ?: return@get call.respond(HttpStatusCode.NotFound)

        if (path.startsWith("http")) {
            return@get call.respondRedirect(path)
        }

        val cleanPath = path.removePrefix("/uploads/").removePrefix("uploads/").removePrefix("/")
        val file = File(appConfig.uploadDir, cleanPath)
        if (file.exists()) {
            val contentType = ContentType.fromFilePath(file.name).firstOrNull() ?: ContentType.Image.Any
            call.respond(LocalFileContent(file, contentType))
        } else {
            log.warn("Category image file not found: ${file.absolutePath} (from path: $path)")
            call.respond(HttpStatusCode.NotFound)
        }
    }

    get("/{id}") {
        val id = call.parameters["id"] ?: return@get call.respond(
            HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Missing ID")
        )
        val serviceFetch = serviceRepository.findById(id)
        when (serviceFetch) {
            is ServiceFetchResult.Success -> {
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = serviceFetch.service))
            }
            is ServiceFetchResult.NotFound -> {
                call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Service not found"))
            }
        }
    }

    authenticate("auth-jwt") {
        route("/categories") {
            post {
                if (!call.isAdmin()) return@post call.respond(HttpStatusCode.Forbidden)
                val request = call.receive<CreateServiceCategoryRequest>()
                val result = serviceRepository.createCategory(request.name, request.iconName, request.imageUrl)
                when (result) {
                    is CategoryOperationResult.Success -> call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = result.category))
                    is CategoryOperationResult.Failure -> call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = result.message))
                    else -> call.respond(HttpStatusCode.InternalServerError)
                }
            }

            put("/{id}") {
                if (!call.isAdmin()) return@put call.respond(HttpStatusCode.Forbidden)
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<UpdateServiceCategoryRequest>()
                val result = serviceRepository.updateCategory(id, request.name, request.iconName, request.imageUrl)
                when (result) {
                    is CategoryOperationResult.Success -> call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result.category))
                    is CategoryOperationResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                    is CategoryOperationResult.Failure -> call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = result.message))
                }
            }

            delete("/{id}") {
                if (!call.isAdmin()) return@delete call.respond(HttpStatusCode.Forbidden)
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val result = serviceRepository.deleteCategory(id)
                when (result) {
                    is CategoryDeleteResult.Success -> call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = "Category deleted"))
                    is CategoryDeleteResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                    is CategoryDeleteResult.Failure -> call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = result.message))
                }
            }
        }


        post("") {
            call.ensureAdmin()
            val adminId = call.getUserId()
            val request = call.receive<CreateServiceRequestDto>()
            validate {
                requireNotBlank("name", request.name)
                requirePositive("price", request.price)
            }

            val result = serviceRepository.create(request)
            when (result) {
                is ServiceCreateResult.Success -> {
                    log.info("Admin {} created service: {}", adminId, result.service.id)
                    call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = result.service))
                }
                is ServiceCreateResult.Failure -> {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = result.message))
                }
            }
        }

        put("/{id}") {
            call.ensureAdmin()
            val adminId = call.getUserId()
            val id = call.parameters["id"]
            if (id.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "ID required"))
                return@put
            }

            val request = call.receive<UpdateServiceRequestDto>()
            validate {
                request.price?.let { requirePositive("price", it) }
                request.name?.let { requireNotBlank("name", it) }
            }

            when (val result = serviceRepository.update(id, request)) {
                is ServiceUpdateResult.Success -> {
                    log.info("Admin {} updated service: {}", adminId, id)
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true))
                }
                is ServiceUpdateResult.NotFound -> {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Service not found"))
                }
                is ServiceUpdateResult.Failure -> {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = result.message))
                }
            }
        }

        delete("/{id}") {
            call.ensureAdmin()
            val adminId = call.getUserId()
            val id = call.parameters["id"]
            if (id.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "ID required"))
                return@delete
            }

            val result = serviceRepository.delete(id)
            when (result) {
                is ServiceDeleteResult.Success -> {
                    log.info("Admin {} deleted service: {}", adminId, id)
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true))
                }
                is ServiceDeleteResult.NotFound -> {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Service not found"))
                }
                is ServiceDeleteResult.Failure -> {
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = result.message))
                }
            }
        }
    }
}
