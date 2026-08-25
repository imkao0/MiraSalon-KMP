package iz.mkao.mirasalon.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp

/**
 * Bridge for exposing Design System tokens to iOS (SwiftUI).
 *
 * Organized into sub-objects to prevent a flat namespace explosion and improve Swift ergonomics.
 **/
object DesignSystemBridge {

    object Colors {
        // MARK: - Light Theme
        val primaryLight get() = iz.mkao.mirasalon.core.designsystem.theme.primaryLight.toHex()
        val onPrimaryLight get() = iz.mkao.mirasalon.core.designsystem.theme.onPrimaryLight.toHex()
        val primaryContainerLight get() = iz.mkao.mirasalon.core.designsystem.theme.primaryContainerLight.toHex()
        val onPrimaryContainerLight get() = iz.mkao.mirasalon.core.designsystem.theme.onPrimaryContainerLight.toHex()

        val secondaryLight get() = iz.mkao.mirasalon.core.designsystem.theme.secondaryLight.toHex()
        val onSecondaryLight get() = iz.mkao.mirasalon.core.designsystem.theme.onSecondaryLight.toHex()
        val secondaryContainerLight get() = iz.mkao.mirasalon.core.designsystem.theme.secondaryContainerLight.toHex()
        val onSecondaryContainerLight get() = iz.mkao.mirasalon.core.designsystem.theme.onSecondaryContainerLight.toHex()

        val tertiaryLight get() = iz.mkao.mirasalon.core.designsystem.theme.tertiaryLight.toHex()
        val onTertiaryLight get() = iz.mkao.mirasalon.core.designsystem.theme.onTertiaryLight.toHex()
        val tertiaryContainerLight get() = iz.mkao.mirasalon.core.designsystem.theme.tertiaryContainerLight.toHex()
        val onTertiaryContainerLight get() = iz.mkao.mirasalon.core.designsystem.theme.onTertiaryContainerLight.toHex()

        val errorLight get() = iz.mkao.mirasalon.core.designsystem.theme.errorLight.toHex()
        val onErrorLight get() = iz.mkao.mirasalon.core.designsystem.theme.onErrorLight.toHex()
        val errorContainerLight get() = iz.mkao.mirasalon.core.designsystem.theme.errorContainerLight.toHex()
        val onErrorContainerLight get() = iz.mkao.mirasalon.core.designsystem.theme.onErrorContainerLight.toHex()

        val backgroundLight get() = iz.mkao.mirasalon.core.designsystem.theme.backgroundLight.toHex()
        val onBackgroundLight get() = iz.mkao.mirasalon.core.designsystem.theme.onBackgroundLight.toHex()
        val surfaceLight get() = iz.mkao.mirasalon.core.designsystem.theme.surfaceLight.toHex()
        val onSurfaceLight get() = iz.mkao.mirasalon.core.designsystem.theme.onSurfaceLight.toHex()
        val surfaceVariantLight get() = iz.mkao.mirasalon.core.designsystem.theme.surfaceVariantLight.toHex()
        val onSurfaceVariantLight get() = iz.mkao.mirasalon.core.designsystem.theme.onSurfaceVariantLight.toHex()

        // MARK: - Dark Theme
        val primaryDark get() = iz.mkao.mirasalon.core.designsystem.theme.primaryDark.toHex()
        val onPrimaryDark get() = iz.mkao.mirasalon.core.designsystem.theme.onPrimaryDark.toHex()
        val primaryContainerDark get() = iz.mkao.mirasalon.core.designsystem.theme.primaryContainerDark.toHex()
        val onPrimaryContainerDark get() = iz.mkao.mirasalon.core.designsystem.theme.onPrimaryContainerDark.toHex()

        val secondaryDark get() = iz.mkao.mirasalon.core.designsystem.theme.secondaryDark.toHex()
        val onSecondaryDark get() = iz.mkao.mirasalon.core.designsystem.theme.onSecondaryDark.toHex()
        val secondaryContainerDark get() = iz.mkao.mirasalon.core.designsystem.theme.secondaryContainerDark.toHex()
        val onSecondaryContainerDark get() = iz.mkao.mirasalon.core.designsystem.theme.onSecondaryContainerDark.toHex()

        val tertiaryDark get() = iz.mkao.mirasalon.core.designsystem.theme.tertiaryDark.toHex()
        val onTertiaryDark get() = iz.mkao.mirasalon.core.designsystem.theme.onTertiaryDark.toHex()
        val tertiaryContainerDark get() = iz.mkao.mirasalon.core.designsystem.theme.tertiaryContainerDark.toHex()
        val onTertiaryContainerDark get() = iz.mkao.mirasalon.core.designsystem.theme.onTertiaryContainerDark.toHex()

        val errorDark get() = iz.mkao.mirasalon.core.designsystem.theme.errorDark.toHex()
        val onErrorDark get() = iz.mkao.mirasalon.core.designsystem.theme.onErrorDark.toHex()
        val errorContainerDark get() = iz.mkao.mirasalon.core.designsystem.theme.errorContainerDark.toHex()
        val onErrorContainerDark get() = iz.mkao.mirasalon.core.designsystem.theme.onErrorContainerDark.toHex()

        val backgroundDark get() = iz.mkao.mirasalon.core.designsystem.theme.backgroundDark.toHex()
        val onBackgroundDark get() = iz.mkao.mirasalon.core.designsystem.theme.onBackgroundDark.toHex()
        val surfaceDark get() = iz.mkao.mirasalon.core.designsystem.theme.surfaceDark.toHex()
        val onSurfaceDark get() = iz.mkao.mirasalon.core.designsystem.theme.onSurfaceDark.toHex()
        val surfaceVariantDark get() = iz.mkao.mirasalon.core.designsystem.theme.surfaceVariantDark.toHex()
        val onSurfaceVariantDark get() = iz.mkao.mirasalon.core.designsystem.theme.onSurfaceVariantDark.toHex()

        // MARK: - Semantic Tokens
        val success get() = Success.toHex()
        val cancelled get() = Cancelled.toHex()
    }

    object Spacing {
        val none get() = SpacingNone.toBridge()
        val tiny get() = SpacingTiny.toBridge()
        val small get() = SpacingSmall.toBridge()
        val medium get() = SpacingMedium.toBridge()
        val large get() = SpacingLarge.toBridge()
        val extraLarge get() = SpacingExtraLarge.toBridge()
        val default get() = SpacingDefault.toBridge()
        val intermediate get() = SpacingIntermediate.toBridge()
        val section get() = SpacingSection.toBridge()
    }

    object Radii {
        val tiny get() = RadiusTiny.toBridge()
        val extraSmall get() = RadiusExtraSmall.toBridge()
        val small get() = RadiusSmall.toBridge()
        val default get() = RadiusDefault.toBridge()
        val medium get() = RadiusDefault.toBridge()
        val large get() = RadiusLarge.toBridge()
        val extraLarge get() = RadiusExtraLarge.toBridge()
        val profileCard get() = RadiusProfileCard.toBridge()
        val full get() = RadiusFull.toBridge()
        val promo get() = RadiusPromo.toBridge()
        val promoInner get() = RadiusPromoInner.toBridge()
    }

    object Metrics {
        // MARK: - Icon Sizes
        val iconTiny get() = IconSizeTiny.toBridge()
        val iconSmall get() = IconSizeSmall.toBridge()
        val iconMedium get() = IconSizeMedium.toBridge()
        val iconIntermediate get() = IconSizeIntermediate.toBridge()
        val iconLarge get() = IconSizeLarge.toBridge()
        val iconExtraLarge get() = IconSizeExtraLarge.toBridge()

        // MARK: - Component Metrics
        val avatarSize get() = AvatarSize.toBridge()
        val profileAvatarSize get() = ProfileAvatarSize.toBridge()
        val buttonHeight get() = ButtonHeight.toBridge()
        val cardWidthLarge get() = CardWidthLarge.toBridge()
        val cardImageHeight get() = CardImageHeight.toBridge()
        val offerCardWidth get() = OfferCardWidth.toBridge()
        val offerCardHeight get() = OfferCardHeight.toBridge()
        val bannerHeight get() = BannerHeight.toBridge()
        val categorySize get() = CategorySize.toBridge()
        val categoryIconSize get() = CategoryIconSize.toBridge()
        val stepperButtonSize get() = StepperButtonSize.toBridge()
        val starSize get() = StarSize.toBridge()
        val starSizeSmall get() = StarSizeSmall.toBridge()
    }

    // MARK: - Helpers
    private fun Color.toHex(): Long = this.toArgb().toLong()
    private fun Dp.toBridge(): Double = this.value.toDouble()
}
