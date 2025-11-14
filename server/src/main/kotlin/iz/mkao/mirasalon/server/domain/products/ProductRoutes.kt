package iz.mkao.mirasalon.server.domain.products

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
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.dto.CreateProductCategoryRequest
import iz.mkao.mirasalon.core.network.model.dto.CreateProductRequest
import iz.mkao.mirasalon.core.network.model.dto.SubmitReviewRequest
import iz.mkao.mirasalon.core.network.model.dto.UpdateProductCategoryRequest
import iz.mkao.mirasalon.core.network.model.dto.UpdateProductRequest
import iz.mkao.mirasalon.server.data.repository.ProductRepository
import iz.mkao.mirasalon.server.util.AppConfig
import iz.mkao.mirasalon.server.util.getUserId
import iz.mkao.mirasalon.server.util.isAdmin
import iz.mkao.mirasalon.server.util.isSpecialist
import iz.mkao.mirasalon.server.util.validate
import org.slf4j.LoggerFactory
import java.io.File

private val log = LoggerFactory.getLogger("ProductRoutes")

fun Route.productRoutes(
    productRepository: ProductRepository,
    appConfig: AppConfig
) {

    // ── Public endpoints ──

    // List products (public)
    get("") {
        val category = call.request.queryParameters["category"] ?: call.request.queryParameters["categoryId"]
        val subCategory = call.request.queryParameters["subCategory"]
        val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
        val query = call.request.queryParameters["query"]
        val gender = call.request.queryParameters["gender"]

        val productPage = productRepository.findAll(category, subCategory, page, pageSize, query, gender)
        log.info("[ProductRoutes] Returning ${productPage.products.size} products (HasMore: ${productPage.hasMore})")
        call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = productPage))
    }

    // Get product by ID (public)
    get("/{id}") {
        val id = call.parameters["id"]
        if (id.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Product ID required"))
            return@get
        }
        val product = productRepository.findById(id)
        if (product == null) {
            call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Product not found"))
        } else {
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = product))
        }
    }

    // Get product image
    get("/{id}/image") {
        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        log.debug("[ProductRoutes] Fallback image request for product ID: $id")
        val path = productRepository.getImagePath(id) ?: return@get call.respond(HttpStatusCode.NotFound)

        if (path.startsWith("http")) {
            return@get call.respondRedirect(path)
        }

        val cleanPath = path.removePrefix("/uploads/").removePrefix("/")
        val file = File(appConfig.uploadDir, cleanPath)
        if (file.exists()) {
            val contentType = ContentType.fromFilePath(file.name).firstOrNull() ?: ContentType.Image.Any
            call.respond(LocalFileContent(file, contentType))
        } else {
            log.warn("Product image file not found: ${file.absolutePath} (from path: $path)")
            call.respond(HttpStatusCode.NotFound)
        }
    }

    // List categories (public)
    get("/categories") {
        val categories = productRepository.findAllCategories()
        call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = categories))
    }

    // Get category image
    get("/categories/{name}/image") {
        val categoryName = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val path = productRepository.getCategoryImagePath(categoryName) ?: return@get call.respond(HttpStatusCode.NotFound)

        if (path.startsWith("http")) {
            return@get call.respondRedirect(path)
        }

        val cleanPath = path.removePrefix("/uploads/").removePrefix("/")
        val file = File(appConfig.uploadDir, cleanPath)
        if (file.exists()) {
            val contentType = ContentType.fromFilePath(file.name).firstOrNull() ?: ContentType.Image.Any
            call.respond(LocalFileContent(file, contentType))
        } else {
            log.warn("Product category image file not found: ${file.absolutePath} (name: $categoryName, from path: $path)")
            call.respond(HttpStatusCode.NotFound)
        }
    }

    // Health check / Count (public)
    get("/count") {
        val count = productRepository.count()
        call.respond(
            HttpStatusCode.OK,
            ApiResponse
                (success = true,
                data = mapOf("count" to count))
        )
    }

    // Low stock – consider making it admin-only or internal
    get("/low-stock") {
        val threshold = call.request.queryParameters["threshold"]?.toIntOrNull()?.coerceAtLeast(0) ?: 20
        val lowStock = productRepository.findLowStock(threshold)
        call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = lowStock))
    }

    /**
     * GET /v1/api/products/{id}/reviews
     * Returns reviews for a specific product.
     */
    get("/{id}/reviews") {
        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
        val result = productRepository.getReviews(id)
        when (result) {
            is Outcome.Success -> call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result.data))
            is Outcome.Error -> call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = result.failure.toString()))
            else -> {}
        }
    }

    // ── Admin‑only endpoints ──
    authenticate("auth-jwt") {

        /**
         * POST /v1/api/products/{id}/reviews
         * Add a review for a product.
         */
        post("/{id}/reviews") {
            val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<SubmitReviewRequest>()
            
            val result = productRepository.submitReview(id, request.rating, request.comment)
            when (result) {
                is Outcome.Success -> call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = result.data))
                is Outcome.Error -> call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = result.failure.toString()))
                else -> {}
            }
        }

        // ── Product Categories (Admin) ──
        route("/categories") {
            post {
                if (!call.isAdmin()) return@post call.respond(HttpStatusCode.Forbidden)
                val request = call.receive<CreateProductCategoryRequest>()
                val result = productRepository.createCategory(request.name, request.imageUrl, request.description)
                when (result) {
                    is Outcome.Success -> call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = result.data))
                    is Outcome.Error -> {
                        val message = (result.failure as? Failure.ServerError)?.message ?: "Database error"
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = message))
                    }
                    else -> call.respond(HttpStatusCode.InternalServerError)
                }
            }

            put("/{id}") {
                if (!call.isAdmin()) return@put call.respond(HttpStatusCode.Forbidden)
                val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
                val request = call.receive<UpdateProductCategoryRequest>()
                val result = productRepository.updateCategory(id, request.name, request.imageUrl, request.description)
                when (result) {
                    is Outcome.Success -> call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = result.data))
                    is Outcome.Error -> {
                         // Simple error handling
                         call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Update failed"))
                    }
                    else -> call.respond(HttpStatusCode.InternalServerError)
                }
            }

            delete("/{id}") {
                if (!call.isAdmin()) return@delete call.respond(HttpStatusCode.Forbidden)
                val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val result = productRepository.deleteCategory(id)
                when (result) {
                    is Outcome.Success -> call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = "Category deleted"))
                    is Outcome.Error -> {
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Delete failed"))
                    }
                    else -> call.respond(HttpStatusCode.InternalServerError)
                }
            }
        }

        // Create product (staff)
        post("") {
            if (!call.isAdmin() && !call.isSpecialist()) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Staff access required"))
                return@post
            }
            val request = call.receive<CreateProductRequest>()
            validateProduct(request)
            val result = productRepository.create(
                name = request.name,
                description = request.description,
                price = request.price,
                category = request.category,
                stockQuantity = request.stockQuantity,
                imageUrl = request.imageUrl,
                discountPercent = request.discountPercent,
                gender = request.gender
            )
            when (result) {
                is Outcome.Success -> {
                    log.info("Staff {} created product: {}", call.getUserId(), result.data.id)
                    val dto = productRepository.findById(result.data.id)
                    call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = dto))
                }
                is Outcome.Error -> {
                    log.warn("Product creation failed: {}", result.failure)
                    val message = (result.failure as? Failure.ServerError)?.message ?: "Creation failed"
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, error = message)
                    )
                }
                else -> {}
            }
        }

        // Update product (staff)
        put("/{id}") {
            if (!call.isAdmin() && !call.isSpecialist()) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Staff access required"))
                return@put
            }
            val id = call.parameters["id"]
            if (id.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Product ID required"))
                return@put
            }
            val request = call.receive<UpdateProductRequest>()
            validateProduct(request)
            val result = productRepository.update(
                id = id,
                name = request.name,
                description = request.description,
                price = request.price,
                category = request.category,
                stockQuantity = request.stockQuantity,
                imageUrl = request.imageUrl,
                discountPercent = request.discountPercent,
                gender = request.gender
            )
            when (result) {
                is Outcome.Success -> {
                    log.info("Staff {} updated product: {}", call.getUserId(), id)
                    val dto = productRepository.findById(id)
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = dto))
                }
                is Outcome.Error -> {
                    log.warn("Product update failed for {}: {}", id, result.failure)
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Update failed"))
                }
                else -> {}
            }
        }

        // Delete product (staff)
        delete("/{id}") {
            if (!call.isAdmin() && !call.isSpecialist()) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Staff access required"))
                return@delete
            }
            val id = call.parameters["id"]
            if (id.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Product ID required"))
                return@delete
            }
            val result = productRepository.delete(id)
            when (result) {
                is Outcome.Success -> {
                    log.info("Staff {} deleted product: {}", call.getUserId(), id)
                    call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = "Product deleted"))
                }
                is Outcome.Error -> {
                    log.warn("Product deletion failed for {}: {}", id, result.failure)
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = "Deletion failed"))
                }
                else -> {}
            }
        }
    }
}

// ── Shared validation ──
private fun validateProduct(request: CreateProductRequest) {
    validate {
        requireNotBlank("name", request.name)
        requireMaxLength("name", request.name, 200)
        requireNonNegative("price", request.price)
        requireNonNegative("stockQuantity", request.stockQuantity.toDouble())
        requireInRange("discountPercent", request.discountPercent.toDouble(), 0.0, 100.0)
    }
}

private fun validateProduct(request: UpdateProductRequest) {
    validate {
        // Only validate fields that are present (non-null)
        request.name?.let { requireNotBlank("name", it); requireMaxLength("name", it, 200) }
        request.price?.let { requireNonNegative("price", it) }
        request.stockQuantity?.let { requireNonNegative("stockQuantity", it.toDouble()) }
        request.discountPercent?.let { requireInRange("discountPercent", it.toDouble(), 0.0, 100.0) }
    }
}
