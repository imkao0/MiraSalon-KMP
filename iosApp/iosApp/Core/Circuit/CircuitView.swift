import SwiftUI
import ComposeApp
import Combine

/**
 * A generic SwiftUI view that wraps a shared Circuit Presenter.
 *
 * The presenter bridge is created once for the view's lifetime (tied to the
 * screen identity), observed via the Kotlin Flow wrapper, and torn down exactly
 * once in `onDisappear`. A missing presenter renders a loud error instead of
 * crashing on a force-cast.
 */
struct CircuitView<UiState: CircuitUiState, Content: View>: View {
    let screen: any Circuit_runtime_screenScreen
    let content: (UiState) -> Content

    @StateObject private var presenterWrapper: PresenterWrapper<UiState>

    init(
        screen: any Circuit_runtime_screenScreen,
        navigator: CircuitNavigation,
        @ViewBuilder content: @escaping (UiState) -> Content
    ) {
        self.screen = screen
        self.content = content
        _presenterWrapper = StateObject(
            wrappedValue: PresenterWrapper(screen: screen, navigator: navigator)
        )
    }

    var body: some View {
        Group {
            if let state = presenterWrapper.state {
                content(state)
            } else if presenterWrapper.failedToCreate {
                ContentUnavailableView(
                    "Screen Unavailable",
                    systemImage: "exclamationmark.triangle",
                    description: Text("No presenter is registered for this screen.")
                )
            } else {
                MiraLoadingView()
            }
        }
        .onAppear {
            presenterWrapper.start()
        }
        .onDisappear {
            presenterWrapper.teardown()
        }
    }
}

/** Observes the Kotlin state flow for a presenter bridge. */
private final class PresenterWrapper<UiState: CircuitUiState>: ObservableObject {
    @Published var state: UiState?
    @Published var failedToCreate = false

    private let screen: any Circuit_runtime_screenScreen
    private let navigator: CircuitNavigation
    
    private var bridge: CircuitPresenterKotlinBridge<UiState>?
    private var job: Kotlinx_coroutines_coreJob?
    private var tornDown = false
    private var started = false

    init(screen: any Circuit_runtime_screenScreen, navigator: CircuitNavigation) {
        self.screen = screen
        self.navigator = navigator
    }

    func start() {
        guard !started && !tornDown else { return }
        started = true
        
        guard let bridge = IosBridge.shared.presenter(screen: screen, navigator: navigator)
                as? CircuitPresenterKotlinBridge<UiState> else {
            let screenName = String(describing: screen)
            print("CircuitView: no presenter registered for screen \(screenName)")
            print("CircuitView: screen type \(type(of: screen))")
            failedToCreate = true
            return
        }
        self.bridge = bridge
        
        // FlowWrapper<T>.watch delivers `T?` (ObjC nullability). Unwrap before
        // casting — casting Optional<T> directly to UiState always fails.
        self.job = bridge.state.watch { [weak self] newState in
            guard let self = self else { return }
            guard let newState = newState else { 
                print("CircuitView: received nil state for screen \(String(describing: self.screen))")
                return 
            }
            DispatchQueue.main.async { [weak self] in
                guard let self = self else { return }
                if let castState = newState as? UiState {
                    self.state = castState
                } else {
                    print("CircuitView: state type mismatch. Expected: \(String(describing: UiState.self)), Got: \(String(describing: type(of: newState)))")
                }
            }
        }
    }

    func teardown() {
        guard !tornDown else { return }
        tornDown = true
        job?.cancel(cause: nil)
        bridge?.clear()
        bridge = nil
        job = nil
    }
}
