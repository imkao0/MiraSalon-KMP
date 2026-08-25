package iz.mkao.mirasalon.core.designsystem.utils

fun showToast(message: String) {
    multiplatform.network.cmptoast.showToast(message = message)
}
