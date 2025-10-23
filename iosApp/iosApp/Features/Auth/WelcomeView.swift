import SwiftUI
import ComposeApp

/**
 * Pixel-faithful mirror of `WelcomeScreen.kt`.
 * Layout: full-width slogan image (weight 1f, ContentScale.Crop) above a
 * 24/32.dp padded column holding Login (filled 56), Register (outlined 56,
 * 1.dp primary border) and "Continue as a guest" (bold 14, secondary).
 */
struct WelcomeView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    let screen: AuthRouteWelcome

    var body: some View {
        CircuitView(screen: screen, navigator: navigation) { (state: AuthState) in
            WelcomeContent(state: state)
        }
    }
}

/// Stateless content so the layout can be previewed without the Circuit runtime.
struct WelcomeContent: View {
    let onLogin: () -> Void
    let onRegister: () -> Void
    let onGuest: () -> Void

    init(state: AuthState) {
        self.onLogin = { state.eventSink(AuthEventNavigateToLogin()) }
        self.onRegister = { state.eventSink(AuthEventNavigateToRegister()) }
        self.onGuest = { state.eventSink(AuthEventContinueAsGuest()) }
    }

    init(onLogin: @escaping () -> Void = {},
         onRegister: @escaping () -> Void = {},
         onGuest: @escaping () -> Void = {}) {
        self.onLogin = onLogin
        self.onRegister = onRegister
        self.onGuest = onGuest
    }

    var body: some View {
        VStack(spacing: 0) {
            // Slogan image — fillMaxWidth + weight(1f), ContentScale.Crop
            GeometryReader { geo in
                if UIImage(named: "slogan") != nil {
                    Image("slogan")
                        .renderingMode(.template)
                        .resizable()
                        .aspectRatio(contentMode: .fill)
                        .frame(width: geo.size.width, height: geo.size.height)
                        .foregroundColor(MiraTheme.onBackground)
                        .clipped()
                } else {
                    // Asset placeholder (brand gradient stand-in for slogan.png)
                    LinearGradient(
                        colors: [MiraTheme.primaryContainer, MiraTheme.primary],
                        startPoint: .top, endPoint: .bottom
                    )
                    .frame(width: geo.size.width, height: geo.size.height)
                    .accessibilityHidden(true)
                }
            }
            .frame(maxWidth: .infinity)
            .frame(maxHeight: .infinity)

            // Buttons — padding(horizontal = 24, vertical = 32)
            VStack(spacing: 0) {
                Button(action: onLogin) {
                    Text("Login")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(MiraTheme.onPrimary)
                        .frame(maxWidth: .infinity)
                        .frame(height: MiraTheme.buttonHeight)
                        .background(MiraTheme.primary)
                        .cornerRadius(MiraTheme.radiusMedium)
                }
                .buttonStyle(.plain)

                Spacer().frame(height: MiraTheme.spacingMedium)

                Button(action: onRegister) {
                    Text("Register")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(MiraTheme.primary)
                        .frame(maxWidth: .infinity)
                        .frame(height: MiraTheme.buttonHeight)
                        .overlay(
                            RoundedRectangle(cornerRadius: MiraTheme.radiusMedium)
                                .stroke(MiraTheme.primary, lineWidth: MiraTheme.strokeThin)
                        )
                }
                .buttonStyle(.plain)

                Spacer().frame(height: MiraTheme.spacingLarge)

                Button(action: onGuest) {
                    Text("Continue as a guest")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(MiraTheme.secondary)
                }
                .buttonStyle(.plain)
            }
            .padding(.horizontal, MiraTheme.spacingLarge)
            .padding(.vertical, MiraTheme.spacingExtraLarge)
        }
        .background(MiraTheme.background)
        .ignoresSafeArea(edges: .top)
    }
}

#Preview {
    WelcomeContent()
}
