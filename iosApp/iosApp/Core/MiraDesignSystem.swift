import SwiftUI
import ComposeApp

// MARK: - Mira Design System

/// Single source of truth for design tokens, bridging Kotlin M3 definitions 
/// to SwiftUI primitives and semantic theme mappings.

// MARK: - Brand shade primitives (Color.kt -> BrandColors)
enum MiraBrand {
    // Primary Seed & Shades (Coral Red)
    static let primary50  = SwiftUI.Color(argb: 0xFFFFEBEB)
    static let primary100 = SwiftUI.Color(argb: 0xFFFFC4C4)
    static let primary200 = SwiftUI.Color(argb: 0xFFFF9A9A)
    static let primary300 = SwiftUI.Color(argb: 0xFFFF7070)
    static let primary400 = SwiftUI.Color(argb: 0xFFF06A6A)
    static let primary500 = SwiftUI.Color(argb: 0xFFE63946)
    static let primary600 = SwiftUI.Color(argb: 0xFFD62839)
    static let primary700 = SwiftUI.Color(argb: 0xFFC1121F)
    static let primary800 = SwiftUI.Color(argb: 0xFF9D0208)
    static let primary900 = SwiftUI.Color(argb: 0xFF6B0F1A)
    static let primary950 = SwiftUI.Color(argb: 0xFF370617)

    // Accent Seed & Shades (Imperial Gold & Champagne)
    static let amber50  = SwiftUI.Color(argb: 0xFFFFFDE7)
    static let amber100 = SwiftUI.Color(argb: 0xFFFFF9C4)
    static let amber200 = SwiftUI.Color(argb: 0xFFFFF59D)
    static let amber300 = SwiftUI.Color(argb: 0xFFFFEE58)
    static let amber400 = SwiftUI.Color(argb: 0xFFFFEB3B)
    static let amber500 = SwiftUI.Color(argb: 0xFFFBC02D)
    static let amber600 = SwiftUI.Color(argb: 0xFFF57F17)
    static let amber700 = SwiftUI.Color(argb: 0xFFD4AF37) // Imperial Gold Accent
    static let amber800 = SwiftUI.Color(argb: 0xFFA67C00)
    static let amber900 = SwiftUI.Color(argb: 0xFF7A5C00)
    static let amber950 = SwiftUI.Color(argb: 0xFF423200)

    // Neutral Seed & Shades (Cool Slate & Jade Neutral)
    static let slate50  = SwiftUI.Color(argb: 0xFFF7FAFA)
    static let slate100 = SwiftUI.Color(argb: 0xFFEFF4F4)
    static let slate200 = SwiftUI.Color(argb: 0xFFE2ECEB)
    static let slate300 = SwiftUI.Color(argb: 0xFFCBDCDA)
    static let slate400 = SwiftUI.Color(argb: 0xFF9FB9B5)
    static let slate500 = SwiftUI.Color(argb: 0xFF71918B)
    static let slate600 = SwiftUI.Color(argb: 0xFF52706A)
    static let slate700 = SwiftUI.Color(argb: 0xFF3F5551)
    static let slate800 = SwiftUI.Color(argb: 0xFF283835)
    static let slate900 = SwiftUI.Color(argb: 0xFF1B2624)
    static let slate950 = SwiftUI.Color(argb: 0xFF0E1413)
}

// MARK: - Semantic theme tokens (bridged from Kotlin DesignSystemBridge)
enum MiraTheme {

    // --- Colors (M3 dynamic scheme) ---
    static let primary = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.primaryLight, dark: DesignSystemBridge.Colors.shared.primaryDark)
    static let onPrimary = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.onPrimaryLight, dark: DesignSystemBridge.Colors.shared.onPrimaryDark)
    static let primaryContainer = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.primaryContainerLight, dark: DesignSystemBridge.Colors.shared.primaryContainerDark)
    static let onPrimaryContainer = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.onPrimaryContainerLight, dark: DesignSystemBridge.Colors.shared.onPrimaryContainerDark)

    static let secondary = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.secondaryLight, dark: DesignSystemBridge.Colors.shared.secondaryDark)
    static let onSecondary = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.onSecondaryLight, dark: DesignSystemBridge.Colors.shared.onSecondaryDark)
    static let secondaryContainer = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.secondaryContainerLight, dark: DesignSystemBridge.Colors.shared.secondaryContainerDark)
    static let onSecondaryContainer = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.onSecondaryContainerLight, dark: DesignSystemBridge.Colors.shared.onSecondaryContainerDark)

    static let tertiary = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.tertiaryLight, dark: DesignSystemBridge.Colors.shared.tertiaryDark)
    static let onTertiary = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.onTertiaryLight, dark: DesignSystemBridge.Colors.shared.onTertiaryDark)
    static let tertiaryContainer = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.tertiaryContainerLight, dark: DesignSystemBridge.Colors.shared.tertiaryContainerDark)
    static let onTertiaryContainer = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.onTertiaryContainerLight, dark: DesignSystemBridge.Colors.shared.onTertiaryContainerDark)

    static let background = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.backgroundLight, dark: DesignSystemBridge.Colors.shared.backgroundDark)
    static let onBackground = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.onBackgroundLight, dark: DesignSystemBridge.Colors.shared.onBackgroundDark)
    static let surface = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.surfaceLight, dark: DesignSystemBridge.Colors.shared.surfaceDark)
    static let onSurface = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.onSurfaceLight, dark: DesignSystemBridge.Colors.shared.onSurfaceDark)
    static let surfaceVariant = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.surfaceVariantLight, dark: DesignSystemBridge.Colors.shared.surfaceVariantDark)
    static let onSurfaceVariant = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.onSurfaceVariantLight, dark: DesignSystemBridge.Colors.shared.onSurfaceVariantDark)

    static let error = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.errorLight, dark: DesignSystemBridge.Colors.shared.errorDark)
    static let onError = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.onErrorLight, dark: DesignSystemBridge.Colors.shared.onErrorDark)
    static let errorContainer = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.errorContainerLight, dark: DesignSystemBridge.Colors.shared.errorContainerDark)
    static let onErrorContainer = SwiftUI.Color.dynamic(light: DesignSystemBridge.Colors.shared.onErrorContainerLight, dark: DesignSystemBridge.Colors.shared.onErrorContainerDark)

    static let outline = MiraBrand.slate400
    static let outlineVariant = MiraBrand.slate200
    static let scrim = SwiftUI.Color.black
    
    // --- Semantic status tokens ---
    static let success = SwiftUI.Color(hex: DesignSystemBridge.Colors.shared.success)
    static let cancelled = SwiftUI.Color(hex: DesignSystemBridge.Colors.shared.cancelled)

    // --- Semantic component aliases (Color.kt section 2) ---
    static let miraBorder = SwiftUI.Color.dynamic(light: 0xFFE2ECEB, dark: 0xFF1E2E2C) // slate200 / outlineDark
    static let textPrimary = onBackground
    static let textSecondary = onSurfaceVariant
    static let chatBubbleSent = primary
    static let chatTextSent = onPrimary
    static let chatBubbleReceived = primaryContainer
    static let chatTextReceived = onPrimaryContainer
    static let chatTimeText = onSurfaceVariant

    // --- Spacing scale (Spacing.kt) ---
    static let spacingNone: CGFloat = CGFloat(DesignSystemBridge.Spacing.shared.none)
    static let spacingTiny: CGFloat = CGFloat(DesignSystemBridge.Spacing.shared.tiny)
    static let spacingSmall: CGFloat = CGFloat(DesignSystemBridge.Spacing.shared.small)
    static let spacingMedium: CGFloat = CGFloat(DesignSystemBridge.Spacing.shared.medium)
    static let spacingLarge: CGFloat = CGFloat(DesignSystemBridge.Spacing.shared.large)
    static let spacingExtraLarge: CGFloat = CGFloat(DesignSystemBridge.Spacing.shared.extraLarge)
    static let spacingDefault: CGFloat = CGFloat(DesignSystemBridge.Spacing.shared.default)
    static let spacingIntermediate: CGFloat = CGFloat(DesignSystemBridge.Spacing.shared.intermediate)
    static let spacingSection: CGFloat = CGFloat(DesignSystemBridge.Spacing.shared.section)

    // --- Icon sizes (Spacing.kt) ---
    static let iconSizeTiny: CGFloat = CGFloat(DesignSystemBridge.Metrics.shared.iconTiny)
    static let iconSizeSmall: CGFloat = CGFloat(DesignSystemBridge.Metrics.shared.iconSmall)
    static let iconSizeMedium: CGFloat = CGFloat(DesignSystemBridge.Metrics.shared.iconMedium)
    static let iconSizeIntermediate: CGFloat = CGFloat(DesignSystemBridge.Metrics.shared.iconIntermediate)
    static let iconSizeLarge: CGFloat = CGFloat(DesignSystemBridge.Metrics.shared.iconLarge)
    static let iconSizeExtraLarge: CGFloat = CGFloat(DesignSystemBridge.Metrics.shared.iconExtraLarge)

    // --- Component metrics (Spacing.kt) ---
    static let avatarSize: CGFloat = CGFloat(DesignSystemBridge.Metrics.shared.avatarSize)
    static let profileAvatarSize: CGFloat = CGFloat(DesignSystemBridge.Metrics.shared.profileAvatarSize)
    static let buttonHeight: CGFloat = CGFloat(DesignSystemBridge.Metrics.shared.buttonHeight)
    static let cardWidthLarge: CGFloat = CGFloat(DesignSystemBridge.Metrics.shared.cardWidthLarge)
    static let cardImageHeight: CGFloat = CGFloat(DesignSystemBridge.Metrics.shared.cardImageHeight)
    static let offerCardWidth: CGFloat = CGFloat(DesignSystemBridge.Metrics.shared.offerCardWidth)
    static let offerCardHeight: CGFloat = CGFloat(DesignSystemBridge.Metrics.shared.offerCardHeight)
    static let bannerHeight: CGFloat = CGFloat(DesignSystemBridge.Metrics.shared.bannerHeight)
    static let categorySize: CGFloat = CGFloat(DesignSystemBridge.Metrics.shared.categorySize)
    static let categoryIconSize: CGFloat = CGFloat(DesignSystemBridge.Metrics.shared.categoryIconSize)
    static let snackbarMaxWidth: CGFloat = 320
    static let snackbarVerticalPadding: CGFloat = 10
    static let textFieldHeightLarge: CGFloat = 120
    static let stepperButtonSize: CGFloat = CGFloat(DesignSystemBridge.Metrics.shared.stepperButtonSize)
    static let starSize: CGFloat = CGFloat(DesignSystemBridge.Metrics.shared.starSize)
    static let starSizeSmall: CGFloat = CGFloat(DesignSystemBridge.Metrics.shared.starSizeSmall)
    static let promoBannerButtonMinWidth: CGFloat = 120
    static let bottomNavHeight: CGFloat = 75
    static let bottomNavIconSize: CGFloat = 24

    // --- Corner radii (Spacing.kt + Shape.kt) ---
    static let radiusTiny: CGFloat = CGFloat(DesignSystemBridge.Radii.shared.tiny)
    static let radiusExtraSmall: CGFloat = CGFloat(DesignSystemBridge.Radii.shared.extraSmall)
    static let radiusSmall: CGFloat = CGFloat(DesignSystemBridge.Radii.shared.small)
    static let radiusDefault: CGFloat = CGFloat(DesignSystemBridge.Radii.shared.default)
    static let radiusMedium: CGFloat = CGFloat(DesignSystemBridge.Radii.shared.medium)
    static let radiusLarge: CGFloat = CGFloat(DesignSystemBridge.Radii.shared.large)
    static let radiusExtraLarge: CGFloat = CGFloat(DesignSystemBridge.Radii.shared.extraLarge)
    static let radiusProfileCard: CGFloat = CGFloat(DesignSystemBridge.Radii.shared.profileCard)
    static let radiusFull: CGFloat = CGFloat(DesignSystemBridge.Radii.shared.full)
    static let radiusPromo: CGFloat = CGFloat(DesignSystemBridge.Radii.shared.promo)
    static let radiusPromoInner: CGFloat = CGFloat(DesignSystemBridge.Radii.shared.promoInner)

    // --- Strokes & thickness (Spacing.kt) ---
    static let strokeThin: CGFloat = 1
    static let strokeMedium: CGFloat = 2
    static let bottomNavDividerThickness: CGFloat = 0.8

    // --- Elevation (Spacing.kt) ---
    static let elevationNone: CGFloat = 0
    static let elevationLow: CGFloat = 4
    static let elevationMedium: CGFloat = 6
}

// MARK: - Typography (Type.kt + Material 3 defaults)
enum MiraType {
    static let displayLarge   = SwiftUI.Font.system(size: 57, weight: .regular)
    static let displayMedium  = SwiftUI.Font.system(size: 45, weight: .regular)
    static let displaySmall   = SwiftUI.Font.system(size: 36, weight: .regular)
    static let headlineLarge  = SwiftUI.Font.system(size: 32, weight: .regular)
    static let headlineMedium = SwiftUI.Font.system(size: 28, weight: .regular)
    static let headlineSmall  = SwiftUI.Font.system(size: 24, weight: .regular)
    static let titleLarge     = SwiftUI.Font.system(size: 22, weight: .regular)
    static let titleMedium    = SwiftUI.Font.system(size: 16, weight: .medium)
    static let titleSmall     = SwiftUI.Font.system(size: 14, weight: .medium)
    static let bodyLarge      = SwiftUI.Font.system(size: 16, weight: .regular)
    static let bodyMedium     = SwiftUI.Font.system(size: 14, weight: .regular)
    static let bodySmall      = SwiftUI.Font.system(size: 12, weight: .regular)
    static let labelLarge     = SwiftUI.Font.system(size: 14, weight: .medium)
    static let labelMedium    = SwiftUI.Font.system(size: 12, weight: .medium)
    static let labelSmall     = SwiftUI.Font.system(size: 11, weight: .medium)
}

// MARK: - View modifiers (token consumers)
extension View {
    func miraElevation(_ level: CGFloat) -> some View {
        guard level > 0 else { return AnyView(self) }
        return AnyView(
            self.shadow(color: SwiftUI.Color.black.opacity(0.08), radius: level, x: 0, y: level / 2)
        )
    }

    func miraOutlineBorder(cornerRadius: CGFloat = MiraTheme.radiusSmall,
                           color: SwiftUI.Color = MiraTheme.outlineVariant,
                           lineWidth: CGFloat = MiraTheme.strokeThin) -> some View {
        overlay(
            RoundedRectangle(cornerRadius: cornerRadius)
                .stroke(color, lineWidth: lineWidth)
        )
    }
}

// MARK: - Color initializers
extension SwiftUI.Color {
    /// Dynamic color helper for Light/Dark mode.
    static func dynamic(light: Int64, dark: Int64) -> SwiftUI.Color {
        SwiftUI.Color(UIColor { traitCollection in
            traitCollection.userInterfaceStyle == .dark ? UIColor(SwiftUI.Color(hex: dark)) : UIColor(SwiftUI.Color(hex: light))
        })
    }

    /// Kotlin AARRGGBB hex (as exposed by DesignSystemBridge).
    init(hex: Int64) {
        let uhex = UInt64(bitPattern: hex)
        let a = Double((uhex >> 24) & 0xff) / 255.0
        let r = Double((uhex >> 16) & 0xff) / 255.0
        let g = Double((uhex >> 8) & 0xff) / 255.0
        let b = Double(uhex & 0xff) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: a)
    }

    /// AARRGGBB literal for compile-time-known brand tokens.
    init(argb: UInt32) {
        let a = Double((argb >> 24) & 0xff) / 255.0
        let r = Double((argb >> 16) & 0xff) / 255.0
        let g = Double((argb >> 8) & 0xff) / 255.0
        let b = Double(argb & 0xff) / 255.0
        self.init(.sRGB, red: r, green: g, blue: b, opacity: a)
    }
}
