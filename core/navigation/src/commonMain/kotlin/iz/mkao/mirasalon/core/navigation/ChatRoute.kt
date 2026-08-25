package iz.mkao.mirasalon.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface ChatRoute : Route {
    @Serializable
    @CommonParcelize
    data object ChatList : ChatRoute

    @Serializable
    @CommonParcelize
    data class ChatDetail(
        val conversationId: String,
        val participantName: String? = null,
        val participantAvatarUrl: String? = null,
        val participantId: String? = null
    ) : ChatRoute
}
