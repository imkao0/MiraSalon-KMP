package iz.mkao.mirasalon.feature.profile.data.mapper

import iz.mkao.mirasalon.core.domain.model.PaymentMethod
import iz.mkao.mirasalon.core.domain.model.PaymentMethodType
import iz.mkao.mirasalon.core.network.config.ApiEndpoints
import iz.mkao.mirasalon.feature.profile.data.dto.AddressDto
import iz.mkao.mirasalon.feature.profile.data.dto.NotificationPreferencesDto
import iz.mkao.mirasalon.feature.profile.data.dto.PaymentMethodDto
import iz.mkao.mirasalon.feature.profile.data.dto.UserProfileDto
import iz.mkao.mirasalon.feature.profile.domain.model.Address
import iz.mkao.mirasalon.feature.profile.domain.model.AddressLabel
import iz.mkao.mirasalon.feature.profile.domain.model.Gender
import iz.mkao.mirasalon.feature.profile.domain.model.NotificationPreferences
import iz.mkao.mirasalon.feature.profile.domain.model.UserProfile

fun UserProfileDto.toDomain(): UserProfile = UserProfile(
    id = id,
    fullName = fullName,
    email = email,
    phoneNumber = phoneNumber,
    avatarUrl = ApiEndpoints.resolveImageUrl(avatarUrl),
    gender = gender?.let { raw -> runCatching { Gender.valueOf(raw) }.getOrNull() },
    dateOfBirth = dateOfBirth,
    allergies = allergies,
    memberSinceEpochSeconds = memberSinceEpochSeconds,
)

fun AddressDto.toDomain(): Address = Address(
    id = id,
    firstName = firstName,
    lastName = lastName,
    label = runCatching { AddressLabel.valueOf(label) }.getOrDefault(AddressLabel.OTHER),
    phoneNumber = phoneNumber,
    streetAddress = streetAddress,
    number = number,
    city = city,
    state = state,
    isDefault = isDefault,
)

fun Address.toDto(): AddressDto = AddressDto(
    id = id,
    firstName = firstName,
    lastName = lastName,
    label = label.name,
    phoneNumber = phoneNumber,
    streetAddress = streetAddress,
    number = number,
    city = city,
    state = state,
    isDefault = isDefault,
)

fun PaymentMethodDto.toDomain(): PaymentMethod =
    PaymentMethod(
        id = id,
        type = runCatching { PaymentMethodType.valueOf(type) }
            .getOrDefault(PaymentMethodType.CASH),
        label = label,
        last4Digits = last4Digits,
        isDefault = isDefault,
    )

fun PaymentMethod.toDto(): PaymentMethodDto =
    PaymentMethodDto(
        id = id,
        type = type.name,
        label = label,
        last4Digits = last4Digits,
        isDefault = isDefault,
    )

fun NotificationPreferencesDto.toDomain(): NotificationPreferences =
    NotificationPreferences(
        pushEnabled = pushEnabled,
        specialistMessagesEnabled = specialistMessagesEnabled,
        bookingRemindersEnabled = bookingRemindersEnabled,
        marketingEnabled = marketingEnabled,
    )

fun NotificationPreferences.toDto(): NotificationPreferencesDto =
    NotificationPreferencesDto(
        pushEnabled = pushEnabled,
        specialistMessagesEnabled = specialistMessagesEnabled,
        bookingRemindersEnabled = bookingRemindersEnabled,
        marketingEnabled = marketingEnabled,
    )
