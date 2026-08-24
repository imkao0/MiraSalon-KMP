import SwiftUI

/**
 * Auth building blocks — pixel-faithful mirrors of
 * `feature-auth/presentation/ui/AuthComponents.kt`:
 * AuthBackButton, AuthTextField, SocialLoginSection, SocialButton.
 */

// MARK: - AuthBackButton (41x41, 1.dp outlineVariant border, RadiusSmall)
struct AuthBackButton: View {
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            Image(systemName: "chevron.left") // Icons.AutoMirrored.Outlined.ArrowBackIos
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 16, height: 16)
                .foregroundColor(MiraTheme.textPrimary)
                .frame(width: 41, height: 41)
                .contentShape(Rectangle())
                .overlay(
                    RoundedRectangle(cornerRadius: MiraTheme.radiusSmall)
                        .stroke(MiraTheme.outlineVariant, lineWidth: MiraTheme.strokeThin)
                )
        }
        .buttonStyle(.plain)
        .accessibilityLabel("Back")
    }
}

// MARK: - AuthTextField (56pt, surfaceVariant fill, RadiusSmall, password toggle)
struct AuthTextField: View {
    @Binding var text: String
    let hint: String
    var isPassword: Bool = false
    var isError: Bool = false
    var submitLabel: SubmitLabel = .next
    var onSubmit: () -> Void = {}
    @State private var passwordVisible: Bool = false
    @FocusState private var isFocused: Bool

    var body: some View {
        HStack(spacing: 0) {
            if isPassword && !passwordVisible {
                SecureField("", text: $text, prompt: prompt)
                    .focused($isFocused)
                    .submitLabel(submitLabel)
                    .onSubmit(onSubmit)
                    .frame(maxWidth: .infinity, alignment: .leading)
            } else {
                TextField("", text: $text, prompt: prompt)
                    .focused($isFocused)
                    .submitLabel(submitLabel)
                    .onSubmit(onSubmit)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            if isPassword {
                Button {
                    passwordVisible.toggle()
                } label: {
                    Image(systemName: passwordVisible ? "eye" : "eye.slash") // Visibility / VisibilityOff
                        .foregroundColor(isError ? MiraTheme.error : MiraTheme.textSecondary)
                        .frame(width: 48, height: 48)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(passwordVisible ? "Hide password" : "Show password")
            }
        }
        .font(MiraType.bodyLarge)
        .foregroundColor(MiraTheme.textPrimary)
        .padding(.leading, MiraTheme.spacingMedium)
        .padding(.trailing, isPassword ? 0 : MiraTheme.spacingMedium)
        .frame(maxWidth: .infinity)
        .frame(height: MiraTheme.buttonHeight)
        .background(isError ? MiraTheme.error.opacity(0.1) : MiraTheme.surfaceVariant)
        .cornerRadius(MiraTheme.radiusSmall)
        .overlay(
            RoundedRectangle(cornerRadius: MiraTheme.radiusSmall)
                .stroke(isError ? MiraTheme.error : Color.clear, lineWidth: 1)
        )
    }

    private var prompt: Text {
        Text(hint).foregroundColor(MiraTheme.textSecondary)
    }
}

// MARK: - SocialLoginSection (dividers + label, then 3 social buttons, 8.dp apart)
struct SocialLoginSection: View {
    let label: String

    var body: some View {
        VStack(spacing: 0) {
            HStack {
                Rectangle()
                    .fill(MiraTheme.outlineVariant)
                    .frame(height: MiraTheme.strokeThin)

                Text(label)
                    .font(.system(size: 14))
                    .foregroundColor(MiraTheme.textSecondary)
                    .padding(.horizontal, 12)

                Rectangle()
                    .fill(MiraTheme.outlineVariant)
                    .frame(height: MiraTheme.strokeThin)
            }

            Spacer().frame(height: 22)

            HStack(spacing: 8) {
                SocialButton(icon: "google_icon") { Image(systemName: "g.circle.fill") }
                SocialButton(icon: "apple_icon")  { Image(systemName: "applelogo") }
                SocialButton(icon: "sms_icon")    { Image(systemName: "message.fill") }
            }
        }
    }
}

// MARK: - SocialButton (72pt high, no border, 32pt icon)
private struct SocialButton<Placeholder: View>: View {
    let icon: String
    @ViewBuilder let placeholder: () -> Placeholder

    var body: some View {
        Button {} label: {
            Group {
                if UIImage(named: icon) != nil {
                    Image(icon)
                        .resizable()
                        .scaledToFit()
                } else {
                    placeholder()
                        .foregroundColor(MiraTheme.textSecondary)
                }
            }
            .frame(width: 32, height: 32)
            .frame(maxWidth: .infinity)
            .frame(height: 72)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Previews
#Preview("Auth Components") {
    VStack(spacing: 16) {
        AuthBackButton(onClick: {})
        AuthTextField(text: .constant(""), hint: "Enter your email")
        AuthTextField(text: .constant("secret"), hint: "Enter your password", isPassword: true)
        SocialLoginSection(label: "Or Login with")
    }
    .padding(24)
    .background(MiraTheme.background)
}
