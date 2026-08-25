package iz.mkao.mirasalon.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
sealed interface BottomNavKey : Route {
    val icon: ImageVector
    val label: String

    @Serializable
    @CommonParcelize
    class Home : BottomNavKey {
        @Transient override val icon: ImageVector = Icons.Outlined.Home
        @Transient override val label: String = "Home"
        override fun equals(other: Any?): Boolean = other is Home
        override fun hashCode(): Int = Home::class.hashCode()
    }

    @Serializable
    @CommonParcelize
    class Discover : BottomNavKey {
        @Transient override val icon: ImageVector = Icons.Outlined.LocationOn
        @Transient override val label: String = "Discover"
        override fun equals(other: Any?): Boolean = other is Discover
        override fun hashCode(): Int = Discover::class.hashCode()
    }

    @Serializable
    @CommonParcelize
    class Booking : BottomNavKey {
        @Transient override val icon: ImageVector = Icons.Outlined.DateRange
        @Transient override val label: String = "Booking"
        override fun equals(other: Any?): Boolean = other is Booking
        override fun hashCode(): Int = Booking::class.hashCode()
    }

    @Serializable
    @CommonParcelize
    class Cart : BottomNavKey {
        @Transient override val icon: ImageVector = Icons.Outlined.ShoppingCart
        @Transient override val label: String = "Cart"
        override fun equals(other: Any?): Boolean = other is Cart
        override fun hashCode(): Int = Cart::class.hashCode()
    }

    @Serializable
    @CommonParcelize
    class Chat : BottomNavKey {
        @Transient override val icon: ImageVector = Icons.AutoMirrored.Outlined.Chat
        @Transient override val label: String = "Chat"
        override fun equals(other: Any?): Boolean = other is Chat
        override fun hashCode(): Int = Chat::class.hashCode()
    }

    @Serializable
    @CommonParcelize
    class Profile : BottomNavKey {
        @Transient override val icon: ImageVector = Icons.Outlined.Person
        @Transient override val label: String = "Profile"
        override fun equals(other: Any?): Boolean = other is Profile
        override fun hashCode(): Int = Profile::class.hashCode()
    }

    companion object {
        val items = listOf(Home(), Discover(), Booking(), Chat(), Profile())
    }
}
