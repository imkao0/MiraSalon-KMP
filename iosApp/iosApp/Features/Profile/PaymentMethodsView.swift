import SwiftUI
import ComposeApp

// MARK: - Payment Methods Placeholder

/// Placeholder view mirroring Android `PaymentMethodsScreen.kt`.
/// Requires binding `PaymentMethodsPresenter` to a `ProfileRoute` in the 
/// shared presenter factory before this can be migrated to a live Circuit view.
struct PaymentMethodsView: View {
    @EnvironmentObject private var navigation: CircuitNavigation

    var body: some View {
        VStack(spacing: 0) {
            MiraTopBar {
                Text("Payment Methods")
                    .font(.headline)
                    .bold()
            }

            EmptyStateView(
                message: "No payment methods added yet",
                description: "Add a payment method to make your checkout experience faster and easier.",
                icon: "creditcard"
            )
        }
        .background(MiraTheme.background.ignoresSafeArea())
    }
}
