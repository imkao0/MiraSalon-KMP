import SwiftUI
import ComposeApp

/**
 * Pixel-faithful mirror of `LoginScreen.kt`.
 * 24.dp horizontal padding, back button, headlineMedium Bold headline
 * (36sp line height), email + password AuthTextFields (12.dp apart),
 * right-aligned Forgot Password, 56pt primary Login button with loading
 * indicator, social section and a footer "Register Now" link.
 */
struct LoginView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    let screen: AuthRouteLogin

    var body: some View {
        CircuitView(screen: screen, navigator: navigation) { (state: AuthState) in
            LoginContent(state: state)
                .onChange(of: state.isSuccess) { success in
                    if success {
                        print("Login success! Sending AuthSuccess event.")
                        state.eventSink(AuthEventAuthSuccess())
                    }
                }
                .navigationBarBackButtonHidden(true)
        }
    }
}

/// Stateless content (state hoisted) so the layout is previewable.
struct LoginContent: View {
    let isLoading: Bool
    let error: String?
    let onBack: () -> Void
    let onLogin: (String, String) -> Void
    let onForgotPassword: () -> Void
    let onRegister: () -> Void

    @State private var email = ""
    @State private var password = ""
    @State private var showErrors = false
    @FocusState private var focusedField: Field?

    enum Field {
        case email, password
    }

    init(state: AuthState) {
        self.isLoading = state.isLoading
        self.error = state.error
        self.onBack = { state.eventSink(AuthEventBack()) }
        self.onLogin = { email, password in
            state.eventSink(AuthEventLogin(email: email, password: password))
        }
        self.onForgotPassword = { state.eventSink(AuthEventForgotPassword()) }
        self.onRegister = { state.eventSink(AuthEventNavigateToRegister()) }
    }

    init(isLoading: Bool = false,
         error: String? = nil,
         onBack: @escaping () -> Void = {},
         onLogin: @escaping (String, String) -> Void = { _, _ in },
         onForgotPassword: @escaping () -> Void = {},
         onRegister: @escaping () -> Void = {}) {
        self.isLoading = isLoading
        self.error = error
        self.onBack = onBack
        self.onLogin = onLogin
        self.onForgotPassword = onForgotPassword
        self.onRegister = onRegister
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Spacer().frame(height: MiraTheme.spacingExtraLarge)

                AuthBackButton(onClick: onBack)

                Spacer().frame(height: MiraTheme.spacingExtraLarge)

                Text("Welcome back! Glad\nto see you, Again!")
                    .font(.system(size: 28, weight: .bold)) // headlineMedium + Bold
                    .lineSpacing(8) // 36.sp line height (28pt font + 8pt)
                    .foregroundColor(MiraTheme.textPrimary)

                Spacer().frame(height: MiraTheme.spacingExtraLarge)

                AuthTextField(
                    text: $email,
                    hint: "Enter your email",
                    isError: showErrors && email.isEmpty,
                    submitLabel: .next,
                    onSubmit: { focusedField = .password }
                )
                .focused($focusedField, equals: .email)
                .keyboardType(.emailAddress)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .onChange(of: email) { _ in showErrors = false }

                Spacer().frame(height: MiraTheme.spacingDefault)

                AuthTextField(
                    text: $password,
                    hint: "Enter your password",
                    isPassword: true,
                    isError: showErrors && password.isEmpty,
                    submitLabel: .done,
                    onSubmit: {
                        if !email.isEmpty && !password.isEmpty {
                            onLogin(email, password)
                        }
                    }
                )
                .focused($focusedField, equals: .password)
                .onChange(of: password) { _ in showErrors = false }

                // Right-aligned Forgot Password TextButton
                HStack {
                    Spacer()
                    Button(action: onForgotPassword) {
                        Text("Forgot Password?")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(MiraTheme.textSecondary)
                    }
                    .buttonStyle(.plain)
                }

                Spacer().frame(height: MiraTheme.spacingMedium)

                if let error {
                    Text(error)
                        .foregroundColor(MiraTheme.error)
                        .padding(.bottom, MiraTheme.spacingMedium)
                }

                Button {
                    if email.isEmpty || password.isEmpty {
                        showErrors = true
                    } else {
                        onLogin(email, password)
                    }
                } label: {
                    ZStack {
                        if isLoading {
                            ProgressView()
                                .tint(MiraTheme.onPrimary)
                        } else {
                            Text("Login")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(MiraTheme.onPrimary)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: MiraTheme.buttonHeight)
                    .background(MiraTheme.primary)
                    .cornerRadius(MiraTheme.radiusSmall)
                }
                .buttonStyle(.plain)
                .disabled(isLoading)

                Spacer().frame(height: MiraTheme.spacingExtraLarge)

                SocialLoginSection(label: "Or Login with")

                Spacer().frame(height: MiraTheme.spacingExtraLarge)

                HStack(spacing: 0) {
                    Spacer()
                    Text("Don't have an account? ")
                        .foregroundColor(MiraTheme.textPrimary)
                    Button(action: onRegister) {
                        Text("Register Now")
                            .fontWeight(.bold)
                            .foregroundColor(MiraTheme.primary)
                    }
                    .buttonStyle(.plain)
                    Spacer()
                }
                .font(.system(size: 15))
                .padding(.vertical, MiraTheme.spacingMedium)
            }
            .padding(.horizontal, MiraTheme.spacingLarge)
        }
        .background(MiraTheme.background)
        .scrollDismissesKeyboard(.interactively)
    }
}

#Preview {
    LoginContent()
}
