package iz.mkao.mirasalon.feature.cart.data.network.api

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.CreateOrderRequest
import iz.mkao.mirasalon.core.network.model.dto.OrderDto
import iz.mkao.mirasalon.core.network.result.apiCall

class KtorOrdersApi(private val httpClient: HttpClient) : OrdersApi {

    override suspend fun fetchMyOrders(
        status: String?,
        page: Int,
        pageSize: Int
    ): Outcome<PagedResponse<OrderDto>> = apiCall {
        httpClient.get(Endpoints.MY_ORDERS) {
            parameter("status", status)
            parameter("page", page)
            parameter("pageSize", pageSize)
        }
    }

    override suspend fun fetchOrderDetail(id: String): Outcome<OrderDto> = apiCall {
        httpClient.get(Endpoints.orderDetail(id))
    }

    override suspend fun checkout(request: CreateOrderRequest): Outcome<OrderDto> = apiCall {
        httpClient.post(Endpoints.CHECKOUT) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun deleteOrder(id: String): Outcome<Unit> = apiCall {
        httpClient.delete(Endpoints.orderDetail(id))
    }

    private object Endpoints {
        const val MY_ORDERS = "/v1/api/orders/my-orders"
        const val CHECKOUT = "/v1/api/orders/checkout"
        fun orderDetail(id: String) = "/v1/api/orders/$id"
    }
}
