import Foundation
import ComposeApp
import SwiftUI

/**
 * A per-tab navigation store implementing the shared Circuit `Navigator` contract.
 *
 * Each tab owns its own store (so navigation in one tab never mutates another).
 * The store keeps a Kotlin `Screen` back stack; the root is always retained and
 * is rendered by `NavigationStack`'s root view, while `stack` holds only the
 * pushed screens that back the navigation path.
 *
 * All mutations happen on the main thread. The implementation honours the
 * Circuit contract: `goTo` returns whether the navigation was accepted, `pop`
 * returns the popped screen (never the root), and `peek*` reflects the full
 * back stack including the root.
 */
final class CircuitNavigation: ObservableObject, Navigator {

    /// The root screen this store was created with. Never popped.
    let root: AnyScreen

    /// Pushed screens (excludes the root). Drives the `NavigationStack` path.
    @Published private(set) var stack: [AnyScreen] = []

    /// Callback triggered when the root is reset.
    var onRootReset: ((any Circuit_runtime_screenScreen) -> Void)?

    /// Forward history for `forward()`. Cleared on any fresh navigation.
    private var forwardStack: [AnyScreen] = []

    init(root: any Circuit_runtime_screenScreen) {
        self.root = AnyScreen(base: root)
    }

    /// Full back stack including the root.
    private var fullStack: [AnyScreen] { [root] + stack }

    // MARK: - Navigator (Circuit contract)

    func goTo(screen: any Circuit_runtime_screenScreen) -> Bool {
        if Thread.isMainThread {
            push(screen)
        } else {
            DispatchQueue.main.sync { push(screen) }
        }
        return true
    }

    @discardableResult
    func pop(result: (any Circuit_runtime_screenPopResult)?) -> (any Circuit_runtime_screenScreen)? {
        if Thread.isMainThread {
            return popInternal()
        } else {
            return DispatchQueue.main.sync { popInternal() }
        }
    }

    @discardableResult
    func resetRoot(
        newRoot: any Circuit_runtime_screenScreen,
        options: NavigatorStateOptions
    ) -> [any Circuit_runtime_screenScreen] {
        let apply = { () -> [any Circuit_runtime_screenScreen] in
            let previous = self.fullStack.map { $0.base }
            let newRootWrapper = AnyScreen(base: newRoot)
            self.stack = []
            // In our multi-tab setup, resetRoot usually means switching from Auth to Main.
            // We notify the root view to swap the root if necessary.
            self.onRootReset?(newRoot)
            self.forwardStack.removeAll()
            return previous
        }
        if Thread.isMainThread {
            return apply()
        } else {
            return DispatchQueue.main.sync { apply() }
        }
    }

    func peek() -> (any Circuit_runtime_screenScreen)? {
        fullStack.last?.base
    }

    func peekBackStack() -> [any Circuit_runtime_screenScreen] {
        fullStack.map { $0.base }
    }

    func peekNavStack() -> (any Circuit_runtime_navigationNavStackList)? {
        // The native stack does not back a Circuit `NavStack`; returning nil is
        // a valid "no snapshot available" response.
        nil
    }

    @discardableResult
    func backward() -> Bool {
        pop(result: nil) != nil
    }

    @discardableResult
    func forward() -> Bool {
        if Thread.isMainThread {
            return forwardInternal()
        } else {
            return DispatchQueue.main.sync { forwardInternal() }
        }
    }

    // MARK: - Internals (main thread only)

    private func push(_ screen: any Circuit_runtime_screenScreen) {
        stack.append(AnyScreen(base: screen))
        forwardStack.removeAll()
    }

    private func popInternal() -> (any Circuit_runtime_screenScreen)? {
        guard let popped = stack.popLast() else { return nil } // never pop the root
        forwardStack.append(popped)
        return popped.base
    }

    private func forwardInternal() -> Bool {
        guard let next = forwardStack.popLast() else { return false }
        stack.append(next)
        return true
    }

    /// Replaces the pushed stack from SwiftUI (`NavigationStack` path edits, e.g. swipe-back).
    fileprivate func replaceStack(_ newStack: [AnyScreen]) {
        stack = newStack
        if newStack.count < stack.count {
            forwardStack.removeAll()
        }
    }
}

extension CircuitNavigation {
    /// Two-way binding over the pushed part of the navigation path.
    var stackBinding: Binding<[AnyScreen]> {
        Binding(
            get: { self.stack },
            set: { self.replaceStack($0) }
        )
    }
}
