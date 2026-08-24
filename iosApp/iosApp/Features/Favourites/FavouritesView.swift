import SwiftUI
import ComposeApp

/// Mirrors Android `FavouritesScreen.kt`.
struct FavouritesView: View {
    @EnvironmentObject private var navigation: CircuitNavigation

    var body: some View {
        CircuitView(screen: ProfileRouteFavourites(), navigator: navigation) { (state: FavouritesState_) in
            VStack(spacing: 0) {
                MiraTopBar {
                    Text("Favourites")
                        .font(.headline)
                        .bold()
                }

                let isEmpty = state.products.isEmpty && state.services.isEmpty

                if isEmpty {
                    EmptyStateView(
                        message: "No favourites yet",
                        description: "Start adding items to your favorites",
                        icon: "heart.fill"
                    )
                } else {
                    ScrollView {
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: MiraTheme.spacingMedium) {
                            // Products section
                            if !state.products.isEmpty {
                                FavouritesSectionHeader(title: "Products", count: state.products.count)
                                    .gridCellColumns(2)
                                
                                ForEach(state.products, id: \.id) { product in
                                    FavouriteProductCardView(product: product) {
                                        state.eventSink(FavouritesEventProductClicked(id: product.id))
                                    } onRemove: {
                                        state.eventSink(FavouritesEventRemoveProductFavorite(id: product.id))
                                    }
                                }
                            }

                            // Services section
                            if !state.services.isEmpty {
                                FavouritesSectionHeader(title: "Services", count: state.services.count)
                                    .gridCellColumns(2)
                                
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

private struct FavouritesSectionHeader: View {
    let title: String
    let count: Int
    
    var body: some View {
        HStack {
            Text(title)
                .font(.title3)
                .bold()
                .foregroundColor(MiraTheme.onSurface)
            Spacer()
            Text("\(count) items")
                .font(.subheadline)
                .foregroundColor(MiraTheme.onSurfaceVariant)
        }
        .padding(.vertical, MiraTheme.spacingSmall)
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
                    .frame(height: 180)
                    .clipped()

                    // Gradient overlay
                    LinearGradient(
                        colors: [Color.clear, Color.black.opacity(0.6)],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(height: 180)

                    Button(action: onRemove) {
                        Image(systemName: "heart.fill")
                            .foregroundColor(.red)
                            .frame(width: 36, height: 36)
                            .background(Color.white.opacity(0.95))
                            .clipShape(Circle())
                            .padding(8)
                    }
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text(product.name)
                        .font(.caption)
                        .bold()
                        .lineLimit(2)
                        .foregroundColor(.white)

                    HStack {
                        Text(Double(product.discountedPrice).miraPrice())
                            .font(.subheadline)
                            .bold()
                            .foregroundColor(MiraTheme.primary)
                        Spacer()
                        if product.discountPercent > 0 {
                            Text("-\(product.discountPercent)%")
                                .font(.system(size: 8, weight: .bold))
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(MiraTheme.primary)
                                .foregroundColor(.white)
                                .cornerRadius(6)
                        }
                    }
                }
                .padding(12)
            }
            .background(MiraTheme.surface)
            .cornerRadius(16)
            .shadow(color: Color.black.opacity(0.1), radius: 4, x: 0, y: 2)
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
                VStack(spacing: 0) {
                    // Gradient background icon area
                    ZStack {
                        LinearGradient(
                            colors: [MiraTheme.primary.opacity(0.2), MiraTheme.primary.opacity(0.4)],
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                        .frame(height: 115)
                        .cornerRadius(2)
                        
                        Image(systemName: "heart.fill")
                            .foregroundColor(MiraTheme.primary)
                            .font(.system(size: 40))
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        Text(service.name)
                            .font(.caption)
                            .bold()
                            .lineLimit(2)
                            .foregroundColor(MiraTheme.onSurface)

                        Text(Double(service.price).miraPrice())
                            .font(.subheadline)
                            .bold()
                            .foregroundColor(MiraTheme.primary)
                    }
                    .padding(.top, 8)
                }
                .padding(12)

                Button(action: onRemove) {
                    Image(systemName: "heart.fill")
                        .foregroundColor(.red)
                        .frame(width: 32, height: 32)
                        .background(Color.white.opacity(0.95))
                        .clipShape(Circle())
                }
                .padding(12)
            }
            .background(MiraTheme.primaryContainer.opacity(0.3))
            .cornerRadius(2)
        }
        .buttonStyle(.plain)
    }
}
