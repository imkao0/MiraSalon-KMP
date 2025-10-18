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
                        TextField("Search services...", text: Binding(
                            get: { state.searchQuery },
                            set: { state.eventSink(ServicesEventSearchQueryChanged(query: $0)) }
                        ))
                        .padding(.horizontal, 16)
                        .frame(height: 56)
                        .background(MiraTheme.surfaceVariant)
                        .cornerRadius(8)
                        .padding(.horizontal, 16)
                        
                        if !state.categories.isEmpty {
                            let columns = Array(repeating: GridItem(.flexible(), spacing: 12), count: 4)
                            LazyVGrid(columns: columns, spacing: 16) {
                                ServiceCategoryCard(
                                    label: "All",
                                    isSelected: state.selectedCategoryId == nil
                                ) {
                                    state.eventSink(ServicesEventCategorySelected(categoryId: nil))
                                }
                                
                                ForEach(state.categories, id: \.id) { category in
                                    ServiceCategoryCard(
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
                    .padding(.vertical, 8)
                } else {
                    if let category = state.categories.first(where: { $0.id == state.selectedCategoryId }) {
                        Text(category.name)
                            .font(.title2)
                            .bold()
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(16)
                    }
                }
                
                // Grid or States
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
                        // Plain screen
                        Spacer()
                    }
                } else {
                    ScrollView {
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                            ForEach(state.services, id: \.id) { service in
                                let category = state.categories.first(where: { $0.id == service.categoryId })
                                let isSale = state.promotions.contains { promo in
                                    let isTargeted = (promo.applicableServices?.count ?? 0) > 0 || (promo.applicableCategories?.count ?? 0) > 0
                                    if !isTargeted { return true }
                                    let matchesService = promo.applicableServices?.contains(service.id) ?? false
                                    let matchesCategory = promo.applicableCategories?.contains(category?.name ?? "") ?? false
                                    return matchesService || matchesCategory
                                }
                                ServiceGridCard(
                                    service: service,
                                    categoryName: category?.name,
                                    isSale: isSale
                                ) {
                                    state.eventSink(ServicesEventServiceClicked(serviceId: service.id))
                                }
                            }
                        }
                        .padding(12)
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
            VStack(alignment: .leading, spacing: 24) {
                // Categories row shimmer
                HStack(spacing: 12) {
                    ForEach(0..<4, id: \.self) { _ in
                        MiraShimmerBlock(height: 80, cornerRadius: 20)
                    }
                }
                .padding(.horizontal, 16)

                // Grid shimmer
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    ForEach(0..<6, id: \.self) { _ in
                        VStack(alignment: .leading, spacing: 12) {
                            HStack {
                                MiraShimmerBlock(width: 20, height: 20)
                                Spacer()
                                MiraShimmerBlock(width: 60, height: 10)
                            }
                            MiraShimmerBlock(height: 48)
                            HStack {
                                MiraShimmerBlock(width: 80, height: 20)
                                Spacer()
                                MiraShimmerBlock(width: 40, height: 15)
                            }
                        }
                        .padding(16)
                        .background(
                            RoundedRectangle(cornerRadius: 16)
                                .fill(MiraTheme.surfaceVariant.opacity(0.3))
                        )
                        .shadow(color: MiraTheme.primary.opacity(0.1), radius: 8, x: 0, y: 4)
                    }
                }
                .padding(12)
            }
        }
    }
}

struct ServiceCategoryCard: View {
    let label: String
    let isSelected: Bool
    let onClick: () -> Void
    
    var body: some View {
        Button(action: onClick) {
            Text(label)
                .font(.system(size: 14, weight: isSelected ? .bold : .medium))
                .foregroundColor(isSelected ? MiraTheme.onPrimary : MiraTheme.textPrimary)
                .lineLimit(1)
                .padding(.horizontal, 8)
                .frame(height: 44)
                .frame(maxWidth: .infinity)
                .background(isSelected ? MiraTheme.primary : MiraTheme.surfaceVariant.opacity(0.3))
                .cornerRadius(12)
        }
        .buttonStyle(.plain)
    }
}

struct ServiceGridCard: View {
    let service: Service
    let categoryName: String?
    let isSale: Bool
    let onClick: () -> Void
    
    var body: some View {
        Button(action: onClick) {
            GeometryReader { geometry in
                let cardWidth = geometry.size.width
                let cardHeight = cardWidth * 0.75
                
                ZStack(alignment: .bottomLeading) {
                    // Background Image
                    AsyncImage(url: URL(string: service.imageUrl ?? "")) { image in
                        image
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(width: cardWidth, height: cardHeight)
                    } placeholder: {
                        Color(red: 0xF1/255.0, green: 0xF3/255.0, blue: 0xF4/255.0)
                            .frame(width: cardWidth, height: cardHeight)
                    }
                    .frame(width: cardWidth, height: cardHeight)
                    .clipped()
                    
                    // Icon placeholder if no image
                    if service.imageUrl == nil || service.imageUrl?.isEmpty == true {
                        Image(systemName: "spa")
                            .font(.system(size: 48))
                            .foregroundColor(MiraTheme.onSurfaceVariant.opacity(0.2))
                            .frame(width: cardWidth, height: cardHeight)
                    }
                    
                    // Sale Badge
                    if isSale || service.discountPercent > 0 {
                        Text("SALE")
                            .font(.caption)
                            .bold()
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color(red: 0xEF/255.0, green: 0x44/255.0, blue: 0x44/255.0))
                            .cornerRadius(MiraTheme.radiusCard)
                            .padding(16)
                            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
                    }
                    
                    // Gradient Overlay
                    LinearGradient(
                        gradient: Gradient(colors: [
                            .clear,
                            MiraTheme.surface.opacity(0.7),
                            MiraTheme.surface
                        ]),
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .frame(width: cardWidth, height: cardHeight)
                    
                    // Info Overlay
                    VStack(alignment: .leading, spacing: 12) {
                        Text(service.name)
                            .font(.headline)
                            .bold()
                            .foregroundColor(MiraTheme.onSurface)
                            .lineLimit(1)
                        
                        Text(categoryName ?? "")
                            .font(.caption)
                            .foregroundColor(MiraTheme.onSurfaceVariant)
                        
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                if service.discountPercent > 0 {
                                    Text("$" + String(format: "%.2f", service.price))
                                        .font(.caption)
                                        .strikethrough()
                                        .foregroundColor(MiraTheme.onSurfaceVariant.opacity(0.6))
                                }
                                Text("$" + String(format: "%.2f", service.discountedPrice))
                                    .font(.title2)
                                    .bold()
                                    .foregroundColor(MiraTheme.primary)
                            }
                            
                            Spacer()
                            
                            Text("\(service.durationMinutes)m")
                                .font(.caption)
                                .bold()
                                .foregroundColor(MiraTheme.primary)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(MiraTheme.primaryContainer.opacity(0.5))
                                .cornerRadius(8)
                        }
                    }
                    .padding(20)
                }
            }
            .aspectRatio(0.75, contentMode: .fit)
            .background(Color(red: 0xF8/255.0, green: 0xF9/255.0, blue: 0xFA/255.0))
            .cornerRadius(MiraTheme.radiusCard)
        }
        .buttonStyle(.plain)
    }
}
