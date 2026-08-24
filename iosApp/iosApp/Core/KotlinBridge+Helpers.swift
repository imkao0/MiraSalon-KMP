import Foundation
import ComposeApp

/**
 * Helpers for bridging Foundation <-> Kotlin/Native types used by Circuit
 * event sinks (e.g. uploading an avatar image as a Kotlin ByteArray).
 */
extension KotlinByteArray {
    /// Build a Kotlin ByteArray from Foundation Data.
    static func from(data: Data) -> KotlinByteArray {
        // Use the efficient native helper instead of the per-byte loop
        return NSDataUtilsKt.toByteArray(data)
    }
}
