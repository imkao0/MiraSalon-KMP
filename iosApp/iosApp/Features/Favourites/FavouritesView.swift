import SwiftUI
import ComposeApp

/// Mirrors Android `FavouritesScreen.kt`.
struct FavouritesView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    @State private var selectedTab = 0

    var body: some View {
        CircuitView(screen: ProfileRouteFavourites(), navigator: navigation) { (state: FavouritesState_) in
            VStack(spacing: 0) {
                MiraTopBar {
                    Text("Favourites")
                        .font(.headline)
                        .bold()
                }

                // Tab Row
                HStack(spacing: 0) {
                    TabItem(title: "Products", isSelected: selectedTab == 0) { selectedTab = 0 }
                    TabItem(title: "Services", isSelected: selectedTab == 1) { selectedTab = 1 }
                }
                .padding(.vertical, 8)
                .background(MiraTheme.background)

                let isEmpty = selectedTab == 0 ? state.products.isEmpty : state.services.isEmpty

                if isEmpty {
                    EmptyStateView(
                        message: "No favourites yet",
                        description: "Start adding items to your favorites",
                        icon: "heart.fill"
                    )
                } else {
                    ScrollView {
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: MiraTheme.spacingMedium) {
                            if selectedTab == 0 {
                                ForEach(state.products, id: \.id) { product in
                                    FavouriteProductCardView(product: product) {
                                        state.eventSink(FavouritesEventProductClicked(id: product.id))
                                    } onRemove: {
                                        state.eventSink(FavouritesEventRemoveProductFavorite(id: product.id))
                                    }
                                }
                            } else {
                                ForEach(state.services, id: \.id) { service in
                                    FavouriteServiceCardView(service: service) {
                                        state.eventSink(FavouritesEventServiceClicked(id: service.id))
                                    } onRemove: {
                                        state.eventSink(FavouritesEventRemoveServiceFavorite(id: service.id))
                                    }
                                }
                            }
                        }
                        .padding(MiraTheme.spacingMedium)
                    }
                }
            }
            .background(MiraTheme.background.ignoresSafeArea())
        }
    }
}

private struct TabItem: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Text(title)
                    .font(.system(size: 15, weight: isSelected ? .bold : .regular))
                    .foregroundColor(isSelected ? MiraTheme.primary : MiraTheme.onSurfaceVariant)
                Rectangle()
                    .fill(isSelected ? MiraTheme.primary : Color.clear)
                    .frame(height: 3)
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.plain)
    }
}

private struct FavouriteProductCardView: View {
    let product: Product
    let onClick: () -> Void
    let onRemove: () -> Void

    var body: some View {
        Button(action: onClick) {
            VStack(alignment: .leading, spacing: 0) {
                ZStack(alignment: .topTrailing) {
                    let resolvedUrl = ApiEndpoints.shared.resolveImageUrl(imagePath: product.imageUrl)
                    AsyncImage(url: URL(string: resolvedUrl ?? "")) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        MiraTheme.surfaceVariant
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: MiraTheme.cardImageHeight)
                    .clipped()

                    Button(action: onRemove) {
                        Image(systemName: "heart.fill")
                            .foregroundColor(.red)
                            .padding(MiraTheme.spacingSmall)
                            .background(Color.white.opacity(0.9))
                            .clipShape(Circle())
                            .padding(MiraTheme.spacingSmall)
                    }
                }

                VStack(alignment: .leading, spacing: MiraTheme.spacingTiny) {
                    Text(product.name)
                        .font(.caption)
                        .bold()
                        .lineLimit(2)
                        .foregroundColor(MiraTheme.onSurface)

                    HStack {
                        Text(Double(product.discountedPrice).miraPrice())
                            .font(.subheadline)
                            .bold()
                            .foregroundColor(MiraTheme.primary)
                        Spacer()
                        Text("SALE")
                            .font(.system(size: 8, weight: .bold))
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(MiraTheme.primaryContainer)
                            .foregroundColor(MiraTheme.onPrimaryContainer)
                            .cornerRadius(4)
                    }
                }
                .padding(MiraTheme.spacingDefault)
            }
            .background(MiraTheme.surface)
            .cornerRadius(MiraTheme.radiusMedium)
            .miraElevation(MiraTheme.elevationLow)
        }
        .buttonStyle(.plain)
    }
}

private struct FavouriteServiceCardView: View {
    let service: Service
    let onClick: () -> Void
    let onRemove: () -> Void

    var body: some View {
        Button(action: onClick) {
            ZStack(alignment: .topTrailing) {
                VStack(spacing: MiraTheme.spacingSmall) {
                    ZStack {
                        Circle().fill(MiraTheme.primary.opacity(0.1)).frame(width: MiraTheme.iconSizeLarge, height: MiraTheme.iconSizeLarge)
                        Image(systemName: "heart.fill").foregroundColor(MiraTheme.primary)
                    }

                    Text(service.name)
                        .font(.caption)
                        .bold()
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                        .foregroundColor(MiraTheme.onSurface)

                    Text(Double(service.price).miraPrice())
                        .font(.subheadline)
                        .bold()
                        .foregroundColor(MiraTheme.primary)
                }
                .padding(MiraTheme.spacingMedium)
                .frame(maxWidth: .infinity)
                .frame(height: 140)
                .background(MiraTheme.primaryContainer.opacity(0.1))
                .cornerRadius(MiraTheme.radiusMedium)

                Button(action: onRemove) {
                    Image(systemName: "heart.fill")
                        .foregroundColor(.red)
                        .padding(6)
                        .background(Color.white.opacity(0.9))
                        .clipShape(Circle())
                        .padding(MiraTheme.spacingSmall)
                }
            }
        }
        .buttonStyle(.plain)
    }
}
