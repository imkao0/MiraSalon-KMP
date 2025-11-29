package iz.mkao.mirasalon.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Bridge for exposing Design System tokens to iOS (SwiftUI).
 * Provides color hex values and spacing constants as platform-neutral types.
 */
object DesignSystemBridge {
    // MARK: - Light Theme
    val primaryLightHex: Long = primaryLight.toHex()
    val onPrimaryLightHex: Long = onPrimaryLight.toHex()
    val primaryContainerLightHex: Long = primaryContainerLight.toHex()
    val onPrimaryContainerLightHex: Long = onPrimaryContainerLight.toHex()

    val secondaryLightHex: Long = secondaryLight.toHex()
    val onSecondaryLightHex: Long = onSecondaryLight.toHex()
    val secondaryContainerLightHex: Long = secondaryContainerLight.toHex()
    val onSecondaryContainerLightHex: Long = onSecondaryContainerLight.toHex()

    val tertiaryLightHex: Long = tertiaryLight.toHex()
    val onTertiaryLightHex: Long = onTertiaryLight.toHex()
    val tertiaryContainerLightHex: Long = tertiaryContainerLight.toHex()
    val onTertiaryContainerLightHex: Long = onTertiaryContainerLight.toHex()

    val errorLightHex: Long = errorLight.toHex()
    val onErrorLightHex: Long = onErrorLight.toHex()
    val errorContainerLightHex: Long = errorContainerLight.toHex()
    val onErrorContainerLightHex: Long = onErrorContainerLight.toHex()

    val backgroundLightHex: Long = backgroundLight.toHex()
    val onBackgroundLightHex: Long = onBackgroundLight.toHex()
    val surfaceLightHex: Long = surfaceLight.toHex()
    val onSurfaceLightHex: Long = onSurfaceLight.toHex()
    val surfaceVariantLightHex: Long = surfaceVariantLight.toHex()
    val onSurfaceVariantLightHex: Long = onSurfaceVariantLight.toHex()

    // MARK: - Dark Theme
    val primaryDarkHex: Long = primaryDark.toHex()
    val onPrimaryDarkHex: Long = onPrimaryDark.toHex()
    val primaryContainerDarkHex: Long = primaryContainerDark.toHex()
    val onPrimaryContainerDarkHex: Long = onPrimaryContainerDark.toHex()

    val secondaryDarkHex: Long = secondaryDark.toHex()
    val onSecondaryDarkHex: Long = onSecondaryDark.toHex()
    val secondaryContainerDarkHex: Long = secondaryContainerDark.toHex()
    val onSecondaryContainerDarkHex: Long = onSecondaryContainerDark.toHex()

    val tertiaryDarkHex: Long = tertiaryDark.toHex()
    val onTertiaryDarkHex: Long = onTertiaryDark.toHex()
    val tertiaryContainerDarkHex: Long = tertiaryContainerDark.toHex()
    val onTertiaryContainerDarkHex: Long = onTertiaryContainerDark.toHex()

    val errorDarkHex: Long = errorDark.toHex()
    val onErrorDarkHex: Long = onErrorDark.toHex()
    val errorContainerDarkHex: Long = errorContainerDark.toHex()
    val onErrorContainerDarkHex: Long = onErrorContainerDark.toHex()

    val backgroundDarkHex: Long = backgroundDark.toHex()
    val onBackgroundDarkHex: Long = onBackgroundDark.toHex()
    val surfaceDarkHex: Long = surfaceDark.toHex()
    val onSurfaceDarkHex: Long = onSurfaceDark.toHex()
    val surfaceVariantDarkHex: Long = surfaceVariantDark.toHex()
    val onSurfaceVariantDarkHex: Long = onSurfaceVariantDark.toHex()

    // MARK: - Semantic Tokens
    val successHex: Long = Success.toHex()
    val cancelledHex: Long = Cancelled.toHex()

    // MARK: - Spacing
    val spacingNone: Double = SpacingNone.value.toDouble()
    val spacingTiny: Double = SpacingTiny.value.toDouble()
    val spacingSmall: Double = SpacingSmall.value.toDouble()
    val spacingMedium: Double = SpacingMedium.value.toDouble()
    val spacingLarge: Double = SpacingLarge.value.toDouble()
    val spacingExtraLarge: Double = SpacingExtraLarge.value.toDouble()
    val spacingDefault: Double = SpacingDefault.value.toDouble()
    val spacingIntermediate: Double = SpacingIntermediate.value.toDouble()
    val spacingSection: Double = SpacingSection.value.toDouble()

    // MARK: - Icon Sizes
    val iconSizeTiny: Double = IconSizeTiny.value.toDouble()
    val iconSizeSmall: Double = IconSizeSmall.value.toDouble()
    val iconSizeMedium: Double = IconSizeMedium.value.toDouble()
    val iconSizeIntermediate: Double = IconSizeIntermediate.value.toDouble()
    val iconSizeLarge: Double = IconSizeLarge.value.toDouble()
    val iconSizeExtraLarge: Double = IconSizeExtraLarge.value.toDouble()

    // MARK: - Corner Radii
    val radiusTiny: Double = RadiusTiny.value.toDouble()
    val radiusExtraSmall: Double = RadiusExtraSmall.value.toDouble()
    val radiusSmall: Double = RadiusSmall.value.toDouble()
    val radiusDefault: Double = RadiusDefault.value.toDouble()
    val radiusMedium: Double = RadiusMedium.value.toDouble()
    val radiusLarge: Double = RadiusLarge.value.toDouble()
    val radiusExtraLarge: Double = RadiusExtraLarge.value.toDouble()
    val radiusProfileCard: Double = RadiusProfileCard.value.toDouble()
    val radiusFull: Double = RadiusFull.value.toDouble()
    val radiusPromo: Double = RadiusPromo.value.toDouble()
    val radiusPromoInner: Double = RadiusPromoInner.value.toDouble()

    // MARK: - Component Metrics
    val avatarSize: Double = AvatarSize.value.toDouble()
    val profileAvatarSize: Double = ProfileAvatarSize.value.toDouble()
    val buttonHeight: Double = ButtonHeight.value.toDouble()
    val cardWidthLarge: Double = CardWidthLarge.value.toDouble()
    val cardImageHeight: Double = CardImageHeight.value.toDouble()
    val offerCardWidth: Double = OfferCardWidth.value.toDouble()
    val offerCardHeight: Double = OfferCardHeight.value.toDouble()
    val bannerHeight: Double = BannerHeight.value.toDouble()
    val categorySize: Double = CategorySize.value.toDouble()
    val categoryIconSize: Double = CategoryIconSize.value.toDouble()
    val stepperButtonSize: Double = StepperButtonSize.value.toDouble()
    val starSize: Double = StarSize.value.toDouble()
    val starSizeSmall: Double = StarSizeSmall.value.toDouble()

    private fun Color.toHex(): Long {
        // Use toArgb() to get a consistent 32-bit AARRGGBB value for the Swift bridge
        return this.toArgb().toLong()
    }
}
