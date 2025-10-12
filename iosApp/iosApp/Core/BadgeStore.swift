import SwiftUI
import ComposeApp
import Combine

/**
 * Global store for bottom navigation badge counts (Cart, Chat, Notifications).
 * Observes real-time Flows from Kotlin and publishes updates to SwiftUI.
 */
class BadgeStore: ObservableObject {
    static let shared = BadgeStore()

    @Published var cartCount: Int = 0
    @Published var chatCount: Int = 0
    @Published var notificationCount: Int = 0

    private var cartJob: Kotlinx_coroutines_coreJob?
    private var chatJob: Kotlinx_coroutines_coreJob?
    private var notificationJob: Kotlinx_coroutines_coreJob?

    private init() {}

    func setup() {
        stopObserving()
        
        // Cart
        cartJob = IosBridge.shared.observeCartCount().watch { [weak self] count in
            if let val = count as? Int32 {
                DispatchQueue.main.async { self?.cartCount = Int(val) }
            }
        }
        
        // Chat
        chatJob = IosBridge.shared.observeUnreadChatCount().watch { [weak self] count in
            if let val = count as? Int32 {
                DispatchQueue.main.async { self?.chatCount = Int(val) }
            }
        }
        
        // Notifications
        notificationJob = IosBridge.shared.observeUnreadNotificationCount().watch { [weak self] count in
            if let val = count as? Int32 {
                DispatchQueue.main.async { self?.notificationCount = Int(val) }
            }
        }
    }

    func stopObserving() {
        cartJob?.cancel(cause: nil)
        chatJob?.cancel(cause: nil)
        notificationJob?.cancel(cause: nil)
        
        cartJob = nil
        chatJob = nil
        notificationJob = nil
    }
}
