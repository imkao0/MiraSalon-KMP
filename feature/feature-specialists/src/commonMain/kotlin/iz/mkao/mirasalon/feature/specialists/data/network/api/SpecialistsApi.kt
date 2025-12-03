package iz.mkao.mirasalon.feature.specialists.data.network.api

import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.dto.SpecialistDto

interface SpecialistsApi {
    suspend fun fetchSpecialists(): Outcome<List<SpecialistDto>>
    suspend fun fetchSpecialist(id: String): Outcome<SpecialistDto>
    suspend fun submitReview(specialistId: String, rating: Int, comment: String): Outcome<Unit>
}
