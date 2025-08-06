package iz.mkao.mirasalon.feature.profile.domain.repository

import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.feature.profile.domain.model.Address
import kotlinx.coroutines.flow.Flow

interface AddressRepository {
    fun observeAddresses(): Flow<List<Address>>
    suspend fun addAddress(address: Address): Outcome<Address>
    suspend fun updateAddress(address: Address): Outcome<Address>
    suspend fun deleteAddress(id: String): Outcome<Unit>
    suspend fun setDefault(id: String): Outcome<Unit>
}
