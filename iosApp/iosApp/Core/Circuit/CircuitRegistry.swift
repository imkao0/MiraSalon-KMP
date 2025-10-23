import Foundation
import ComposeApp

/**
 * A central registry for Circuit factories in the iOS app.
 * Koin is started once from `iOSApp.init()` via `initKoinForIos()`; this
 * registry only resolves already-registered, null-checked Kotlin accessors,
 * so there are no force-casts on the Swift side.
 */
final class CircuitRegistry {
    static let shared = CircuitRegistry()

    let bridge: CircuitBridge

    private init() {
        self.bridge = IosBridge.shared.circuitBridge()
    }
}
