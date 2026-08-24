import SwiftUI
import ComposeApp

struct ServicesDiscoveryView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    let categoryId: String?
    
    var body: some View {
        CircuitView(screen: ServiceRouteServices(categoryId: categoryId), navigator: navigation) { (state: ServicesState) in
            VStack(spacing: 0) {
                // Top App Bar
                MiraTopBar {
                    Text("Services")
                        .font(.headline)
                        .bold()
                } actions: {
                    if !state.services.isEmpty {
                        Button {
                            state.eventSink(ServicesEventToggleSortOrder())
                        } label: {
                            Image(systemName: "arrow.up.arrow.down")
                                .foregroundColor(MiraTheme.onSurface)
                        }
                    }
                }
                
                // Search & Filter
                if !state.isCategoryFixed {
                    VStack(spacing: 16) {
                        HStack {
                            TextField("Search services...", text: Binding(
                                get: { state.searchQuery },
                                set: { state.eventSink(ServicesEventSearchQueryChanged(query: $0)) }
                            ))
                            .padding(.leading, 16)

                            Image(systemName: "magnifyingglass")
                                .foregroundColor(MiraTheme.onSurfaceVariant)
                                .padding(.trailing, 16)
                        }
                        .frame(height: 56)
                        .background(MiraTheme.surfaceVariant.opacity(0.5))
                        .cornerRadius(28)
                        .padding(.horizontal, 16)
                        
                        if !state.categories.isEmpty {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 12) {
                                    ServiceCategoryChip(
                                        label: "All",
                                        isSelected: state.selectedCategoryId == nil
                                    ) {
                                        state.eventSink(ServicesEventCategorySelected(categoryId: nil))
                                    }

                                    ForEach(state.categories, id: \.id) { category in
                                        ServiceCategoryChip(
                                            label: category.name,
                                            isSelected: category.id == state.selectedCategoryId
                                        ) {
                                            state.eventSink(ServicesEventCategorySelected(categoryId: category.id))
                                        }
                                    }
                                }
                                .padding(.horizontal, 16)
                            }
                        }
                    }
                    .padding(.vertical, 8)
                } else {
                    if let category = state.categories.first(where: { $0.id == state.selectedCategoryId }) {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Featured")
                                .font(.subheadline)
                                .bold()
                                .foregroundColor(MiraTheme.primary)

                            HStack {
                                Text("\(category.name) Services")
                                    .font(.system(size: 32, weight: .bold))
                                    .foregroundColor(MiraTheme.onSurface)

                                Image(systemName: "sparkles")
                                    .foregroundColor(MiraTheme.primary)
                                    .font(.title)
                            }

                            Text("Handpicked services just for you")
                                .font(.subheadline)
                                .foregroundColor(MiraTheme.onSurfaceVariant)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 24)

                        if !state.subCategories.isEmpty {
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    SubCategoryChipView(
                                        label: "All",
                                        isSelected: state.selectedSubCategory == nil
                                    ) {
                                        state.eventSink(ServicesEventSubCategorySelected(subCategory: nil))
                                    }

                                    ForEach(state.subCategories, id: \.self) { subCat in
                                        SubCategoryChipView(
                                            label: subCat,
                                            isSelected: state.selectedSubCategory == subCat
                                        ) {
                                            state.eventSink(ServicesEventSubCategorySelected(subCategory: subCat))
                                        }
                                    }
                                }
                                .padding(.horizontal, 16)
                                .padding(.bottom, 16)
                            }
                        }
                    }
                }
                
                // List or States
                if state.isLoading {
                    ServicesDiscoveryShimmerView()
                } else if let error = state.error {
                    Spacer()
                    VStack {
                        Text(error).foregroundColor(MiraTheme.error)
                        Button("Retry") { state.eventSink(ServicesEventRetry()) }
                    }
                    Spacer()
                } else if state.services.isEmpty {
                    let isSearchActive = !state.searchQuery.isEmpty || state.selectedCategoryId != nil
                    if isSearchActive {
                        EmptyStateView(
                            message: "No services found",
                            description: "Try a different search",
                            icon: "magnifyingglass"
                        )
                    } else {
                        Spacer()
                    }
                } else {
                    ScrollView {
                        LazyVStack(spacing: 16) {
                            ForEach(state.services, id: \.id) { service in
                                let category = state.categories.first(where: { $0.id == service.categoryId })
                                ServiceRowCard(
                                    service: service,
                                    categoryName: category?.name
                                ) {
                                    state.eventSink(ServicesEventServiceClicked(serviceId: service.id))
                                }
                            }
                        }
                        .padding(16)
                    }
                }
            }
            .background(MiraTheme.background)
        }
    }
}

struct ServicesDiscoveryShimmerView: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                ForEach(0..<6, id: \.self) { _ in
                    HStack(spacing: 16) {
                        MiraShimmerBlock(width: 140, height: 140, cornerRadius: 2)
                        VStack(alignment: .leading, spacing: 12) {
                            MiraShimmerBlock(width: 150, height: 20, cornerRadius: 4)
                            MiraShimmerBlock(width: 200, height: 40, cornerRadius: 4)
                            Spacer()
                            HStack {
                                MiraShimmerBlock(width: 60, height: 24, cornerRadius: 4)
                                Spacer()
                                MiraShimmerBlock(width: 80, height: 32, cornerRadius: 16)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                    .padding(12)
                    .background(MiraTheme.surface)
                    .cornerRadius(2)
                }
            }
            .padding(16)
        }
    }
}

struct SubCategoryChipView: View {
    let label: String
    let isSelected: Bool
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            Text(label)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(isSelected ? MiraTheme.onPrimary : MiraTheme.onSurfaceVariant)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(isSelected ? MiraTheme.primary : MiraTheme.surfaceVariant.opacity(0.3))
                .cornerRadius(8)
        }
        .buttonStyle(.plain)
    }
}

struct ServiceCategoryChip: View {
    let label: String
    let isSelected: Bool
    let onClick: () -> Void
    
    var body: some View {
        Button(action: onClick) {
            Text(label)
                .font(.system(size: 14, weight: isSelected ? .bold : .medium))
                .foregroundColor(isSelected ? MiraTheme.onPrimary : MiraTheme.onSurfaceVariant)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(isSelected ? MiraTheme.primary : MiraTheme.surfaceVariant.opacity(0.3))
                .cornerRadius(20)
        }
        .buttonStyle(.plain)
    }
}

struct ServiceRowCard: View {
    let service: Service
    let categoryName: String?
    let onClick: () -> Void
    
    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 16) {
                // Image
                ZStack(alignment: .topTrailing) {
                    AsyncImage(url: URL(string: service.imageUrl ?? "")) { image in
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(width: 140, height: 140)
                    } placeholder: {
                        Color(MiraTheme.surfaceVariant.opacity(0.3))
                            .frame(width: 140, height: 140)
                    }
                    .frame(width: 140, height: 140)
                    .cornerRadius(2)
                    .clipped()
                    
                    if service.imageUrl == nil || service.imageUrl?.isEmpty == true {
                        Image(systemName: "spa")
                            .font(.system(size: 48))
                            .foregroundColor(MiraTheme.onSurfaceVariant.opacity(0.2))
                            .frame(width: 140, height: 140)
                    }
                    
                    Button {
                        // Favorite toggle
                    } label: {
                        Image(systemName: "heart")
                            .font(.system(size: 14))
                            .foregroundColor(MiraTheme.primary)
                            .padding(8)
                            .background(Color.white.opacity(0.8))
                            .clipShape(Circle())
                    }
                    .padding(4)
                }

                // Info
                VStack(alignment: .leading, spacing: 4) {
                    Text(service.name)
                        .font(.headline)
                        .bold()
                        .foregroundColor(MiraTheme.onSurface)
                        .lineLimit(1)

                    Text(service.description_)
                        .font(.caption)
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                    
                    Spacer(minLength: 8)

                    HStack {
                        Text("$" + String(format: "%.0f", service.discountedPrice))
                            .font(.title3)
                            .fontWeight(.black)
                            .foregroundColor(MiraTheme.onSurface)

                        Spacer()
                        
                        Text("Book Now")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(MiraTheme.primary)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(MiraTheme.primary.opacity(0.2))
                            .cornerRadius(20)
                    }
                }
                .padding(.vertical, 4)
            }
            .padding(12)
            .background(MiraTheme.surface)
            .cornerRadius(2)
        }
        .buttonStyle(.plain)
    }
}
