import SwiftUI
import ComposeApp

struct ProductDetailView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    let productId: String
    @State private var quantity: Int = 1
    
    var body: some View {
        CircuitView(screen: ProductRouteProductDetail(productId: productId), navigator: navigation) { (state: ProductDetailState) in
            VStack(spacing: 0) {
                // Top App Bar
                MiraTopBar {
                    Text(state.product?.category ?? "Product")
                        .font(MiraType.headlineSmall.weight(.bold))
                } actions: {
                    Button {
                        state.eventSink(ProductDetailEventToggleWishlist())
                    } label: {
                        Image(systemName: state.isWishlisted ? "heart.fill" : "heart")
                            .foregroundColor(state.isWishlisted ? .red : MiraTheme.onSurface)
                    }
                }
                
                if state.isLoading {
                    Spacer()
                    ProgressView()
                    Spacer()
                } else if let error = state.error {
                    Spacer()
                    Text(error).foregroundColor(MiraTheme.error)
                    Spacer()
                } else if let product = state.product {
                    ScrollView {
                        VStack(alignment: .leading, spacing: 0) {
                            // Image Pager
                            TabView {
                                let resolved = ApiEndpoints.shared.resolveImageUrl(imagePath: product.imageUrl)
                                AsyncImage(url: URL(string: resolved ?? "")) { phase in
                                    switch phase {
                                    case .success(let image):
                                        image.resizable()
                                            .aspectRatio(contentMode: .fill)
                                    case .failure(let error):
                                        let _ = print("ProductDetail: Image load failed for \(resolved ?? "nil"): \(error.localizedDescription)")
                                        MiraTheme.surfaceVariant
                                    case .empty:
                                        MiraTheme.surfaceVariant
                                    @unknown default:
                                        MiraTheme.surfaceVariant
                                    }
                                }
                                .frame(maxWidth: .infinity)
                                .clipped()
                                .onAppear {
                                    if let resolved = resolved {
                                        print("ProductDetail: Attempting to load \(resolved)")
                                    }
                                }
                            }
                            .aspectRatio(1, contentMode: .fit)
                            .tabViewStyle(.page)
                            .indexViewStyle(.page(backgroundDisplayMode: .always))
                            
                            VStack(alignment: .leading, spacing: MiraTheme.spacingMedium) {
                                // Name & Stepper
                                Text(product.category)
                                    .font(MiraType.bodySmall)
                                    .foregroundColor(MiraTheme.onSurfaceVariant)
                                
                                HStack(alignment: .top) {
                                    Text(product.name)
                                        .font(MiraType.titleLarge.weight(.bold))
                                        .foregroundColor(MiraTheme.onSurface)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                }
                                
                                HStack {
                                    HStack(spacing: MiraTheme.spacingSmall) {
                                        Text("$" + String(format: "%.2f", product.price))
                                            .font(MiraType.titleSmall)
                                            .strikethrough()
                                            .foregroundColor(MiraTheme.onSurfaceVariant.opacity(0.6))
                                        
                                        Text("$" + String(format: "%.2f", product.discountedPrice))
                                            .font(MiraType.titleLarge.weight(.bold))
                                            .foregroundColor(MiraTheme.primary)
                                    }
                                    
                                    Spacer()
                                    
                                    QuantityStepperView(quantity: $quantity, stockQuantity: Int(product.stockQuantity))
                                }
                                
                                Text("Brand: Mira Salon")
                                    .font(MiraType.bodyMedium)
                                    .foregroundColor(MiraTheme.onSurfaceVariant)
                                
                                // Ratings
                                HStack(spacing: MiraTheme.spacingSmall) {
                                    RatingChip(rating: String(format: "%.1f", product.averageRating))
                                    RatingTextChip(text: "\(product.reviewCount) Ratings")
                                    RatingTextChip(text: "\(product.reviewCount) Reviews")
                                }
                                
                                // Description
                                VStack(alignment: .leading, spacing: MiraTheme.spacingSmall) {
                                    Text("Description")
                                        .font(MiraType.titleMedium.weight(.bold))
                                    Text(formattedDescription(product.description_))
                                        .font(MiraType.bodyMedium)
                                        .lineSpacing(4)
                                        .foregroundColor(MiraTheme.onSurface)
                                }
                                .padding(.top, MiraTheme.spacingMedium)
                                
                                Button {
                                    // Review trigger
                                } label: {
                                    Text("Leave a review")
                                        .font(MiraType.titleMedium.weight(.bold))
                                        .foregroundColor(MiraTheme.primary)
                                }
                                .padding(.top, MiraTheme.spacingMedium)
                                
                                // Reviews Section
                                if !state.reviews.isEmpty {
                                    VStack(alignment: .leading, spacing: MiraTheme.spacingMedium) {
                                        Text("Reviews (\(state.reviews.count))")
                                            .font(MiraType.titleMedium.weight(.bold))
                                        
                                        ForEach(state.reviews, id: \.id) { review in
                                            ReviewItemView(
                                                userName: review.userName,
                                                userAvatarUrl: review.userAvatarUrl,
                                                rating: Int(review.rating),
                                                comment: review.comment,
                                                date: Date(timeIntervalSince1970: TimeInterval(review.createdAtEpochSeconds)).formatted(date: .abbreviated, time: .omitted)
                                            )
                                            Divider().opacity(0.5)
                                        }
                                    }
                                    .padding(.top, MiraTheme.spacingLarge)
                                } else {
                                    Text("No reviews yet. Be the first to review!")
                                        .font(MiraType.bodyMedium)
                                        .foregroundColor(MiraTheme.onSurfaceVariant)
                                        .padding(.top, MiraTheme.spacingMedium)
                                }
                                
                                Spacer().frame(height: 120)
                            }
                            .padding(MiraTheme.spacingMedium)
                        }
                    }
                }
                
                // Bottom Bar
                if let product = state.product {
                    HStack {
                        VStack(alignment: .leading) {
                            Text("Total amount")
                                .font(MiraType.labelSmall)
                                .foregroundColor(MiraTheme.onSurfaceVariant)
                            Text("$" + String(format: "%.2f", product.discountedPrice * Double(quantity)))
                                .font(MiraType.titleLarge.weight(.bold))
                                .foregroundColor(MiraTheme.primary)
                        }
                        
                        Spacer()
                        
                        Button {
                            state.eventSink(ProductDetailEventAddToCart(productId: product.id, quantity: Int32(quantity)))
                        } label: {
                            HStack(spacing: MiraTheme.spacingSmall) {
                                Image(systemName: "bag")
                                Text("Order Now")
                            }
                            .font(MiraType.titleMedium.weight(.bold))
                            .foregroundColor(.white)
                            .padding(.horizontal, MiraTheme.spacingLarge + 4)
                            .padding(.vertical, MiraTheme.spacingMedium)
                            .background(MiraTheme.primary)
                            .cornerRadius(MiraTheme.radiusPromo)
                        }
                    }
                    .padding(MiraTheme.spacingMedium)
                    .background(MiraTheme.surface)
                }
            }
            .background(MiraTheme.background)
        }
    }

    private func formattedDescription(_ desc: String) -> String {
        if desc.starts(with: "{") && desc.contains("\":\"") {
            if let data = desc.data(using: .utf8),
               let dict = try? JSONSerialization.jsonObject(with: data) as? [String: String],
               let value = dict.values.first {
                return value
            }
        }
        return desc
    }
}

struct QuantityStepperView: View {
    @Binding var quantity: Int
    let stockQuantity: Int
    
    var body: some View {
        HStack(spacing: MiraTheme.spacingMedium) {
            Button {
                if quantity > 1 { quantity -= 1 }
            } label: {
                Image(systemName: "minus")
                    .foregroundColor(MiraTheme.onSurface)
                    .frame(width: MiraTheme.stepperButtonSize, height: MiraTheme.stepperButtonSize)
            }
            
            Text("\(quantity)")
                .font(MiraType.titleMedium.weight(.bold))
            
            Button {
                if quantity < stockQuantity { quantity += 1 }
            } label: {
                Image(systemName: "plus")
                    .foregroundColor(quantity < stockQuantity ? .white : MiraTheme.onSurfaceVariant)
                    .frame(width: MiraTheme.stepperButtonSize, height: MiraTheme.stepperButtonSize)
                    .background(quantity < stockQuantity ? MiraTheme.primary : MiraTheme.surfaceVariant)
                    .clipShape(Circle())
            }
            .disabled(quantity >= stockQuantity)
        }
    }
}

struct RatingTextChip: View {
    let text: String
    var body: some View {
        Text(text)
            .font(MiraType.labelSmall)
            .foregroundColor(MiraTheme.onSurfaceVariant)
            .padding(.horizontal, MiraTheme.spacingDefault)
            .padding(.vertical, MiraTheme.spacingTiny + 2)
            .background(MiraTheme.surfaceVariant)
            .cornerRadius(MiraTheme.radiusMedium)
    }
}
