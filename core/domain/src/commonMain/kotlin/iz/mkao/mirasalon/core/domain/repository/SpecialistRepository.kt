package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.Specialist
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import kotlinx.coroutines.flow.Flow

interface SpecialistRepository {
    fun observeSpecialists(): Flow<Outcome<List<Specialist>>>
    fun observeSpecialist(id: String): Flow<Outcome<Specialist>>
    suspend fun getSpecialists(): Outcome<List<Specialist>>
    suspend fun getSpecialist(id: String): Outcome<Specialist>
    suspend fun refresh()
    suspend fun submitReview(specialistId: String, rating: Int, comment: String): Outcome<Unit>
}
