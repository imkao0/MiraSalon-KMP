package iz.mkao.mirasalon.feature.salon.salon.data.network.api

import iz.mkao.mirasalon.core.network.result.NetworkResult
import iz.mkao.mirasalon.core.network.model.dto.SalonDto
import iz.mkao.mirasalon.core.network.model.dto.SalonHomeDto

interface SalonApi {
    suspend fun fetchHome(): NetworkResult<SalonHomeDto>
    suspend fun fetchSalon(id: String): NetworkResult<SalonDto>
}
