package iz.mkao.mirasalon.server.domain.review

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.dto.UpdateReviewVisibilityRequest
import iz.mkao.mirasalon.server.data.repository.AdminReplyResult
import iz.mkao.mirasalon.server.data.repository.ReviewCreationResult
import iz.mkao.mirasalon.server.data.repository.ReviewRepository
import iz.mkao.mirasalon.server.util.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("ReviewRoutes")

fun Route.reviewRoutes(
    reviewRepository: ReviewRepository
) {

    // ── Public (unauthenticated) endpoints ──

    /**
     * GET /reviews?targetId=...&targetType=...&page=...&pageSize=...
     * Returns paginated reviews for a specific target (product or appointment).
     */
    get("") {
        val targetId = call.request.queryParameters["targetId"]
        if (targetId.isNullOrBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(
                    success = false,
                    error = "targetId is required"
                )
            )
            return@get
        }

        val targetType = call.request.queryParameters["targetType"]
            ?.takeIf { it.isNotBlank() } ?: "PRODUCT"
        val page = call.request.queryParameters["page"]
            ?.toIntOrNull()
            ?.coerceAtLeast(1) ?: 1
        val pageSize = call.request.queryParameters["pageSize"]
            ?.toIntOrNull()
            ?.coerceIn(1, 100) ?: 20

        // Use the new paginated repository method
        val pagedReviews = reviewRepository.findByTargetPaginated(
            targetId = targetId,
            targetType = targetType,
            page = page,
            pageSize = pageSize
        )

        log.debug(
            "Fetched ${pagedReviews.items.size} reviews for target $targetId " +
                "(page $page, size $pageSize)"
        )
        call.respond(
            HttpStatusCode.OK,
            ApiResponse(
                success = true,
                data = pagedReviews
            )
        )
    }

    /**
     * GET /reviews/specialist/{id}?page=...&pageSize=...
     * Returns reviews related to a specific specialist (via their appointments).
     */
    get("/specialist/{id}") {
        val specialistId = call.parameters["id"]
        if (specialistId.isNullOrBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(
                    success = false,
                    error = "specialistId is required"
                )
            )
            return@get
        }

        val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20

        val pagedReviews = reviewRepository.findBySpecialistPaginated(
            specialistId = specialistId,
            page = page,
            pageSize = pageSize
        )

        call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = pagedReviews))
    }

    authenticate("auth-jwt") {

        // ── Authenticated user endpoints ──

        /**
         * POST /reviews
         * Create a new review (authenticated user).
         */
        post("") {
            val userId = call.getUserId()
                ?: return@post call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, error = "Authentication required")
                )
            val request = call.receive<CreateReviewRequest>()

            log.info("Review creation requested by user $userId for target ${request.targetType}:${request.targetId}")

            validate {
                requireNotBlank("targetId", request.targetId)
                requireOneOf("targetType", request.targetType, listOf("APPOINTMENT", "PRODUCT", "SERVICE"))
                if (request.rating !in 1..5) addError("Rating must be between 1 and 5")
            }

            try {
                val result = reviewRepository.createReview(
                    userId = userId,
                    targetId = request.targetId,
                    targetType = request.targetType,
                    rating = request.rating,
                    comment = request.comment,
                    imageUrl = request.imageUrl
                )

                when (result) {
                    is ReviewCreationResult.Success -> {
                        log.info("Review created: ${result.reviewId} by user $userId")
                        call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = result.reviewId))
                    }
                    is ReviewCreationResult.Error -> {
                        log.warn("Review creation failed: ${result.message} for user $userId")
                        call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = result.message))
                    }
                }
            } catch (e: Exception) {
                log.error("Unexpected error creating review", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(success = false, error = "Internal server error")
                )
            }
        }

        get("/all") {
            if (!call.isAdmin()) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(success = false, error = "Admin access required")
                )
                return@get
            }
            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
            val query = call.request.queryParameters["query"]

            val pagedReviews = reviewRepository.findAllPaginated(page, pageSize, query)
            log.info("Admin ${call.getUserId()} fetched all reviews (page $page, size $pageSize, query $query)")
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = pagedReviews))
        }

        /**
         * POST /reviews/{id}/reply
         * Admin replies to a review.
         */
        post("/{id}/reply") {
            if (!call.isAdmin()) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(success = false, error = "Admin access required")
                )
                return@post
            }

            val reviewId = call.parameters["id"]
            if (reviewId.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = "Review ID required")
                )
                return@post
            }

            val request = call.receive<AdminReplyRequest>()
            validate {
                requireNotBlank("reply", request.reply)
            }

            val result = reviewRepository.addAdminReply(reviewId, request.reply)
            when (result) {
                is AdminReplyResult.Success -> {
                    log.info("Admin ${call.getUserId()} replied to review $reviewId")
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true))
                }

                is AdminReplyResult.NotFound -> {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Review not found"))
                }

                is AdminReplyResult.Error -> {
                    log.warn("Reply failed for review $reviewId: $result")
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = result.message))
                }
            }
        }

        /**
         * PUT /v1/api/reviews/{id}/visibility
         * Toggle review visibility.
         */
        put("/{id}/visibility") {
            if (!call.isAdmin()) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Admin access required"))
                return@put
            }
            val id = call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest)
            val request = call.receive<UpdateReviewVisibilityRequest>()

            reviewRepository.updateVisibility(id, request.isVisible)
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = Unit))
        }

        /**
         * DELETE /v1/api/reviews/{id}
         * Delete a review.
         */
        delete("/{id}") {
            if (!call.isAdmin()) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Admin access required"))
                return@delete
            }
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)

            reviewRepository.deleteReview(id)
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = Unit))
        }

        // ── Admin‑only endpoints ──
        route("/admin") {
            /**
             * GET /reviews/admin
             * Admin list all reviews (paginated).
             */
            get {
                if (!call.isAdmin()) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Admin access required"))
                    return@get
                }
                val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20

                val pagedReviews = reviewRepository.findAllPaginated(page, pageSize)
                log.info("Admin ${call.getUserId()} fetched all reviews (page $page, size $pageSize)")
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = pagedReviews))
            }
        }
    }
}

// ---- DTOs ----
@Serializable
data class CreateReviewRequest(
    val targetId: String,
    val targetType: String,
    val rating: Int,
    val comment: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class AdminReplyRequest(
    val reply: String
)
