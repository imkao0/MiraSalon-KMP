package iz.mkao.mirasalon.core.domain.model

data class SalonHome(
    val categories: List<SalonCategory>,
    val specialists: List<Specialist>,
    val promotions: List<Promotion>,
    val isLoggedIn: Boolean,
)
