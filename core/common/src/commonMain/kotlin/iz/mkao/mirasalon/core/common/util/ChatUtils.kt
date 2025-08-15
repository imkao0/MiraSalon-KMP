package iz.mkao.mirasalon.core.common.util

object ChatUtils {
    private const val CHAT_PREFIX = "chat_"
    private const val SEPARATOR = "__"

    /**
     * Generates a deterministic chat ID for a pair of users.
     * This ensures both the customer and the specialist can join the same
     * real-time partition without needing a server-generated ID.
     *
     * The ID is stable and collision-safe: it embeds both participant IDs
     * (sorted, so the order does not matter) rather than a lossy `hashCode()`.
     * Because both participant IDs are preserved in the ID, the desktop app can
     * resolve the actual chat participants back from the chat ID (see
     * [parseParticipantIds]), which is what keeps the customer card in sync with
     * the real chat participant instead of an unrelated user.
     */
    fun getDeterministicChatId(userId1: String, userId2: String): String {
        val id1 = userId1.trim()
        val id2 = userId2.trim()
        val sortedIds = listOf(id1, id2).sorted()
        val rawId = sortedIds.joinToString(SEPARATOR)
        
        // If the combined ID is too long for Stream (max 64), 
        // we use a hash of the sorted pair to keep it unique but short.
        return if ((CHAT_PREFIX.length + rawId.length) > 64) {
            CHAT_PREFIX + rawId.hashCode().toString().replace("-", "n") + "_" + 
                rawId.take(10) + rawId.takeLast(10)
        } else {
            CHAT_PREFIX + rawId
        }
    }

    /**
     * Extracts the two participant IDs from a deterministic chat ID produced by
     * [getDeterministicChatId]. Returns `null` if [chatId] is not a deterministic
     * chat ID (e.g. a legacy `chat_<hash>` id or an arbitrary channel id).
     *
     * The returned list contains exactly the two participant IDs (sorted).
     */
    fun parseParticipantIds(chatId: String): List<String>? {
        if (!chatId.startsWith(CHAT_PREFIX)) return null
        val body = chatId.removePrefix(CHAT_PREFIX)
        if (body.isBlank() || !body.contains(SEPARATOR)) return null
        val parts = body.split(SEPARATOR).filter { it.isNotBlank() }
        return if (parts.size == 2) parts else null
    }

    /**
     * Returns true if [chatId] looks like a deterministic chat ID produced by
     * [getDeterministicChatId].
     */
    fun isDeterministicChatId(chatId: String): Boolean = parseParticipantIds(chatId) != null
}
