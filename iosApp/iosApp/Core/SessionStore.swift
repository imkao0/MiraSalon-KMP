import SwiftUI
import ComposeApp

/**
 * Global store for user session state in iOS.
 * Handles login status, guest mode, and coordinated logout.
 */
class SessionStore: ObservableObject {
    static let shared = SessionStore()
    
    @Published var isSessionStarted: Bool = false
    @Published var isGuest: Bool = false
    
    private init() {
        updateLoginStatus()
    }
    
    func updateLoginStatus() {
        let loggedIn = IosBridge.shared.isLoggedIn()
        DispatchQueue.main.async {
            self.isSessionStarted = loggedIn || self.isGuest
        }
    }
    
    func logout() {
        IosBridge.shared.logout()
        DispatchQueue.main.async {
            self.isSessionStarted = false
            self.isGuest = false
        }
    }
}
