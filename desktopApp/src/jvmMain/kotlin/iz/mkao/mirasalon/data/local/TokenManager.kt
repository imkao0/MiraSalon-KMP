package iz.mkao.mirasalon.data.local

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSession(
    val token: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val gender: String? = null,
    val avatarUrl: String? = null
)

class TokenManager(private val settings: Settings) : SalonTokenProvider {

    companion object {
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
        private const val USER_ID_KEY = "user_id"
        private const val USER_NAME_KEY = "user_name"
        private const val USER_FIRST_NAME_KEY = "user_first_name"
        private const val USER_LAST_NAME_KEY = "user_last_name"
        private const val USER_PHONE_KEY = "user_phone"
        private const val USER_ADDRESS_KEY = "user_address"
        private const val USER_GENDER_KEY = "user_gender"
        private const val USER_AVATAR_KEY = "user_avatar"
    }

    private val _session = MutableStateFlow(
        UserSession(
            token = settings.getStringOrNull(ACCESS_TOKEN_KEY),
            firstName = settings.getStringOrNull(USER_FIRST_NAME_KEY),
            lastName = settings.getStringOrNull(USER_LAST_NAME_KEY),
            name = settings.getStringOrNull(USER_NAME_KEY),
            phone = settings.getStringOrNull(USER_PHONE_KEY),
            address = settings.getStringOrNull(USER_ADDRESS_KEY),
            gender = settings.getStringOrNull(USER_GENDER_KEY),
            avatarUrl = settings.getStringOrNull(USER_AVATAR_KEY)
        )
    )
    val session: StateFlow<UserSession> = _session.asStateFlow()

    override suspend fun accessToken(): String? = settings.getStringOrNull(ACCESS_TOKEN_KEY)

    override suspend fun refreshToken(): String? = settings.getStringOrNull(REFRESH_TOKEN_KEY)

    override suspend fun userId(): String? = settings.getStringOrNull(USER_ID_KEY)

    override suspend fun userName(): String? = settings.getStringOrNull(USER_NAME_KEY)

    override suspend fun userAddress(): String? = settings.getStringOrNull(USER_ADDRESS_KEY)

    override suspend fun userAvatarUrl(): String? = settings.getStringOrNull(USER_AVATAR_KEY)

    override suspend fun savedEmail(): String? = settings.getStringOrNull("saved_email")

    override suspend fun saveEmail(email: String?) {
        if (email != null) {
            settings["saved_email"] = email
        } else {
            settings.remove("saved_email")
        }
    }

    override suspend fun onTokensRefreshed(
        accessToken: String,
        refreshToken: String,
        userId: String?,
        userName: String?,
        userAvatarUrl: String?,
        firstName: String?,
        lastName: String?,
        phone: String?,
        address: String?,
        gender: String?
    ) {
        settings[ACCESS_TOKEN_KEY] = accessToken
        settings[REFRESH_TOKEN_KEY] = refreshToken
        if (userId != null) {
            settings[USER_ID_KEY] = userId
        }
        if (userName != null) {
            settings[USER_NAME_KEY] = userName
        }
        if (userAvatarUrl != null) {
            settings[USER_AVATAR_KEY] = userAvatarUrl
        }
        if (firstName != null) {
            settings[USER_FIRST_NAME_KEY] = firstName
        }
        if (lastName != null) {
            settings[USER_LAST_NAME_KEY] = lastName
        }
        if (phone != null) {
            settings[USER_PHONE_KEY] = phone
        }
        if (address != null) {
            settings[USER_ADDRESS_KEY] = address
        }
        if (gender != null) {
            settings[USER_GENDER_KEY] = gender
        }
        
        _session.value = _session.value.copy(
            token = accessToken,
            name = userName ?: _session.value.name,
            avatarUrl = userAvatarUrl ?: _session.value.avatarUrl,
            firstName = firstName ?: _session.value.firstName,
            lastName = lastName ?: _session.value.lastName,
            phone = phone ?: _session.value.phone,
            address = address ?: _session.value.address,
            gender = gender ?: _session.value.gender
        )
    }

    override suspend fun onAuthenticationExpired() {
        clearToken()
    }

    fun clearToken() {
        settings.remove(ACCESS_TOKEN_KEY)
        settings.remove(REFRESH_TOKEN_KEY)
        settings.remove(USER_ID_KEY)
        settings.remove(USER_NAME_KEY)
        settings.remove(USER_AVATAR_KEY)
        settings.remove(USER_FIRST_NAME_KEY)
        settings.remove(USER_LAST_NAME_KEY)
        settings.remove(USER_PHONE_KEY)
        settings.remove(USER_ADDRESS_KEY)
        settings.remove(USER_GENDER_KEY)
        _session.value = UserSession()
    }

    fun updateProfile(
        firstName: String?,
        lastName: String?,
        name: String?,
        phone: String?,
        address: String?,
        gender: String?,
        avatar: String?
    ) {
        firstName?.let { settings[USER_FIRST_NAME_KEY] = it }
        lastName?.let { settings[USER_LAST_NAME_KEY] = it }
        name?.let { settings[USER_NAME_KEY] = it }
        phone?.let { settings[USER_PHONE_KEY] = it }
        address?.let { settings[USER_ADDRESS_KEY] = it }
        gender?.let { settings[USER_GENDER_KEY] = it }
        avatar?.let { settings[USER_AVATAR_KEY] = it }
        
        _session.value = _session.value.copy(
            firstName = firstName ?: _session.value.firstName,
            lastName = lastName ?: _session.value.lastName,
            name = name ?: _session.value.name,
            phone = phone ?: _session.value.phone,
            address = address ?: _session.value.address,
            gender = gender ?: _session.value.gender,
            avatarUrl = avatar ?: _session.value.avatarUrl
        )
    }

    fun getAuthHeader(): String {
        return "Bearer ${_session.value.token.orEmpty()}"
    }
}
