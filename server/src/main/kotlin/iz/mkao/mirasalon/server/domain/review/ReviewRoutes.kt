package iz.mkao.mirasalon.server.domain.review

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.ReviewDto
import iz.mkao.mirasalon.core.network.model.dto.AdminReviewDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateReviewVisibilityRequest
import iz.mkao.mirasalon.server.data.repository.AdminReplyResult
import iz.mkao.mirasalon.server.data.repository.ReviewCreationResult
import iz.mkao.mirasalon.server.data.repository.ReviewRepository
import iz.mkao.mirasalon.server.error.DomainException
import iz.mkao.mirasalon.server.error.ForbiddenException
import iz.mkao.mirasalon.server.error.GeneralDomainException
import iz.mkao.mirasalon.server.error.ResourceNotFoundException
import iz.mkao.mirasalon.server.error.UnauthorizedException
import iz.mkao.mirasalon.server.util.getUserId
import iz.mkao.mirasalon.server.util.isAdmin
import iz.mkao.mirasalon.server.util.validate
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
            throw GeneralDomainException("targetId is required", HttpStatusCode.BadRequest)
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
            ApiResponse<PagedResponse<ReviewDto>>(
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
            throw GeneralDomainException("specialistId is required", HttpStatusCode.BadRequest)
        }

        val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20

        val pagedReviews = reviewRepository.findBySpecialistPaginated(
            specialistId = specialistId,
            page = page,
            pageSize = pageSize
        )

        call.respond(HttpStatusCode.OK, ApiResponse<PagedResponse<ReviewDto>>(success = true, data = pagedReviews))
    }

    authenticate("auth-jwt") {

        // ── Authenticated user endpoints ──

        /**
         * POST /reviews
         * Create a new review (authenticated user).
         */
        post("") {
            val userId = call.getUserId()
                ?: throw UnauthorizedException("Authentication required")
            val request = call.receive<CreateReviewRequest>()
            val targetType = request.targetType.uppercase()

            log.info("Review creation requested by user $userId for target $targetType:${request.targetId}")

            validate {
                requireNotBlank("targetId", request.targetId)
                requireOneOf("targetType", targetType, listOf("APPOINTMENT", "PRODUCT", "SERVICE", "SPECIALIST"))
                if (request.rating !in 1..5) addError("Rating must be between 1 and 5")
            }

            try {
                val result = reviewRepository.createReview(
                    userId = userId,
                    targetId = request.targetId,
                    targetType = targetType,
                    rating = request.rating,
                    comment = request.comment,
                    imageUrl = request.imageUrl
                )

                when (result) {
                    is ReviewCreationResult.Success -> {
                        log.info("Review created: ${result.reviewId} by user $userId")
                        call.respond(HttpStatusCode.Created, ApiResponse<String>(success = true, data = result.reviewId))
                    }
                    is ReviewCreationResult.Error -> {
                        log.warn("Review creation failed: ${result.message} for user $userId")
                        throw GeneralDomainException(result.message, HttpStatusCode.BadRequest)
                    }
                }
            } catch (e: Exception) {
                if (e is DomainException) throw e
                log.error("Unexpected error creating review", e)
                throw GeneralDomainException("Internal server error", HttpStatusCode.InternalServerError)
            }
        }

        get("/all") {
            if (!call.isAdmin()) {
                throw ForbiddenException("Admin access required")
            }
            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
            val query = call.request.queryParameters["query"]

            try {
                val pagedReviews = reviewRepository.findAllPaginated(page, pageSize, query)
                log.info("Admin ${call.getUserId()} fetched all reviews (page $page, size $pageSize, query $query)")
                call.respond(HttpStatusCode.OK, ApiResponse<PagedResponse<AdminReviewDto>>(success = true, data = pagedReviews))
            } catch (e: Exception) {
                log.error("Error fetching all reviews", e)
                throw GeneralDomainException("Failed to fetch reviews", HttpStatusCode.InternalServerError)
            }
        }

        /**
         * POST /reviews/{id}/reply
         * Admin replies to a review.
         */
        post("/{id}/reply") {
            if (!call.isAdmin()) {
                throw ForbiddenException("Admin access required")
            }

            val reviewId = call.parameters["id"]
            if (reviewId.isNullOrBlank()) {
                throw GeneralDomainException("Review ID required", HttpStatusCode.BadRequest)
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
                    throw ResourceNotFoundException("Review not found")
                }

                is AdminReplyResult.Error -> {
                    log.warn("Reply failed for review $reviewId: $result")
                    throw GeneralDomainException(result.message, HttpStatusCode.BadRequest)
                }
            }
        }

        /**
         * PUT /v1/api/reviews/{id}/visibility
         * Toggle review visibility.
         */
        put("/{id}/visibility") {
            if (!call.isAdmin()) {
                throw ForbiddenException("Admin access required")
            }
            val id = call.parameters["id"] ?: throw GeneralDomainException("Review ID required", HttpStatusCode.BadRequest)
            val request = call.receive<UpdateReviewVisibilityRequest>()

            reviewRepository.updateVisibility(id, request.isVisible)
            call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true, data = Unit))
        }

        /**
         * DELETE /v1/api/reviews/{id}
         * Delete a review.
         */
        delete("/{id}") {
            if (!call.isAdmin()) {
                throw ForbiddenException("Admin access required")
            }
            val id = call.parameters["id"] ?: throw GeneralDomainException("Review ID required", HttpStatusCode.BadRequest)

            reviewRepository.deleteReview(id)
            call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true, data = Unit))
        }

        // ── Admin‑only endpoints ──
        route("/admin") {
            /**
             * GET /reviews/admin
             * Admin list all reviews (paginated).
             */
            get {
                if (!call.isAdmin()) {
                    throw ForbiddenException("Admin access required")
                }
                val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20

                val pagedReviews = reviewRepository.findAllPaginated(page, pageSize)
                log.info("Admin ${call.getUserId()} fetched all reviews (page $page, size $pageSize)")
                call.respond(HttpStatusCode.OK, ApiResponse<PagedResponse<AdminReviewDto>>(success = true, data = pagedReviews))
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
