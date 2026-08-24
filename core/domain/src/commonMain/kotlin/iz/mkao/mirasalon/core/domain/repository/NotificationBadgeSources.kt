package iz.mkao.mirasalon.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Source for the unread-messages part of the profile Notifications badge.
 * Implemented by the chat feature and provided via DI.
 */
interface UnreadMessagesSource {
    fun observeUnreadMessagesCount(): Flow<Int>
}

/**
 * Source for the appointment-reminders part of the profile Notifications
 * badge. Implemented by the appointments feature and provided via DI.
 */
interface UpcomingAppointmentsSource {
    fun observeUpcomingAppointmentsCount(): Flow<Int>
}
