package iz.mkao.mirasalon.core.network.client

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsTokenProvider(private val settings: Settings) : SalonTokenProvider {
    companion object {
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
        private const val USER_ID_KEY = "user_id"
        private const val USER_NAME_KEY = "user_name"
        private const val USER_AVATAR_KEY = "user_avatar"
        private const val USER_FIRST_NAME_KEY = "user_first_name"
        private const val USER_LAST_NAME_KEY = "user_last_name"
        private const val USER_PHONE_KEY = "user_phone"
        private const val USER_ADDRESS_KEY = "user_address"
        private const val USER_GENDER_KEY = "user_gender"
    }

    private val _userIdFlow = MutableStateFlow(settings.getStringOrNull(USER_ID_KEY))

    override fun observeUserId(): Flow<String?> = _userIdFlow.asStateFlow()

    override suspend fun accessToken(): String? {
        return settings.getStringOrNull(ACCESS_TOKEN_KEY)
    }

    override suspend fun refreshToken(): String? {
        return settings.getStringOrNull(REFRESH_TOKEN_KEY)
    }

    override suspend fun userId(): String? {
        return settings.getStringOrNull(USER_ID_KEY)
    }

    override suspend fun userName(): String? {
        return settings.getStringOrNull(USER_NAME_KEY)
    }

    override suspend fun userAddress(): String? {
        return settings.getStringOrNull(USER_ADDRESS_KEY)
    }

    override suspend fun userAvatarUrl(): String? {
        return settings.getStringOrNull(USER_AVATAR_KEY)
    }

    override suspend fun savedEmail(): String? {
        return settings.getStringOrNull("saved_email")
    }

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
            _userIdFlow.value = userId
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
    }

    override suspend fun onAuthenticationExpired() {
        settings.remove(ACCESS_TOKEN_KEY)
        settings.remove(REFRESH_TOKEN_KEY)
        settings.remove(USER_ID_KEY)
        _userIdFlow.value = null
        settings.remove(USER_NAME_KEY)
        settings.remove(USER_AVATAR_KEY)
        settings.remove(USER_FIRST_NAME_KEY)
        settings.remove(USER_LAST_NAME_KEY)
        settings.remove(USER_PHONE_KEY)
        settings.remove(USER_ADDRESS_KEY)
        settings.remove(USER_GENDER_KEY)
    }
}
