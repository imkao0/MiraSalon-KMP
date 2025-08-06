package iz.mkao.mirasalon.feature.profile.data.repository

import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.result.NetworkResult
import iz.mkao.mirasalon.feature.profile.domain.model.Address
import iz.mkao.mirasalon.feature.profile.domain.repository.AddressRepository
import iz.mkao.mirasalon.feature.profile.data.mapper.toDomain
import iz.mkao.mirasalon.feature.profile.data.mapper.toDto
import iz.mkao.mirasalon.feature.profile.data.mapper.toFailure
import iz.mkao.mirasalon.feature.profile.data.remote.ProfileApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddressRepositoryImpl(
    private val api: ProfileApi,
    private val scope: CoroutineScope
) : AddressRepository {

    private val _addresses = MutableStateFlow<List<Address>>(emptyList())
    private val addresses: StateFlow<List<Address>> = _addresses.asStateFlow()

    init {
        refresh()
    }

    override fun observeAddresses(): StateFlow<List<Address>> = addresses

    override suspend fun addAddress(address: Address): Outcome<Address> =
        when (val result = api.addAddress(address.toDto())) {
            is NetworkResult.Success -> {
                refresh()
                Outcome.Success(result.data.toDomain())
            }
            is NetworkResult.Error -> Outcome.Error(result.error.toFailure())
        }

    override suspend fun updateAddress(address: Address): Outcome<Address> =
        when (val result = api.updateAddress(address.toDto())) {
            is NetworkResult.Success -> {
                refresh()
                Outcome.Success(result.data.toDomain())
            }
            is NetworkResult.Error -> Outcome.Error(result.error.toFailure())
        }

    override suspend fun deleteAddress(id: String): Outcome<Unit> =
        when (val result = api.deleteAddress(id)) {
            is NetworkResult.Success -> {
                refresh()
                Outcome.Success(Unit)
            }
            is NetworkResult.Error -> Outcome.Error(result.error.toFailure())
        }

    override suspend fun setDefault(id: String): Outcome<Unit> =
        when (val result = api.setDefaultAddress(id)) {
            is NetworkResult.Success -> {
                refresh()
                Outcome.Success(Unit)
            }
            is NetworkResult.Error -> Outcome.Error(result.error.toFailure())
        }

    private fun refresh() {
        scope.launch {
            when (val result = api.fetchAddresses()) {
                is NetworkResult.Success -> _addresses.value = result.data.map { it.toDomain() }
                is NetworkResult.Error -> Unit
            }
        }
    }
}
