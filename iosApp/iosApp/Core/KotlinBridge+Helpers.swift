import Foundation
import ComposeApp

/**
 * Helpers for bridging Foundation <-> Kotlin/Native types used by Circuit
 * event sinks (e.g. uploading an avatar image as a Kotlin ByteArray).
 */
extension KotlinByteArray {
    /// Build a Kotlin ByteArray from Foundation Data.
    static func from(data: Data) -> KotlinByteArray {
        let bytes = KotlinByteArray(size: Int32(data.count))
        data.enumerated().forEach { index, byte in
            bytes.set(index: Int32(index), value: Int8(bitPattern: byte))
        }
        return bytes
    }
}
