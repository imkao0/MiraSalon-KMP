package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.Salon
import iz.mkao.mirasalon.core.domain.model.SalonHome
import iz.mkao.mirasalon.core.domain.outcome.Outcome

interface SalonRepository {
    suspend fun getHome(): Outcome<SalonHome>
    suspend fun getSalon(id: String): Outcome<Salon>
}
