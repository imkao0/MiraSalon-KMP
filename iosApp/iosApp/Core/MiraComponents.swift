import SwiftUI
import ComposeApp

// MARK: - Buttons

struct MiraButton: View {
    let text: String
    let onClick: () -> Void
    var enabled: Bool = true
    var isLoading: Bool = false

    var body: some View {
        Button(action: onClick) {
            ZStack {
                if isLoading {
                    MiraShimmer(
                        baseColor: MiraTheme.onPrimary.opacity(0.1),
                        highlightColor: MiraTheme.onPrimary.opacity(0.3)
                    )
                    .frame(width: 80, height: 20)
                    .cornerRadius(4)
                } else {
                    Text(text)
                        .font(MiraType.titleMedium.weight(.bold))
                }
            }
            .foregroundColor(enabled ? MiraTheme.onPrimary : MiraTheme.textSecondary)
            .frame(maxWidth: .infinity)
            .frame(height: MiraTheme.buttonHeight)
            .background(enabled ? MiraTheme.primary : MiraTheme.surfaceVariant)
            .cornerRadius(MiraTheme.radiusSmall)
        }
        .buttonStyle(.plain)
        .disabled(!enabled || isLoading)
    }
}

struct MiraOutlinedButton: View {
    let text: String
    let onClick: () -> Void
    var enabled: Bool = true

    var body: some View {
        Button(action: onClick) {
            Text(text)
                .font(MiraType.titleMedium.weight(.bold))
                .foregroundColor(enabled ? MiraTheme.primary : MiraTheme.textSecondary)
                .frame(maxWidth: .infinity)
                .frame(height: MiraTheme.buttonHeight)
                .overlay(
                    RoundedRectangle(cornerRadius: MiraTheme.radiusSmall)
                        .stroke(
                            enabled ? MiraTheme.primary : MiraTheme.textSecondary.opacity(0.4),
                            lineWidth: MiraTheme.strokeThin
                        )
                )
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }
}

// MARK: - Search Bar

struct MiraSearchBar: View {
    let query: String
    var placeholder: String = "Search by Salons"
    let onSearchClick: () -> Void

    var body: some View {
        Button(action: onSearchClick) {
            HStack {
                Text(query.isEmpty ? placeholder : query)
                    .font(MiraType.bodyLarge)
                    .foregroundColor(query.isEmpty ? MiraTheme.textSecondary : MiraTheme.textPrimary)

                Spacer()

                Image(systemName: "slider.horizontal.3")
                    .foregroundColor(MiraTheme.textPrimary)
            }
            .padding(.horizontal, MiraTheme.spacingMedium)
            .frame(maxWidth: .infinity)
            .frame(height: MiraTheme.buttonHeight)
            .background(MiraTheme.surfaceVariant.opacity(0.5))
            .cornerRadius(MiraTheme.radiusExtraLarge)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Filter Chip

struct MiraChip: View {
    let label: String
    let isSelected: Bool
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            Text(label)
                .font(MiraType.labelLarge)
                .foregroundColor(isSelected ? MiraTheme.onSecondaryContainer : MiraTheme.textSecondary)
                .padding(.horizontal, MiraTheme.spacingMedium)
                .padding(.vertical, MiraTheme.spacingSmall)
                .background(isSelected ? MiraTheme.secondaryContainer : Color.clear)
                .cornerRadius(MiraTheme.radiusSmall)
                .overlay(
                    RoundedRectangle(cornerRadius: MiraTheme.radiusSmall)
                        .stroke(
                            isSelected ? Color.clear : MiraTheme.outlineVariant,
                            lineWidth: MiraTheme.strokeThin
                        )
                )
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Card container

struct MiraCard<Content: View>: View {
    var cornerRadius: CGFloat = MiraTheme.radiusLarge
    var backgroundColor: Color = MiraTheme.surface
    var elevation: CGFloat = 0
    let content: Content

    init(
        cornerRadius: CGFloat = MiraTheme.radiusLarge,
        backgroundColor: Color = MiraTheme.surface,
        elevation: CGFloat = 0,
        @ViewBuilder content: () -> Content
    ) {
        self.cornerRadius = cornerRadius
        self.backgroundColor = backgroundColor
        self.elevation = elevation
        self.content = content()
    }

    var body: some View {
        content
            .background(backgroundColor)
            .cornerRadius(cornerRadius)
            .miraElevation(elevation)
    }
}

// MARK: - Feedback States

struct MiraLoadingView: View {
    var body: some View {
        MiraShimmer()
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .accessibilityLabel("Loading")
    }
}

struct MiraShimmer: View {
    var baseColor: Color = Color(argb: 0xFF2A2A2A)
    var highlightColor: Color = Color(argb: 0xFF3A3A3A)

    @State private var phase: CGFloat = -1

    var body: some View {
        LinearGradient(
            colors: [baseColor, highlightColor, baseColor],
            startPoint: UnitPoint(x: phase, y: 0),
            endPoint: UnitPoint(x: phase + 0.6, y: 0)
        )
        .onAppear {
            withAnimation(.timingCurve(0.4, 0, 1, 1, duration: 1.2).repeatForever(autoreverses: false)) {
                phase = 2
            }
        }
    }
}

struct MiraShimmerBlock: View {
    var width: CGFloat? = nil
    var height: CGFloat
    var cornerRadius: CGFloat = 8

    var body: some View {
        MiraShimmer()
            .frame(width: width, height: height)
            .cornerRadius(cornerRadius)
    }
}

struct MiraEmptyView: View {
    let message: String
    var systemImage: String = "tray"

    var body: some View {
        VStack(spacing: MiraTheme.spacingDefault) {
            Spacer()
            Image(systemName: systemImage)
                .font(.system(size: 40))
                .foregroundColor(MiraTheme.textSecondary.opacity(0.5))
            Text(message)
                .font(MiraType.bodyLarge)
                .foregroundColor(MiraTheme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, MiraTheme.spacingExtraLarge)
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

struct MiraErrorView: View {
    let message: String
    var retryLabel: String = "Retry"
    var onRetry: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: MiraTheme.spacingMedium) {
            Spacer()
            Image(systemName: "exclamationmark.circle")
                .font(.system(size: 40))
                .foregroundColor(MiraTheme.error)
            Text(message)
                .font(MiraType.bodyLarge)
                .foregroundColor(MiraTheme.error)
                .multilineTextAlignment(.center)
                .padding(.horizontal, MiraTheme.spacingExtraLarge)
            if let onRetry {
                Button(action: onRetry) {
                    Text(retryLabel)
                        .font(MiraType.titleMedium.weight(.bold))
                        .foregroundColor(MiraTheme.onPrimary)
                        .padding(.horizontal, MiraTheme.spacingLarge)
                        .frame(height: 40)
                        .background(MiraTheme.primary)
                        .cornerRadius(MiraTheme.radiusSmall)
                }
                .buttonStyle(.plain)
            }
            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Chips

struct RatingChip: View {
    let rating: String

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: "star.fill")
                .resizable()
                .frame(width: MiraTheme.iconSizeTiny, height: MiraTheme.iconSizeTiny)
                .foregroundColor(.yellow)

            Text(rating)
                .font(.caption)
                .bold()
                .foregroundColor(MiraTheme.textSecondary)
        }
        .padding(.horizontal, MiraTheme.spacingDefault)
        .padding(.vertical, 6)
        .background(MiraTheme.surfaceVariant.opacity(0.5))
        .cornerRadius(MiraTheme.radiusSmall)
    }
}

struct InfoChip: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption2)
                .foregroundColor(MiraTheme.textSecondary)
            Text(value)
                .font(.subheadline)
                .bold()
                .foregroundColor(MiraTheme.textPrimary)
        }
        .padding(MiraTheme.spacingDefault)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(
            RoundedRectangle(cornerRadius: MiraTheme.radiusSmall)
                .stroke(MiraTheme.surfaceVariant, lineWidth: 1)
        )
    }
}

// MARK: - Feature Cards

struct SpecialistCard: View {
    let specialist: Specialist
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            VStack(spacing: 8) {
                ZStack(alignment: .bottomTrailing) {
                    let resolvedUrl = ApiEndpoints.shared.resolveImageUrl(imagePath: specialist.imageUrl)
                    AsyncImage(url: URL(string: resolvedUrl ?? "")) { phase in
                        switch phase {
                        case .success(let image):
                            image.resizable()
                                .aspectRatio(contentMode: .fill)
                        case .failure(let error):
                            let _ = print("SpecialistCard: Image load failed for \(resolvedUrl ?? "nil"): \(error.localizedDescription)")
                            MiraTheme.surfaceVariant.overlay(
                                Image(systemName: "person.fill")
                                    .font(.system(size: 40))
                                    .foregroundColor(MiraTheme.onSurfaceVariant.opacity(0.3))
                            )
                        case .empty:
                            MiraTheme.surfaceVariant
                        @unknown default:
                            MiraTheme.surfaceVariant
                        }
                    }
                    .frame(width: 100, height: 100)
                    .clipShape(Circle())
                    .onAppear {
                        if let resolvedUrl = resolvedUrl {
                            print("SpecialistCard: Attempting to load \(resolvedUrl)")
                        }
                    }

                    // Rating badge at bottom-right
                    Circle()
                        .fill(Color(hex: 0xFFFFD700))
                        .frame(width: 24, height: 24)
                        .overlay(
                            Text(String(format: "%.1f", specialist.rating))
                                .font(.system(size: 8, weight: .bold))
                                .foregroundColor(.black)
                        )
                        .overlay(Circle().stroke(MiraTheme.surface, lineWidth: 1.5))
                }

                VStack(alignment: .center, spacing: 2) {
                    Text(specialist.name.components(separatedBy: " ").first ?? "")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(MiraTheme.textPrimary)
                        .lineLimit(1)

                    Text(specialist.role ?? "Specialist")
                        .font(.system(size: 10))
                        .foregroundColor(MiraTheme.textSecondary)
                        .lineLimit(1)
                }
            }
            .frame(width: 120)
        }
        .buttonStyle(.plain)
    }
}

struct OfferCard: View {
    let promotion: Promotion
    let isUsed: Bool
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 0) {
                VStack(alignment: .leading) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(promotion.title.isEmpty ? "-\(promotion.discountPercent)%" : promotion.title)
                            .font(.system(size: 20, weight: .black))
                            .foregroundColor(isUsed ? MiraTheme.textSecondary : MiraTheme.onPrimaryContainer)
                            .lineLimit(1)

                        Text(isUsed ? "You've already used\nthis promotion" : (promotion.discountDescription.isEmpty ? "Voucher for you next\nhaircut service" : promotion.discountDescription))
                            .font(.system(size: 12))
                            .foregroundColor(isUsed ? MiraTheme.textSecondary : MiraTheme.onPrimaryContainer)
                            .lineLimit(2)

                        if let code = promotion.code, !isUsed {
                            Text("Code: \(code)")
                                .font(.system(size: 10, weight: .bold))
                                .foregroundColor(MiraTheme.primary)
                                .padding(.top, 2)
                        }
                    }

                    Spacer()

                    Text(isUsed ? "Used" : (promotion.ctaText ?? "Book now"))
                        .font(.subheadline)
                        .bold()
                        .foregroundColor(isUsed ? MiraTheme.textSecondary : MiraTheme.onPrimary)
                        .padding(.horizontal, MiraTheme.spacingMedium)
                        .padding(.vertical, MiraTheme.spacingSmall)
                        .background(isUsed ? MiraTheme.surface : MiraTheme.primary)
                        .cornerRadius(MiraTheme.radiusLarge)
                }
                .padding(MiraTheme.spacingIntermediate)
                .frame(maxWidth: .infinity, alignment: .leading)

                let resolvedUrl = ApiEndpoints.shared.resolveImageUrl(imagePath: promotion.imageUrl)
                AsyncImage(url: URL(string: resolvedUrl ?? "")) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable()
                            .aspectRatio(contentMode: .fill)
                    case .failure(let error):
                        let _ = print("OfferCard: Image load failed for \(resolvedUrl ?? "nil"): \(error.localizedDescription)")
                        MiraTheme.surfaceVariant
                    case .empty:
                        MiraTheme.surfaceVariant
                    @unknown default:
                        MiraTheme.surfaceVariant
                    }
                }
                .frame(width: MiraTheme.cardWidthLarge)
                .clipShape(
                    UnevenRoundedRectangle(
                        topLeadingRadius: MiraTheme.radiusPromoInner,
                        bottomLeadingRadius: MiraTheme.radiusPromoInner,
                        bottomTrailingRadius: 0,
                        topTrailingRadius: 0
                    )
                )
                .onAppear {
                    if let resolvedUrl = resolvedUrl {
                        print("OfferCard: Attempting to load \(resolvedUrl)")
                    }
                }
            }
            .frame(width: MiraTheme.offerCardWidth, height: MiraTheme.offerCardHeight)
            .background(isUsed ? MiraTheme.surfaceVariant.opacity(0.5) : MiraTheme.primaryContainer)
            .cornerRadius(MiraTheme.radiusPromo)
        }
        .buttonStyle(.plain)
    }
}

struct MiraPromoBanner: View {
    let title: String
    let buttonText: String
    let onButtonClick: () -> Void
    let imageUrl: String?
    let onClick: (() -> Void)?

    var body: some View {
        Button {
            onClick?()
        } label: {
            ZStack(alignment: .bottomLeading) {
                let resolvedUrl = ApiEndpoints.shared.resolveImageUrl(imagePath: imageUrl)
                AsyncImage(url: URL(string: resolvedUrl ?? "")) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable()
                            .aspectRatio(contentMode: .fill)
                    case .failure(let error):
                        let _ = print("MiraPromoBanner: Image load failed for \(resolvedUrl ?? "nil"): \(error.localizedDescription)")
                        MiraTheme.secondaryContainer.opacity(0.2)
                    case .empty:
                        MiraTheme.secondaryContainer.opacity(0.2)
                    @unknown default:
                        MiraTheme.secondaryContainer.opacity(0.2)
                    }
                }
                .frame(maxWidth: .infinity)
                .frame(height: 200)
                .clipped()
                .onAppear {
                    if let resolvedUrl = resolvedUrl {
                        print("MiraPromoBanner: Attempting to load \(resolvedUrl)")
                    }
                }

                LinearGradient(
                    stops: [
                        .init(color: .clear, location: 0),
                        .init(color: MiraTheme.primaryContainer.opacity(0.8), location: 0.7),
                        .init(color: MiraTheme.primaryContainer, location: 1.0)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )

                VStack(alignment: .leading, spacing: 12) {
                    Text(title)
                        .font(.title2)
                        .fontWeight(.black)
                        .foregroundColor(MiraTheme.onPrimaryContainer)
                        .lineLimit(2)

                    Button(action: onButtonClick) {
                        Text(buttonText)
                            .font(.headline)
                            .bold()
                            .foregroundColor(MiraTheme.onPrimary)
                            .padding(.horizontal, MiraTheme.spacingLarge)
                            .padding(.vertical, MiraTheme.spacingDefault)
                            .background(MiraTheme.primary)
                            .cornerRadius(MiraTheme.radiusLarge)
                    }
                    .buttonStyle(.plain)
                }
                .padding(MiraTheme.spacingIntermediate)
            }
            .frame(maxWidth: .infinity)
            .frame(height: MiraTheme.bannerHeight)
            .background(MiraTheme.primaryContainer)
            .cornerRadius(MiraTheme.radiusPromo)
            .overlay(
                RoundedRectangle(cornerRadius: MiraTheme.radiusPromo)
                    .stroke(MiraTheme.textPrimary.opacity(0.1), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .disabled(onClick == nil)
    }
}

// MARK: - Previews

#Preview("Buttons") {
    VStack(spacing: 16) {
        MiraButton(text: "Primary", onClick: {})
        MiraButton(text: "Loading", onClick: {}, isLoading: true)
        MiraButton(text: "Disabled", onClick: {}, enabled: false)
        MiraOutlinedButton(text: "Outlined", onClick: {})
    }
    .padding()
}

#Preview("Search / Chips / States") {
    VStack(spacing: 24) {
        MiraSearchBar(query: "", onSearchClick: {})
        HStack {
            MiraChip(label: "Hair", isSelected: true, onClick: {})
            MiraChip(label: "Nails", isSelected: false, onClick: {})
        }
        MiraShimmerBlock(height: 56)
        MiraErrorView(message: "Something went wrong", onRetry: {})
    }
    .padding()
}
