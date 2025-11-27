package iz.mkao.mirasalon.data.repository

import iz.mkao.mirasalon.core.domain.model.CustomerDetail
import iz.mkao.mirasalon.core.domain.model.CustomerSummary
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.domain.repository.CustomerRepository
import iz.mkao.mirasalon.core.network.client.admin.AdminCustomerApi
import iz.mkao.mirasalon.core.network.mapper.admin.toDomain
import iz.mkao.mirasalon.core.network.model.dto.UpdateCustomerRequestDto

class KtorCustomerRepository(
    private val api: AdminCustomerApi
) : CustomerRepository {

    override suspend fun getAll(query: String?): Outcome<List<CustomerSummary>> {
        return api.fetchCustomers(query = query).map { pagedResponse ->
            pagedResponse.items.map { it.toDomain() }
        }
    }

    override suspend fun getDetail(id: String): Outcome<CustomerDetail> {
        return api.fetchCustomerDetail(id).map { it.toDomain() }
    }

    override suspend fun create(name: String, email: String): Outcome<Unit> {
        val request = UpdateCustomerRequestDto(name = name, email = email)
        return api.createCustomer(request).map { }
    }

    override suspend fun update(id: String, name: String, email: String, avatarUrl: String?): Outcome<Unit> {
        val request = UpdateCustomerRequestDto(name = name, email = email, avatarUrl = avatarUrl)
        return api.updateCustomer(id, request)
    }

    override suspend fun delete(id: String): Outcome<Unit> {
        return api.deleteCustomer(id)
    }
}
