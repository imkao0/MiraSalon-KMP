package iz.mkao.mirasalon.server.domain.order

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import iz.mkao.mirasalon.core.domain.model.UserRole
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.ApiResponse
import iz.mkao.mirasalon.core.network.model.dto.CreateOrderRequest
import iz.mkao.mirasalon.core.network.model.dto.UpdateOrderStatusRequest
import iz.mkao.mirasalon.server.data.repository.OrderCreationResult
import iz.mkao.mirasalon.server.data.repository.OrderRepoStatus
import iz.mkao.mirasalon.server.data.repository.OrderRepository
import iz.mkao.mirasalon.server.data.repository.OrderStatusUpdateResult
import iz.mkao.mirasalon.server.util.getUserId
import iz.mkao.mirasalon.server.util.getUserRole
import iz.mkao.mirasalon.server.util.isAdmin
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("OrderRoutes")

fun Route.orderRoutes(orderRepository: OrderRepository) {

    authenticate("auth-jwt") { // all endpoints require authentication


        post("/checkout") {
            val userId = call.getUserId()
                ?: return@post call.respond(HttpStatusCode.Unauthorized)
            val idempotencyKey = call.request.headers["Idempotency-Key"]

            val request = try {
                call.receive<CreateOrderRequest>()
            } catch (e: Exception) {
                log.warn("Invalid checkout request from user {}: {}", userId, e.message)
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, error = "Invalid request format: ${e.message}")
                )
            }

            log.info("Checkout request from user {}: total {}, items {}", userId, request.totalAmount, request.items.size)
            
            val salonId = request.salonId ?: return@post call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, error = "Salon ID is required")
            )

            val result = orderRepository.createOrder(
                userId = userId,
                salonId = salonId,
                items = request.items,
                shippingAddress = request.shippingAddress,
                paymentMethod = request.paymentMethod,
                specialInstructions = request.specialInstructions,
                promotionCode = request.promoCode,
                idempotencyKey = idempotencyKey
            )

            when (result) {
                is OrderCreationResult.Success -> {
                    log.info("Order created: {} for user {}", result.orderId, userId)
                    val orderDto = orderRepository.findOrderById(result.orderId)
                    call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = orderDto))
                }
                is OrderCreationResult.Error -> {
                    log.warn("Checkout failed for user {}: {}", userId, result.toString())
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = result.toString()))
                }
            }
        }


        get("/all") {
            if (!call.isAdmin()) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Admin access required"))
                return@get
            }
            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
            val status = call.request.queryParameters["status"]?.let {
                try { OrderRepoStatus.valueOf(it.uppercase()) }
                catch (e: Exception) { null }
            }
            val query = call.request.queryParameters["query"]
            val dateFrom = call.request.queryParameters["dateFrom"]?.toLongOrNull()
            val dateTo = call.request.queryParameters["dateTo"]?.toLongOrNull()

            val orders = orderRepository.findAllOrdersPaginated(
                page = page,
                pageSize = pageSize,
                status = status,
                query = query,
                dateFrom = dateFrom,
                dateTo = dateTo
            )
            log.info("Admin {} fetched orders page {} with status {} query {} dateFrom {} dateTo {}", 
                call.getUserId(), page, status, query, dateFrom, dateTo)
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = orders))
        }


        put("/{id}/status") {
            val id = call.parameters["id"]
            if (id.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Order ID is required"))
                return@put
            }

            // Check if user is owner of the order or an admin
            val userId = call.getUserId() ?: return@put call.respond(HttpStatusCode.Unauthorized)
            val isAdmin = call.isAdmin()

            val request = call.receive<UpdateOrderStatusRequest>()
            val statusParam = request.status

            val status = try {
                OrderRepoStatus.valueOf(statusParam.uppercase())
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid status: $statusParam"))
                return@put
            }

            // Authorization logic
            if (!isAdmin) {
                val order = orderRepository.findOrderById(id)
                if (order == null) {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Order not found"))
                    return@put
                }

                val isOwner = order.userId == userId
                val isSpecialistAtSalon = call.getUserRole() == UserRole.SPECIALIST // For now, allow all specialists or check salon

                if (!isOwner && !isSpecialistAtSalon) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Access denied"))
                    return@put
                }

                // Owners can only cancel or see delivered
                if (isOwner && !isSpecialistAtSalon && status != OrderRepoStatus.CANCELLED && status != OrderRepoStatus.DELIVERED) {
                    call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Access denied for status $status"))
                    return@put
                }

                // Specialists can update to any valid status for their salon (assuming they have access)
            }

            val result = orderRepository.updateOrderStatus(id, status)
            when (result) {
                is OrderStatusUpdateResult.Success -> {
                    log.info("Order {} status updated to {} by admin {}", id, status, call.getUserId())
                    call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true))
                }
                is OrderStatusUpdateResult.NotFound -> {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Order not found"))
                }
                is OrderStatusUpdateResult.InvalidTransition -> {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Invalid status transition"))
                }
                is OrderStatusUpdateResult.DatabaseError -> {
                    log.error("DB error updating order status: {}", result.cause)
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = "Database error"))
                }
            }
        }


        get("/my-orders") {
            val userId = call.getUserId()
                ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull()?.coerceIn(1, 100) ?: 20
            val status = call.request.queryParameters["status"] // optional filter

            val statusEnum = status?.let {
                try { OrderRepoStatus.valueOf(it.uppercase()) }
                catch (e: IllegalArgumentException) { null }
            }

            val orders = orderRepository.findByUserPaginated(userId, page, pageSize, statusEnum)
            log.info("User {} fetched orders page {}", userId, page)
            call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = orders))
        }


        get("/{id}") {
            val id = call.parameters["id"]
            if (id.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Order ID is required"))
                return@get
            }

            val userId = call.getUserId()
                ?: return@get call.respond(HttpStatusCode.Unauthorized)
            val isAdmin = call.isAdmin()

            val order = if (isAdmin) {
                orderRepository.findOrderById(id)
            } else {
                orderRepository.findOrderById(id, userId)
            }

            if (order != null) {
                call.respond(HttpStatusCode.OK, ApiResponse(success = true, data = order))
            } else {
                call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Order not found"))
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"]
            if (id.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(success = false, error = "Order ID is required"))
                return@delete
            }

            val userId = call.getUserId()
                ?: return@delete call.respond(HttpStatusCode.Unauthorized)
            
            // Check if order exists and belongs to user
            val order = orderRepository.findOrderById(id)
            if (order == null) {
                call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(success = false, error = "Order not found"))
                return@delete
            }

            if (order.userId != userId && !call.isAdmin()) {
                call.respond(HttpStatusCode.Forbidden, ApiResponse<Unit>(success = false, error = "Access denied"))
                return@delete
            }

            val result = orderRepository.deleteOrder(id)
            when (result) {
                is Outcome.Success -> call.respond(HttpStatusCode.OK, ApiResponse<Unit>(success = true))
                is Outcome.Error -> call.respond(HttpStatusCode.InternalServerError, ApiResponse<Unit>(success = false, error = "Failed to delete order"))
                else -> {}
            }
        }
    }
}
