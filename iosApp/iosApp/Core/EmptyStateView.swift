import SwiftUI

struct EmptyStateView: View {
    let message: String
    let description: String?
    let icon: String // SF Symbol name

    @State private var isAnimating = false

    init(message: String, description: String? = nil, icon: String) {
        self.message = message
        self.description = description
        self.icon = icon
    }

    var body: some View {
        VStack(spacing: MiraTheme.spacingMedium) {
            ZStack {
                Circle()
                    .fill(MiraTheme.primaryContainer.opacity(0.2))
                    .frame(width: 100, height: 100)
                    .scaleEffect(isAnimating ? 1.1 : 0.9)

                Image(systemName: icon)
                    .font(.system(size: 48))
                    .foregroundColor(MiraTheme.primary)
            }
            .onAppear {
                withAnimation(.easeInOut(duration: 1.5).repeatForever(autoreverses: true)) {
                    isAnimating = true
                }
            }

            Text(message)
                .font(MiraType.titleMedium)
                .bold()
                .foregroundColor(MiraTheme.onSurface)
                .multilineTextAlignment(.center)

            if let description = description {
                Text(description)
                    .font(MiraType.bodyMedium)
                    .foregroundColor(MiraTheme.onSurfaceVariant.opacity(0.7))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, MiraTheme.spacingSmall)
            }
        }
        .padding(MiraTheme.spacingLarge)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
