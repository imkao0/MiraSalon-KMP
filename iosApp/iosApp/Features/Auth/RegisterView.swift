import SwiftUI
import ComposeApp

/**
 * Pixel-faithful mirror of `RegisterScreen.kt`.
 * Same scaffold as Login: back button, headlineMedium Bold headline,
 * Username / Email / Password / Confirm password AuthTextFields (12.dp
 * apart, sharing one visibility toggle), 56pt primary Register button,
 * social section and a footer "Login Now" link.
 */
struct RegisterView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    let screen: AuthRouteRegister

    var body: some View {
        CircuitView(screen: screen, navigator: navigation) { (state: AuthState) in
            RegisterContent(state: state)
                .onChange(of: state.isSuccess) { success in
                    if success {
                        print("Register success! Sending AuthSuccess event.")
                        state.eventSink(AuthEventAuthSuccess())
                    }
                }
                .navigationBarBackButtonHidden(true)
        }
    }
}

/// Stateless content (state hoisted) so the layout is previewable.
struct RegisterContent: View {
    let isLoading: Bool
    let error: String?
    let onBack: () -> Void
    let onRegister: (String, String, String) -> Void
    let onLogin: () -> Void

    @State private var username = ""
    @State private var email = ""
    @State private var password = ""
    @State private var confirmPassword = ""
    @State private var showErrors = false
    @FocusState private var focusedField: Field?

    enum Field {
        case username, email, password, confirmPassword
    }

    init(state: AuthState) {
        self.isLoading = state.isLoading
        self.error = state.error
        self.onBack = { state.eventSink(AuthEventBack()) }
        self.onRegister = { name, email, password in
            state.eventSink(AuthEventRegister(name: name, email: email, password: password))
        }
        self.onLogin = { state.eventSink(AuthEventNavigateToLogin()) }
    }

    init(isLoading: Bool = false,
         error: String? = nil,
         onBack: @escaping () -> Void = {},
         onRegister: @escaping (String, String, String) -> Void = { _, _, _ in },
         onLogin: @escaping () -> Void = {}) {
        self.isLoading = isLoading
        self.error = error
        self.onBack = onBack
        self.onRegister = onRegister
        self.onLogin = onLogin
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Spacer().frame(height: MiraTheme.spacingExtraLarge)

                AuthBackButton(onClick: onBack)

                Spacer().frame(height: MiraTheme.spacingExtraLarge)

                Text("Hello! Register to get\nstarted")
                    .font(.system(size: 28, weight: .bold)) // headlineMedium + Bold
                    .lineSpacing(8) // 36.sp line height
                    .foregroundColor(MiraTheme.textPrimary)

                Spacer().frame(height: MiraTheme.spacingExtraLarge)

                AuthTextField(
                    text: $username,
                    hint: "Username",
                    isError: showErrors && username.isEmpty,
                    submitLabel: .next,
                    onSubmit: { focusedField = .email }
                )
                .focused($focusedField, equals: .username)
                .onChange(of: username) { _ in showErrors = false }

                Spacer().frame(height: MiraTheme.spacingDefault)

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
                    submitLabel: .next,
                    onSubmit: { focusedField = .confirmPassword }
                )
                .focused($focusedField, equals: .password)
                .onChange(of: password) { _ in showErrors = false }

                Spacer().frame(height: MiraTheme.spacingDefault)

                AuthTextField(
                    text: $confirmPassword,
                    hint: "Confirm password",
                    isPassword: true,
                    isError: showErrors && (confirmPassword.isEmpty || confirmPassword != password),
                    submitLabel: .done,
                    onSubmit: {
                        if !username.isEmpty && !email.isEmpty && !password.isEmpty && confirmPassword == password {
                            onRegister(username, email, password)
                        }
                    }
                )
                .focused($focusedField, equals: .confirmPassword)
                .onChange(of: confirmPassword) { _ in showErrors = false }

                Spacer().frame(height: MiraTheme.spacingExtraLarge)

                if let error {
                    Text(error)
                        .foregroundColor(MiraTheme.error)
                        .padding(.bottom, MiraTheme.spacingMedium)
                }

                Button {
                    if username.isEmpty || email.isEmpty || password.isEmpty || confirmPassword != password {
                        showErrors = true
                    } else {
                        print("Register button tapped with name: \(username), email: \(email)")
                        onRegister(username, email, password)
                    }
                } label: {
                    ZStack {
                        if isLoading {
                            ProgressView()
                                .tint(MiraTheme.onPrimary)
                        } else {
                            Text("Register")
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(MiraTheme.onPrimary)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: MiraTheme.buttonHeight)
                    .background(MiraTheme.primary)
                    .cornerRadius(MiraTheme.radiusMedium)
                }
                .buttonStyle(.plain)
                .disabled(isLoading)

                Spacer().frame(height: MiraTheme.spacingExtraLarge)

                SocialLoginSection(label: "Or Register with")

                Spacer().frame(height: MiraTheme.spacingExtraLarge)

                HStack(spacing: 0) {
                    Spacer()
                    Text("Already have an account? ")
                        .foregroundColor(MiraTheme.textPrimary)
                    Button(action: onLogin) {
                        Text("Login Now")
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
    RegisterContent()
}
