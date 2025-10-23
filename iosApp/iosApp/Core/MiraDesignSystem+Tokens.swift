import SwiftUI
import ComposeApp

/**
 * Extended design tokens + shared primitives so every revamp screen is a
 * pixel-faithful SwiftUI twin of its Android (Compose) counterpart.
 *
 * All values map 1:1 to `core:designsystem` (Spacing.kt / Color.kt) and the
 * hard-coded dp values used by the Android screens being mirrored.
 */
extension MiraTheme {
    // --- Status / brand accents used by the Android screens ---
    // These now delegate to the bridged tokens in MiraDesignSystem.swift
    static let unreadDot = SwiftUI.Color.red

    // Primary brand accents used by the Android screens (Mapped to Primary Teal)
    static let brandBlue = MiraTheme.primary
    static let brandBlueDark = MiraTheme.primary

    // Common radii actually used by the mirrored Android screens
    static let radiusCard: CGFloat = MiraTheme.radiusMedium
    static let radiusSection: CGFloat = MiraTheme.radiusLarge
    static let radiusButton: CGFloat = MiraTheme.radiusSmall

    static let radiusField: CGFloat = MiraTheme.radiusSmall
    static let radiusPill: CGFloat = 100 // Fully rounded
}


// MARK: - Currency formatting helper (mirrors toPriceString())
extension Double {
    /// Android `toPriceString()` renders two-decimal currency. Kept symbol-agnostic.
    func miraPrice() -> String {
        String(format: "$%.2f", self)
    }
}

// MARK: - Date/Time helpers mirroring the Android formatters
enum MiraDateFormat {
    /// "12 Aug 2025 - 14:30"  (MyBookings card)
    static func bookingDateTime(epochMillis: Int64) -> String {
        guard epochMillis > 0 else { return "-" }
        let date = Date(timeIntervalSince1970: TimeInterval(epochMillis) / 1000.0)
        let df = DateFormatter()
        df.locale = Locale(identifier: "en_US_POSIX")
        df.dateFormat = "d MMM yyyy"
        let tf = DateFormatter()
        tf.locale = Locale(identifier: "en_US_POSIX")
        tf.dateFormat = "HH:mm"
        return "\(df.string(from: date)) - \(tf.string(from: date))"
    }

    /// "Mon, 12 August 2025" (Payment success)
    static func successDate(epochMillis: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(epochMillis) / 1000.0)
        let df = DateFormatter()
        df.locale = Locale(identifier: "en_US_POSIX")
        df.dateFormat = "EEE, d MMMM yyyy"
        return df.string(from: date)
    }

    /// "14.30" (Payment success time)
    static func dotTime(epochMillis: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(epochMillis) / 1000.0)
        let tf = DateFormatter()
        tf.locale = Locale(identifier: "en_US_POSIX")
        tf.dateFormat = "HH.mm"
        return tf.string(from: date)
    }

    /// "12 Aug at 14:30" (Appointments list item)
    static func shortAt(epochMillis: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(epochMillis) / 1000.0)
        let df = DateFormatter()
        df.locale = Locale(identifier: "en_US_POSIX")
        df.dateFormat = "d MMM"
        let tf = DateFormatter()
        tf.locale = Locale(identifier: "en_US_POSIX")
        tf.dateFormat = "HH:mm"
        return "\(df.string(from: date)) at \(tf.string(from: date))"
    }

    /// "12 Aug" style separator used by checkout + chat
    static func separator(epochSeconds: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(epochSeconds))
        let df = DateFormatter()
        df.locale = Locale(identifier: "en_US_POSIX")
        df.dateFormat = "d MMM"
        return df.string(from: date)
    }

    static func time(epochSeconds: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(epochSeconds))
        let tf = DateFormatter()
        tf.locale = Locale(identifier: "en_US_POSIX")
        tf.dateFormat = "h:mm a"
        return tf.string(from: date)
    }
}
