package iz.mkao.mirasalon.core.network.client.admin

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.OrderDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateOrderStatusRequest
import iz.mkao.mirasalon.core.network.result.apiCall

class KtorAdminOrdersApi(private val httpClient: HttpClient) : AdminOrdersApi {

    override suspend fun fetchOrders(
        status: String?,
        query: String?,
        dateFrom: Long?,
        dateTo: Long?,
        page: Int,
        pageSize: Int
    ): Outcome<PagedResponse<OrderDto>> = apiCall {
        httpClient.get(Endpoints.ORDERS_ALL) {
            parameter("status", status)
            parameter("query", query)
            parameter("dateFrom", dateFrom)
            parameter("dateTo", dateTo)
            parameter("page", page)
            parameter("pageSize", pageSize)
        }
    }

    override suspend fun updateOrderStatus(id: String, status: String): Outcome<Unit> = apiCall {
        httpClient.put(Endpoints.orderStatus(id)) {
            contentType(ContentType.Application.Json)
            setBody(UpdateOrderStatusRequest(status))
        }
    }

    override suspend fun deleteOrder(id: String): Outcome<Unit> = apiCall {
        httpClient.delete(Endpoints.orderDetail(id))
    }

    private object Endpoints {
        const val ORDERS_ALL = "/v1/api/orders/all"
        fun orderStatus(id: String) = "/v1/api/orders/$id/status"
        fun orderDetail(id: String) = "/v1/api/orders/$id"
    }
}
