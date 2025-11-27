package iz.mkao.mirasalon.core.network.client.admin

import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.CustomerDetailDto
import iz.mkao.mirasalon.core.network.model.dto.CustomerSummaryDto
import iz.mkao.mirasalon.core.network.model.dto.UpdateCustomerRequestDto

interface AdminCustomerApi {
    suspend fun fetchCustomers(
        query: String? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Outcome<PagedResponse<CustomerSummaryDto>>

    suspend fun createCustomer(request: UpdateCustomerRequestDto): Outcome<String>

    suspend fun fetchCustomerDetail(id: String): Outcome<CustomerDetailDto>

    suspend fun updateCustomer(id: String, request: UpdateCustomerRequestDto): Outcome<Unit>

    suspend fun deleteCustomer(id: String): Outcome<Unit>
}
