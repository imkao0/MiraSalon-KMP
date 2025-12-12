package iz.mkao.mirasalon.server.util

import java.io.File

object ServerEnvironment {
    private val envMap: Map<String, String> by lazy {
        val map = mutableMapOf<String, String>()
        val dotenvFile = File(".env")
        if (dotenvFile.exists()) {
            dotenvFile.readLines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    val parts = trimmed.split("=", limit = 2)
                    if (parts.size == 2) {
                        map[parts[0].trim()] = parts[1].trim().removeSurrounding("\"", "\"").removeSurrounding("'", "'")
                    }
                }
            }
        }
        map
    }

    fun environment(): String = orDefault("APP_ENV", "development")

    fun secret(key: String): String {
        return secretOrNull(key) ?: throw IllegalStateException("Missing required environment variable: $key")
    }

    fun secretOrNull(key: String): String? {
        return envMap[key] ?: System.getenv(key)
    }

    fun orDefault(key: String, default: String): String {
        return secretOrNull(key) ?: default
    }

    fun optional(key: String): String? {
        return secretOrNull(key)
    }
}
