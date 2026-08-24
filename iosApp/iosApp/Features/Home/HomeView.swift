import SwiftUI
import ComposeApp

struct HomeView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    @State private var isCategoriesExpanded = false
    
    var body: some View {
        CircuitView(screen: BottomNavKeyHome(), navigator: navigation) { (state: SalonState) in
            VStack(spacing: 0) {
                HomeTopBar(state: state)
                
                HomeContent(state: state, isCategoriesExpanded: $isCategoriesExpanded)
            }
            .background(MiraTheme.background)
            .navigationBarHidden(true)
            .scrollContentBackground(.hidden)
        }
    }
}

// MARK: - HomeTopBar
private struct HomeTopBar: View {
    let state: SalonState
    
    var body: some View {
        MiraTopBar {
            HStack(spacing: 12) {
                // Avatar
                let avatarUrl = ApiEndpoints.shared.resolveImageUrl(imagePath: state.userAvatarUrl)
                AsyncImage(url: URL(string: avatarUrl ?? "")) { image in
                    image.resizable()
                        .aspectRatio(contentMode: .fill)
                } placeholder: {
                    MiraTheme.surfaceVariant.overlay(
                        Image(systemName: "person")
                            .foregroundColor(MiraTheme.onSurfaceVariant)
                    )
                }
                .frame(width: MiraTheme.avatarSize, height: MiraTheme.avatarSize)
                .clipShape(Circle())
                .clipped()
                
                VStack(alignment: .leading, spacing: MiraTheme.spacingTiny) {
                    Text("Hi, \(state.userName ?? "")")
                        .font(MiraType.titleMedium.weight(.bold))
                        .foregroundColor(MiraTheme.textPrimary)
                    
                    HStack(spacing: MiraTheme.spacingTiny) {
                        Image(systemName: "location")
                            .resizable()
                            .frame(width: 12, height: 12)
                            .foregroundColor(MiraTheme.textSecondary)
                        
                        Text(state.userLocation ?? "Set your location")
                            .font(MiraType.bodySmall.weight(.semibold))
                            .foregroundColor(MiraTheme.textSecondary)
                            .lineLimit(1)
                    }
                }
            }
        } actions: {
            HStack(spacing: 8) {
                // Notifications
                Button {
                    state.eventSink(SalonEventNotificationClicked())
                } label: {
                    ZStack(alignment: .topTrailing) {
                        Image(systemName: "bell")
                            .font(.system(size: 20))
                            .foregroundColor(MiraTheme.textPrimary)
                            .accessibilityLabel("Notifications")
                            .frame(width: 24, height: 24)

                        if state.inAppNotificationsEnabled && state.unreadNotificationCount > 0 {
                            Text(state.unreadNotificationCount > 5 ? "5+" : "\(state.unreadNotificationCount)")
                                .font(.system(size: 9, weight: .bold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 4)
                                .padding(.vertical, 2)
                                .background(MiraTheme.error)
                                .clipShape(Capsule())
                                .offset(x: 10, y: -8)
                                .accessibilityLabel("\(state.unreadNotificationCount) unread notifications")
                        }
                    }
                }
                
                // Favourites
                Button {
                    state.eventSink(SalonEventFavoriteClicked())
                } label: {
                    Image(systemName: "heart")
                        .font(.system(size: 20))
                        .foregroundColor(MiraTheme.textPrimary)
                }
                .accessibilityLabel("Favorites")
            }
        }
    }
}

// MARK: - HomeContent
private struct HomeContent: View {
    let state: SalonState
    @Binding var isCategoriesExpanded: Bool
    
    var body: some View {
        ScrollView {
            if state.isLoading {
                HomeShimmerView()
            } else {
                VStack(alignment: .leading, spacing: MiraTheme.spacingMedium) {
                    // Search Bar
                    MiraSearchBar(
                        query: state.searchQuery,
                        placeholder: "Search by Salons"
                    ) {
                        state.eventSink(SalonEventViewAllCategories())
                    }
                    .padding(.horizontal, MiraTheme.spacingLarge)
                    .padding(.top, MiraTheme.spacingMedium)
                    
                    // Services Section
                    VStack(alignment: .leading, spacing: MiraTheme.spacingSmall) {
                        SectionHeaderView(
                            title: "Services",
                            viewAllText: isCategoriesExpanded ? "View Less" : "View All",
                            showViewAll: state.categories.count > 8
                        ) {
                            isCategoriesExpanded.toggle()
                        }
                        
                        CategoryGridView(
                            categories: state.categories,
                            selectedCategoryId: state.selectedCategoryId,
                            isExpanded: isCategoriesExpanded
                        ) { categoryId in
                            state.eventSink(SalonEventCategorySelected(id: categoryId))
                        }
                    }
                    
                    // Promotions Carousel
                    if !state.promotions.isEmpty {
                        BannerCarousel(
                            banners: state.promotions,
                            onBannerClick: { bannerId in
                                state.eventSink(SalonEventPromotionClicked(id: bannerId))
                            },
                            onViewAll: {
                                // onViewAll removed as promotions/banners are same
                            },
                            usedPromotionIds: Set(state.usedPromotionIds.map { String($0) })
                        )
                    }
                    
                    // Specialists Section
                    VStack(alignment: .leading, spacing: MiraTheme.spacingMedium) {
                        SectionHeaderView(title: "Specialists") {
                            state.eventSink(SalonEventViewAllSpecialists())
                        }
                        
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: MiraTheme.spacingMedium) {
                                ForEach(state.specialists, id: \.id) { specialist in
                                    SpecialistCard(specialist: specialist) {
                                        state.eventSink(SalonEventSpecialistSelected(id: specialist.id))
                                    }
                                }
                            }
                            .padding(.horizontal, MiraTheme.spacingLarge)
                        }
                        .scrollTargetBehavior(.viewAligned)
                    }
                    
                    Spacer().frame(height: 32)
                }
            }
        }
        .scrollContentBackground(.hidden)
    }
}

struct HomeShimmerView: View {
    private let baseColor = MiraTheme.surfaceVariant.opacity(0.4)
    private let highlightColor = MiraTheme.surfaceVariant

    var body: some View {
        VStack(alignment: .leading, spacing: MiraTheme.spacingLarge) {
            // Search Bar Shimmer
            MiraShimmerBlock(height: MiraTheme.buttonHeight, cornerRadius: MiraTheme.radiusExtraLarge)
                .padding(.horizontal, MiraTheme.spacingLarge)
                .padding(.top, MiraTheme.spacingMedium)

            // Services Section Shimmer
            VStack(alignment: .leading, spacing: MiraTheme.spacingMedium) {
                HStack {
                    MiraShimmerBlock(width: 100, height: 20)
                    Spacer()
                    MiraShimmerBlock(width: 60, height: 20)
                }
                .padding(.horizontal, MiraTheme.spacingLarge)

                let columns = Array(repeating: GridItem(.flexible(), spacing: 12), count: 4)
                LazyVGrid(columns: columns, spacing: 16) {
                    ForEach(0..<8, id: \.self) { _ in
                        VStack(spacing: 8) {
                            MiraShimmerBlock(width: 64, height: 64, cornerRadius: 18)
                            MiraShimmerBlock(width: 50, height: 12, cornerRadius: 4)
                        }
                    }
                }
                .padding(.horizontal, MiraTheme.spacingLarge)
            }

            // Promotions Carousel Shimmer
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: MiraTheme.spacingMedium) {
                    ForEach(0..<2, id: \.self) { _ in
                        MiraShimmerBlock(width: 320, height: 180, cornerRadius: 24)
                    }
                }
                .padding(.horizontal, MiraTheme.spacingLarge)
            }

            // Offers Section Shimmer
            VStack(alignment: .leading, spacing: MiraTheme.spacingMedium) {
                MiraShimmerBlock(width: 120, height: 20)
                    .padding(.horizontal, MiraTheme.spacingLarge)

                MiraShimmerBlock(height: MiraTheme.bannerHeight, cornerRadius: 24)
                    .padding(.horizontal, MiraTheme.spacingLarge)
            }

            // Specialists Section Shimmer
            VStack(alignment: .leading, spacing: MiraTheme.spacingMedium) {
                MiraShimmerBlock(width: 120, height: 20)
                    .padding(.horizontal, MiraTheme.spacingLarge)

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: MiraTheme.spacingMedium) {
                        ForEach(0..<3, id: \.self) { _ in
                            MiraShimmerBlock(width: 160, height: 160 / 0.7, cornerRadius: MiraTheme.radiusSmall)
                        }
                    }
                    .padding(.horizontal, MiraTheme.spacingLarge)
                }
            }

            Spacer().frame(height: 32)
        }
    }
}

struct SectionHeaderView: View {
    let title: String
    var viewAllText: String = "View All"
    var showViewAll: Bool = true
    var onSeeAll: () -> Void
    
    var body: some View {
        HStack {
            Text(title)
                .font(MiraType.titleMedium.weight(.bold))
                .foregroundColor(MiraTheme.textPrimary)
            
            Spacer()
            
            if showViewAll {
                Button(action: onSeeAll) {
                    Text(viewAllText)
                        .font(MiraType.labelLarge.weight(.bold))
                        .foregroundColor(MiraTheme.primary)
                }
            }
        }
        .padding(.horizontal, MiraTheme.spacingLarge)
    }
}

struct CategoryGridView: View {
    let categories: [SalonCategory]
    let selectedCategoryId: String?
    let isExpanded: Bool
    let onCategorySelect: (String) -> Void
    
    private var itemsToShow: [SalonCategory] {
        isExpanded ? categories : Array(categories.prefix(8))
    }
    
    private let columns = Array(repeating: GridItem(.flexible(), spacing: 12), count: 4)
    
    var body: some View {
        LazyVGrid(columns: columns, spacing: 16) {
            ForEach(itemsToShow, id: \.id) { category in
                CategoryItem(
                    category: category,
                    isSelected: category.id == selectedCategoryId
                ) {
                    onCategorySelect(category.id)
                }
                .accessibilityLabel("\(category.name) category")
            }
        }
        .padding(.horizontal, MiraTheme.spacingLarge)
        .padding(.top, MiraTheme.spacingSmall)
    }
}

struct CategoryItem: View {
    let category: SalonCategory
    let isSelected: Bool
    let onClick: () -> Void
    
    private var backgroundColor: Color {
        isSelected ? MiraTheme.primary : MiraTheme.surfaceVariant.opacity(0.3)
    }
    
    private var contentColor: Color {
        isSelected ? .white : MiraTheme.primary
    }
    
    private var labelColor: Color {
        isSelected ? MiraTheme.onPrimary : MiraTheme.textPrimary
    }
    
    var body: some View {
        Button(action: onClick) {
            VStack(spacing: 0) {
                // Icon Container: Square with rounded corners (18pt)
                ZStack {
                    RoundedRectangle(cornerRadius: 18)
                        .fill(isSelected ? Color.clear : MiraTheme.surfaceVariant.opacity(0.3))
                        .frame(width: 64, height: 64)

                    Image(systemName: categoryIconName(for: category.name))
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 40, height: 40)
                        .foregroundColor(contentColor)
                }
                .padding(.top, 8)
                
                Text(category.name)
                    .font(.system(size: 13, weight: isSelected ? .heavy : .semibold))
                    .foregroundColor(labelColor)
                    .lineLimit(1)
                    .padding(.top, 10)
                    .padding(.bottom, 8)
            }
            .frame(maxWidth: .infinity)
            .aspectRatio(0.75, contentMode: .fit)
            .background(backgroundColor)
            .cornerRadius(20)
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
    }
    
    private func categoryIconName(for name: String) -> String {
        let k = name.lowercased()
            .replacingOccurrences(of: "content_cut", with: "haircut")
            .replacingOccurrences(of: "palette", with: "coloring")
            .replacingOccurrences(of: "back_hand", with: "nails")
            .replacingOccurrences(of: "brush", with: "makeup")
            .replacingOccurrences(of: "face", with: "styling")
            .trimmingCharacters(in: .whitespacesAndNewlines)

        if k.contains("nail") || k.contains("manicure") || k.contains("pedicure") { return "hand.raised.fill" }
        if k.contains("makeup") || k.contains("make-up") { return "sparkles" }
        if k.contains("wax") || k.contains("thread") || k.contains("removal") { return "eraser.fill" }
        if k.contains("skin") || k.contains("facial") || k.contains("spa") || k.contains("massage") { return "flower.fill" }
        if k.contains("color") || k.contains("dye") { return "paintpalette.fill" }
        if k.contains("hair") || k.contains("cut") || k.contains("styl") || k.contains("barber") || k.contains("braid") { return "scissors" }
        
        return "square.grid.2x2.fill"
    }
}

// MARK: - BannerCarousel
struct BannerCarousel: View {
    let banners: [Promotion]
    let onBannerClick: (String) -> Void
    let onViewAll: () -> Void
    let usedPromotionIds: Set<String>

    @State private var currentIndex: Int = 0
    @State private var timer: Timer?

    var body: some View {
        VStack(alignment: .leading, spacing: MiraTheme.spacingMedium) {
            SectionHeaderView(title: "Offers", showViewAll: false, onSeeAll: onViewAll)

            VStack(spacing: MiraTheme.spacingSmall) {
                ScrollViewReader { proxy in
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: MiraTheme.spacingMedium) {
                            ForEach(banners, id: \.id) { promo in
                                OfferCard(
                                    promotion: promo,
                                    isUsed: promo.id != nil && usedPromotionIds.contains(promo.id!)
                                ) {
                                    onBannerClick(promo.id ?? "")
                                }
                                .id(promo.id)
                            }
                        }
                        .padding(.horizontal, MiraTheme.spacingLarge)
                    }
                    .onAppear {
                        startTimer(proxy: proxy)
                    }
                    .onDisappear {
                        stopTimer()
                    }
                }

                // Indicators
                if banners.count > 1 {
                    HStack(spacing: 6) {
                        ForEach(0..<banners.count, id: \.self) { index in
                            Circle()
                                .fill(currentIndex == index ? MiraTheme.primary : MiraTheme.surfaceVariant)
                                .frame(width: currentIndex == index ? 8 : 6, height: currentIndex == index ? 8 : 6)
                                .animation(.spring(), value: currentIndex)
                        }
                    }
                    .frame(maxWidth: .infinity)
                }
            }
        }
    }

    private func startTimer(proxy: ScrollViewProxy) {
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 3.0, repeats: true) { _ in
            if banners.count > 1 {
                withAnimation(.easeInOut(duration: 0.8)) {
                    currentIndex = (currentIndex + 1) % banners.count
                    if let id = banners[currentIndex].id {
                        proxy.scrollTo(id, anchor: .center)
                    }
                }
            }
        }
    }

    private func stopTimer() {
        timer?.invalidate()
        timer = nil
    }
}
