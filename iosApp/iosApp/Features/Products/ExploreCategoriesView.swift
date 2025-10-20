import SwiftUI
import ComposeApp

struct ExploreCategoriesView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    
    var body: some View {
        CircuitView(screen: ProductRouteExploreCategories(), navigator: navigation) { (state: ExploreCategoriesState) in
            VStack(spacing: 0) {
                // Top App Bar
                MiraTopBar {
                    Text("Explore Categories")
                        .font(.headline)
                        .bold()
                } actions: {
                    Button {
                        state.eventSink(ExploreCategoriesEventCartClicked())
                    } label: {
                        ZStack(alignment: .topTrailing) {
                            Image(systemName: "cart")
                                .font(.system(size: 20))
                                .foregroundColor(MiraTheme.onSurface)
                            
                            if state.cartItemCount > 0 {
                                Text("\(state.cartItemCount)")
                                    .font(.system(size: 10, weight: .bold))
                                    .foregroundColor(.white)
                                    .padding(4)
                                    .background(Color.red)
                                    .clipShape(Circle())
                                    .offset(x: 4, y: -4)
                            }
                        }
                    }
                }
                
                // Filter Bar
                HStack {
                    Menu {
                        Button("All") { state.eventSink(ExploreCategoriesEventCategorySelected(category: nil)) }
                        ForEach(state.categories, id: \.id) { category in
                            Button(category.name) {
                                state.eventSink(ExploreCategoriesEventCategorySelected(category: category))
                            }
                        }
                    } label: {
                        HStack {
                            Text(state.selectedCategory?.name ?? "Category")
                            Image(systemName: "chevron.down")
                        }
                        .font(.body)
                        .foregroundColor(MiraTheme.primary)
                    }
                    
                    Spacer()
                    
                    Menu {
                        Button("All") { state.eventSink(ExploreCategoriesEventVariationSelected(variation: nil)) }
                        ForEach(ProductVariation.allCases, id: \.self) { variation in
                            Button(variation.displayName) {
                                state.eventSink(ExploreCategoriesEventVariationSelected(variation: variation))
                            }
                        }
                    } label: {
                        HStack {
                            Text(state.selectedVariation?.displayName ?? "Variations")
                            Image(systemName: "chevron.down")
                        }
                        .font(.body)
                        .foregroundColor(MiraTheme.primary)
                    }
                    
                    Spacer()
                    
                    Text("\(state.products.count) Product")
                        .font(.caption)
                        .foregroundColor(MiraTheme.primary)
                }
                .padding(20)
                
                // Grid
                if state.isLoading && !state.isRefreshing {
                    Spacer()
                    ProgressView()
                    Spacer()
                } else if state.products.isEmpty {
                    EmptyStateView(
                        message: "No products found",
                        description: "We couldn't find any products in this category. Try adjusting your filters or search query.",
                        icon: "shippingbox"
                    )
                } else {
                    ScrollView {
                        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                            ForEach(state.products, id: \.id) { product in
                                let isSale = state.promotions.contains { promo in
                                    let isTargeted = (promo.applicableServices?.count ?? 0) > 0 || (promo.applicableCategories?.count ?? 0) > 0
                                    if !isTargeted { return true }
                                    let matchesService = promo.applicableServices?.contains(product.id) ?? false
                                    let matchesCategory = promo.applicableCategories?.contains(product.category) ?? false
                                    return matchesService || matchesCategory
                                }
                                ExploreProductItemView(product: product, isSale: isSale) {
                                    state.eventSink(ExploreCategoriesEventProductClicked(productId: product.id))
                                } onAddClick: {
                                    state.eventSink(ExploreCategoriesEventAddToCart(productId: product.id))
                                }
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.bottom, 12)
                    }
                }
            }
            .background(MiraTheme.background)
        }
    }
}

struct ExploreProductItemView: View {
    let product: Product
    let isSale: Bool
    let onClick: () -> Void
    let onAddClick: () -> Void
    
    @State private var isAdded = false
    
    var body: some View {
        Button(action: onClick) {
            VStack(alignment: .leading, spacing: 8) {
                ZStack(alignment: .topLeading) {
                    AsyncImage(url: URL(string: product.imageUrl)) { image in
                        image.resizable()
                            .aspectRatio(contentMode: .fill)
                    } placeholder: {
                        MiraTheme.surfaceVariant
                    }
                    .frame(maxWidth: .infinity)
                    .frame(height: 140)
                    .cornerRadius(12)
                    
                    if isSale || product.discountPercent > 0 {
                        Text("SALE")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.red)
                            .cornerRadius(4)
                            .padding(8)
                    }

                    VStack {
                        Spacer()
                        HStack {
                            Spacer()
                            Button(action: {
                                isAdded = true
                                onAddClick()
                                DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
                                    isAdded = false
                                }
                            }) {
                                Image(systemName: isAdded ? "checkmark" : "plus")
                                    .padding(8)
                                    .background(Color.white)
                                    .foregroundColor(isAdded ? MiraTheme.primary : MiraTheme.onSurface)
                                    .clipShape(Circle())
                                    .shadow(color: .black.opacity(0.1), radius: 2)
                            }
                            .padding(8)
                        }
                    }
                }
                
                Text(product.name)
                    .font(.subheadline)
                    .bold()
                    .lineLimit(2)
                    .foregroundColor(MiraTheme.onSurface)
                
                Text(String(format: "$%.2f", product.price))
                    .font(.headline)
                    .foregroundColor(MiraTheme.primary)
            }
        }
        .buttonStyle(.plain)
    }
}
