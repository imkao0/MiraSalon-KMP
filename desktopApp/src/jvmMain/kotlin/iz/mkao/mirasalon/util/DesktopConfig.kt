package iz.mkao.mirasalon.util

import java.io.File
import java.util.Properties

object DesktopConfig {
    private val properties = Properties()
    
    init {
        loadDotEnv()
    }

    private fun loadDotEnv() {
        val paths = listOf(
            ".env", 
            "server/.env",
            "../server/.env",
            "../../server/.env",
            "MiraSalon-KM/server/.env"
        )
        
        var envFile: File? = null
        for (path in paths) {
            val file = File(path)
            if (file.exists()) {
                envFile = file
                break
            }
        }

        if (envFile != null && envFile.exists()) {
            envFile.bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    val trimmed = line.trim()
                    if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                        val parts = trimmed.split("=", limit = 2)
                        if (parts.size == 2) {
                            val key = parts[0].trim()
                            val value = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
                            properties.setProperty(key, value)
                        }
                    }
                }
            }
        }
    }

    val streamApiKey: String get() = properties.getProperty("STREAM_API_KEY") ?: System.getenv("STREAM_API_KEY") ?: ""
    val streamApiSecret: String get() = properties.getProperty("STREAM_API_SECRET") ?: System.getenv("STREAM_API_SECRET") ?: ""
    val streamAppId: String get() = properties.getProperty("STREAM_APP_ID") ?: System.getenv("STREAM_APP_ID") ?: ""
}
