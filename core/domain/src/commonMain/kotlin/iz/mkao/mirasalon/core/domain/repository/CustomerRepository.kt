package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.CustomerDetail
import iz.mkao.mirasalon.core.domain.model.CustomerSummary
import iz.mkao.mirasalon.core.domain.outcome.Outcome

interface CustomerRepository {
    suspend fun getAll(query: String? = null): Outcome<List<CustomerSummary>>
    suspend fun getDetail(id: String): Outcome<CustomerDetail>
    suspend fun create(name: String, email: String): Outcome<Unit>
    suspend fun update(id: String, name: String, email: String, avatarUrl: String? = null): Outcome<Unit>
    suspend fun delete(id: String): Outcome<Unit>
}
