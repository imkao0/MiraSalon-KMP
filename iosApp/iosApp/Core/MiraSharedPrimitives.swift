import SwiftUI
import ComposeApp

/**
 * Cross-feature SwiftUI primitives that mirror the reusable Compose building
 * blocks used by the Android screens (RectangularSwitch, status chips, section
 * headers, menu rows, empty states). Built once, reused across all phases.
 */

// MARK: - Rectangular Switch (mirrors RectangularSwitch.kt)
struct MiraRectangularSwitch: View {
    let isOn: Bool
    let onToggle: (Bool) -> Void

    var body: some View {
        Toggle(isOn: Binding(get: { isOn }, set: { onToggle($0) })) { EmptyView() }
            .labelsHidden()
            .tint(MiraTheme.primary)
    }
}

// MARK: - Radio Button (mirrors Material RadioButton)
struct MiraRadioButton: View {
    let isSelected: Bool
    let onSelect: () -> Void

    var body: some View {
        Button(action: onSelect) {
            ZStack {
                Circle()
                    .stroke(isSelected ? MiraTheme.primary : MiraTheme.onSurfaceVariant, lineWidth: 2)
                    .frame(width: 20, height: 20)
                if isSelected {
                    Circle()
                        .fill(MiraTheme.primary)
                        .frame(width: 10, height: 10)
                }
            }
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Status Chip (Appointments list)
struct MiraStatusChip: View {
    let status: String

    private var color: Color {
        switch status {
        case "Confirmed": return MiraTheme.success
        case "Completed": return MiraTheme.primary
        case "Cancelled": return MiraTheme.cancelled
        default: return MiraTheme.onSurfaceVariant
        }
    }

    var body: some View {
        Text(status)
            .font(.caption)
            .bold()
            .foregroundColor(color)
            .padding(.horizontal, 12)
            .padding(.vertical, 4)
            .background(color.opacity(0.1))
            .cornerRadius(MiraTheme.radiusCard)
    }
}

// MARK: - Section header (Profile / lists)
struct MiraSectionHeader: View {
    let title: String
    var body: some View {
        Text(title)
            .font(.subheadline)
            .bold()
            .foregroundColor(MiraTheme.onBackground)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 24)
            .padding(.vertical, 12)
    }
}

// MARK: - Empty state
struct MiraEmptyState: View {
    let message: String
    var body: some View {
        VStack(spacing: 12) {
            Spacer()
            Image(systemName: "tray")
                .font(.system(size: 40))
                .foregroundColor(MiraTheme.onSurfaceVariant.opacity(0.5))
            Text(message)
                .font(.body)
                .foregroundColor(MiraTheme.onSurfaceVariant)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Async avatar with placeholder
struct MiraAvatar: View {
    let url: String?
    var size: CGFloat = 52
    var body: some View {
        let resolved = ApiEndpoints.shared.resolveImageUrl(imagePath: url)
        AsyncImage(url: URL(string: resolved ?? "")) { phase in
            switch phase {
            case .success(let image):
                image.resizable().aspectRatio(contentMode: .fill)
            case .failure(let error):
                let _ = print("MiraAvatar: Image load failed for \(resolved ?? "nil"): \(error.localizedDescription)")
                MiraTheme.surfaceVariant.overlay(
                    Image(systemName: "person.fill")
                        .foregroundColor(MiraTheme.onSurfaceVariant.opacity(0.3))
                )
            case .empty:
                MiraTheme.surfaceVariant
            @unknown default:
                MiraTheme.surfaceVariant
            }
        }
        .frame(width: size, height: size)
        .clipShape(RoundedRectangle(cornerRadius: MiraTheme.radiusSmall))
        .onAppear {
            if let resolved = resolved {
                print("MiraAvatar: Attempting to load \(resolved)")
            }
        }
    }
}

// MARK: - Selective corner radius (for chat bubbles, asymmetric cards)
struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}

extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}
