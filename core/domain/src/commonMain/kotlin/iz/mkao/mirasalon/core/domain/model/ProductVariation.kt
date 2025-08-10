package iz.mkao.mirasalon.core.domain.model

enum class ProductVariation(val displayName: String) {
    WOMEN("Women"),
    MEN("Men"),
    OTHERS("Others");

    companion object {
        fun fromString(value: String?): ProductVariation? {
            return entries.find { it.name.equals(value, ignoreCase = true) }
        }
    }
}
