package iz.mkao.mirasalon.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// MARK: - Brand Palette (Coral Red & Gold)

private object BrandColors {
    // Primary Seed & Shades (Coral Red)
    val Primary50 = Color(0xFFFFEBEB)
    val Primary100 = Color(0xFFFFC4C4)
    val Primary200 = Color(0xFFFF9A9A)
    val Primary300 = Color(0xFFFF7070)
    val Primary400 = Color(0xFFF06A6A)
    val Primary500 = Color(0xFFE63946)
    val Primary600 = Color(0xFFD62839)
    val Primary700 = Color(0xFFC1121F)
    val Primary800 = Color(0xFF9D0208)
    val Primary900 = Color(0xFF6B0F1A)
    val Primary950 = Color(0xFF370617)

    // Accent Seed & Shades (Imperial Gold & Champagne)
    val Amber50 = Color(0xFFFFFDE7)
    val Amber100 = Color(0xFFFFF9C4)
    val Amber200 = Color(0xFFFFF59D)
    val Amber300 = Color(0xFFFFEE58)
    val Amber400 = Color(0xFFFFEB3B)
    val Amber500 = Color(0xFFFBC02D)
    val Amber600 = Color(0xFFF57F17)
    val Amber700 = Color(0xFFD4AF37)
    val Amber800 = Color(0xFFA67C00)
    val Amber900 = Color(0xFF7A5C00)
    val Amber950 = Color(0xFF423200)

    // Neutral Seed & Shades (Cool Slate & Jade Neutral)
    val Slate50 = Color(0xFFF7FAFA)
    val Slate100 = Color(0xFFEFF4F4)
    val Slate200 = Color(0xFFE2ECEB)
    val Slate300 = Color(0xFFCBDCDA)
    val Slate400 = Color(0xFF9FB9B5)
    val Slate500 = Color(0xFF71918B)
    val Slate600 = Color(0xFF52706A)
    val Slate700 = Color(0xFF3F5551)
    val Slate800 = Color(0xFF283835)
    val Slate900 = Color(0xFF1B2624)
    val Slate950 = Color(0xFF0E1413)

    val Coral = Color(0xFFF06A6A)
    val Yellow = Color(0xFFFFD54F)
    val Green = Color(0xFF4CAF50)
    val Orange = Color(0xFFFFB74D)
}

// MARK: - Semantic Tokens

val Success = BrandColors.Green
val Cancelled = BrandColors.Coral

val MiraCoral = BrandColors.Coral
val MiraYellow = BrandColors.Yellow
val MiraGreen = BrandColors.Green
val MiraOrange = BrandColors.Orange

// MARK: - Light Theme

val primaryLight = BrandColors.Coral
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = BrandColors.Primary50
val onPrimaryContainerLight = BrandColors.Coral

val secondaryLight = BrandColors.Amber700
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = BrandColors.Amber100
val onSecondaryContainerLight = BrandColors.Amber950

val tertiaryLight = BrandColors.Amber600
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = BrandColors.Amber200
val onTertiaryContainerLight = BrandColors.Amber950

val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF410002)

val backgroundLight = Color(0xFFFFFFFF)
val onBackgroundLight = BrandColors.Slate900

val surfaceLight = Color(0xFFFFFFFF)
val onSurfaceLight = BrandColors.Slate900
val surfaceVariantLight = BrandColors.Slate100
val onSurfaceVariantLight = BrandColors.Slate600

val outlineLight = BrandColors.Slate400
val outlineVariantLight = BrandColors.Slate200

val scrimLight = Color(0xFF000000)
val inverseSurfaceLight = BrandColors.Slate800
val inverseOnSurfaceLight = BrandColors.Slate50
val inversePrimaryLight = BrandColors.Primary300

val surfaceDimLight = BrandColors.Slate200
val surfaceBrightLight = Color(0xFFFFFFFF)
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = BrandColors.Slate50
val surfaceContainerLight = BrandColors.Slate100
val surfaceContainerHighLight = BrandColors.Slate200
val surfaceContainerHighestLight = BrandColors.Slate300

// MARK: - Dark Theme

val primaryDark = BrandColors.Primary300
val onPrimaryDark = BrandColors.Primary950
val primaryContainerDark = BrandColors.Primary900
val onPrimaryContainerDark = BrandColors.Primary100

val secondaryDark = BrandColors.Amber300
val onSecondaryDark = BrandColors.Amber950
val secondaryContainerDark = BrandColors.Amber800
val onSecondaryContainerDark = BrandColors.Amber100

val tertiaryDark = BrandColors.Amber400
val onTertiaryDark = BrandColors.Amber950
val tertiaryContainerDark = BrandColors.Amber700
val onTertiaryContainerDark = BrandColors.Amber100

val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)

val backgroundDark = Color(0xFF000000)
val onBackgroundDark = BrandColors.Slate100

val surfaceDark = Color(0xFF000000)
val onSurfaceDark = BrandColors.Slate100
val surfaceVariantDark = Color(0xFF0C1615)
val onSurfaceVariantDark = BrandColors.Slate300

val outlineDark = Color(0xFF1E2E2C)
val outlineVariantDark = Color(0xFF121E1C)

val scrimDark = Color(0xFF000000)
val inverseSurfaceDark = BrandColors.Slate100
val inverseOnSurfaceDark = BrandColors.Slate900
val inversePrimaryDark = BrandColors.Primary700

val surfaceDimDark = Color(0xFF000000)
val surfaceBrightDark = Color(0xFF121E1C)
val surfaceContainerLowestDark = Color(0xFF000000)
val surfaceContainerLowDark = Color(0xFF080F0E)
val surfaceContainerDark = Color(0xFF0C1615)
val surfaceContainerHighDark = Color(0xFF142220)
val surfaceContainerHighestDark = Color(0xFF1E2E2C)

// MARK: - Semantic Color Tokens
// Use these tokens instead of direct color references to ensure consistency
// across the application and enable easy theme updates.

// Primary brand colors (Coral Red palette)
val MiraPrimary = BrandColors.Primary500
val MiraPrimaryLight = BrandColors.Primary50
val MiraPrimaryDark = BrandColors.Primary700

// Accent colors (Gold/Yellow palette)
val MiraAccent = BrandColors.Amber700
val MiraAccentLight = BrandColors.Amber100

// Status colors
val MiraSuccess = BrandColors.Green
val MiraWarning = BrandColors.Yellow
val MiraError = BrandColors.Coral
val MiraInfo = BrandColors.Orange

// Neutral colors
val MiraBorder = Color(0xFFE5E5E5) // Pale light gray for dividers
val MiraTextPrimary = Color.Black
val MiraTextSecondary = BrandColors.Slate700
val MiraBackground = Color(0xFFFAFAFA)
val MiraSurface = Color.White

// Chat colors
val ChatBubbleSent = BrandColors.Coral
val ChatTextSent = Color(0xFFFFFFFF)
val ChatBubbleReceived = BrandColors.Primary50
val ChatTextReceived = BrandColors.Slate900
val ChatTimeText = BrandColors.Slate700

// DEPRECATED: Use semantic tokens above instead
// These aliases are kept for backward compatibility but should not be used in new code
@Deprecated("Use MiraPrimary instead", ReplaceWith("MiraPrimary"))
val MiraPrimaryBlue = BrandColors.Primary700
@Deprecated("Use MiraPrimaryLight instead", ReplaceWith("MiraPrimaryLight"))
val VelvetaPistachioSoft = BrandColors.Primary50
@Deprecated("Use MiraPrimaryDark instead", ReplaceWith("MiraPrimaryDark"))
val MiraPrimaryDeep = BrandColors.Primary700
@Deprecated("Use BrandColors.Slate300 instead", ReplaceWith("BrandColors.Slate300"))
val MiraFaintGray = BrandColors.Slate300
@Deprecated("Use MiraError instead", ReplaceWith("MiraError"))
val MiraCoralMain = BrandColors.Coral
@Deprecated("Use MiraPrimaryLight instead", ReplaceWith("MiraPrimaryLight"))
val MiraCoralLight = BrandColors.Primary50
@Deprecated("Use MiraAccent instead", ReplaceWith("MiraAccent"))
val VelvetaOrange = BrandColors.Amber700
@Deprecated("Use MiraBackground instead", ReplaceWith("MiraBackground"))
val VelvetaOffWhiteLight = BrandColors.Slate50
@Deprecated("Use BrandColors.Slate700 instead", ReplaceWith("BrandColors.Slate700"))
val VelvetaSlateBlue = BrandColors.Slate700
