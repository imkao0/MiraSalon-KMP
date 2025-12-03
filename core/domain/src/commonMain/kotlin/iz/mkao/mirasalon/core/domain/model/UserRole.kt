package iz.mkao.mirasalon.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents the roles available in the MiraSalon system.
 */
@Serializable
public enum class UserRole {
    ADMIN,
    SPECIALIST,
    USER;

    public companion object {
        public fun fromString(value: String?): UserRole = when (value?.uppercase()) {
            "ADMIN" -> ADMIN
            "SPECIALIST" -> SPECIALIST
            else -> USER
        }
    }

    /**
     * Returns the set of permissions associated with this role.
     */
    public val permissions: Set<Permission>
        get() = when (this) {
            ADMIN -> Permission.entries.toSet()
            SPECIALIST -> setOf(
                Permission.VIEW_CALENDAR,
                Permission.MANAGE_OWN_SCHEDULE,
                Permission.VIEW_CUSTOMERS,
                Permission.CHAT_WITH_CUSTOMERS,
                Permission.MANAGE_PRODUCTS
            )
            USER -> setOf(
                Permission.VIEW_CATALOG,
                Permission.BOOK_APPOINTMENT,
                Permission.VIEW_OWN_PROFILE
            )
        }
}

/**
 * Granular permissions for the system.
 */
@Serializable
public enum class Permission {
    // Admin Only
    MANAGE_STAFF,
    MANAGE_SALON_SETTINGS,
    VIEW_ANALYTICS,
    MANAGE_PROMOTIONS,

    // Staff / Admin
    VIEW_CALENDAR,
    MANAGE_OWN_SCHEDULE,
    CHAT_WITH_CUSTOMERS,
    MANAGE_PRODUCTS,

    // User / Staff / Admin
    VIEW_CATALOG,
    BOOK_APPOINTMENT,
    VIEW_OWN_PROFILE,
    VIEW_CUSTOMERS
}
