package iz.mkao.mirasalon.core.common.util

/**
 * Utility for generating and parsing stable, deterministic chat identifiers.
 */
object ChatUtils {
    private const val CHAT_PREFIX = "chat_"
    private const val SEPARATOR = "__"

    /**
     * Generates a stable chat ID for two users. Sorts IDs to ensure commutativity
     * and uses a stable hash if the resulting ID exceeds length limits.
     */
    fun getDeterministicChatId(userId1: String, userId2: String): String {
        val id1 = userId1.trim()
        val id2 = userId2.trim()
        val sortedIds = listOf(id1, id2).sorted()
        val rawId = sortedIds.joinToString(SEPARATOR)

        return if ((CHAT_PREFIX.length + rawId.length) > 128) {
            val hash = stableHash(rawId)
            CHAT_PREFIX + hash + "_" + rawId.take(10) + rawId.takeLast(10)
        } else {
            CHAT_PREFIX + rawId
        }
    }

    /**
     * Cross-platform stable hash implementation.
     */
    private fun stableHash(input: String): String {
        var hash = 0L
        for (char in input) {
            hash += char.code.toLong()
        }
        return hash.toString(16)
    }

    /**
     * Extracts participant IDs from a deterministic chat ID. Returns null if the format is invalid.
     */
    fun parseParticipantIds(chatId: String): List<String>? {
        if (!chatId.startsWith(CHAT_PREFIX)) return null
        val body = chatId.removePrefix(CHAT_PREFIX)
        if (body.isBlank() || !body.contains(SEPARATOR)) return null
        val parts = body.split(SEPARATOR).filter { it.isNotBlank() }
        return if (parts.size == 2) parts else null
    }

    /**
     * Validates if the given [chatId] follows the deterministic naming convention.
     */
    fun isDeterministicChatId(chatId: String): Boolean = parseParticipantIds(chatId) != null
}
