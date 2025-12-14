package iz.mkao.mirasalon.server.util

import java.util.concurrent.ConcurrentHashMap

object TokenVersionCache {
    private val cache = ConcurrentHashMap<String, Int>()

    fun get(userId: String): Int? = cache[userId]

    fun put(userId: String, version: Int) {
        cache[userId] = version
    }

    fun invalidate(userId: String) {
        cache.remove(userId)
    }

    fun clear() {
        cache.clear()
    }
}
