package iz.mkao.mirasalon.feature.chat.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class QuickAccessContact(
    val id: String,
    val name: String,
    val role: String,
    val avatarUrl: String?,
)
