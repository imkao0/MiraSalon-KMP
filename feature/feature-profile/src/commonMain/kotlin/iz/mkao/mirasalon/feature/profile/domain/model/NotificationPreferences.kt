package iz.mkao.mirasalon.feature.profile.domain.model

data class NotificationPreferences(
    val pushEnabled: Boolean = true,
    val inAppEnabled: Boolean = true,
    val specialistMessagesEnabled: Boolean = true,
    val bookingRemindersEnabled: Boolean = true,
    val marketingEnabled: Boolean = false,
)
