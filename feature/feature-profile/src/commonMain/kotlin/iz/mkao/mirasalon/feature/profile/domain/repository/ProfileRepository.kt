package iz.mkao.mirasalon.feature.profile.domain.repository

import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.feature.profile.domain.model.ProfileUpdate
import iz.mkao.mirasalon.feature.profile.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(): Flow<Outcome<UserProfile>>
    suspend fun getProfile(): Outcome<UserProfile>
    suspend fun updateProfile(patch: ProfileUpdate): Outcome<UserProfile>
    suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): Outcome<String>
}
