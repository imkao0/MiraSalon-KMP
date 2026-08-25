package iz.mkao.mirasalon.core.network.client.admin

import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.CustomerDetailDto
import iz.mkao.mirasalon.core.network.model.dto.CustomerSummaryDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateCustomerRequestDto
import iz.mkao.mirasalon.core.network.result.apiCall

class KtorAdminCustomerApi(private val httpClient: HttpClient) : AdminCustomerApi {

    override suspend fun fetchCustomers(
        query: String?,
        page: Int?,
        pageSize: Int?
    ): Outcome<PagedResponse<CustomerSummaryDto>> = apiCall<PagedResponse<CustomerSummaryDto>> {
        httpClient.get(Endpoints.CUSTOMERS) {
            parameter("query", query)
            parameter("page", page)
            parameter("pageSize", pageSize)
        }
    }

    override suspend fun createCustomer(request: UpdateCustomerRequestDto): Outcome<String> = apiCall<String> {
        httpClient.post(Endpoints.CUSTOMERS) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun fetchCustomerDetail(id: String): Outcome<CustomerDetailDto> = apiCall<CustomerDetailDto> {
        httpClient.get(Endpoints.customer(id))
    }

    override suspend fun updateCustomer(id: String, request: UpdateCustomerRequestDto): Outcome<Unit> = apiCall<Unit> {
        httpClient.put(Endpoints.customer(id)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    override suspend fun deleteCustomer(id: String): Outcome<Unit> = apiCall<Unit> {
        httpClient.delete(Endpoints.customer(id))
    }

    private object Endpoints {
        const val CUSTOMERS = "/v1/api/customers"
        fun customer(id: String) = "/v1/api/customers/$id"
    }
}
