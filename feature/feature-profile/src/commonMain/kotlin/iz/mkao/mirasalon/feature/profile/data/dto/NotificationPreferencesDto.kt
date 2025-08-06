package iz.mkao.mirasalon.feature.profile.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class NotificationPreferencesDto(
    val pushEnabled: Boolean = true,
    val specialistMessagesEnabled: Boolean = true,
    val bookingRemindersEnabled: Boolean = true,
    val marketingEnabled: Boolean = false,
)
