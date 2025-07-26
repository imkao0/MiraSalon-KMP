import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        IosBridgeKt.doInitKoinForIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
