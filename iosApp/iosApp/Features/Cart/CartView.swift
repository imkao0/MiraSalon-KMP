import SwiftUI
import ComposeApp

struct CartView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    
    var body: some View {
        CircuitView(screen: CartRouteCart(), navigator: navigation) { (state: CartState) in
            VStack(spacing: 0) {
                // Top App Bar (fixed, not scrollable)
                MiraTopBar {
                    Text("Cart (\(state.cart.itemCount))")
                        .font(.headline)
                        .bold()
                }
                
                // Scrollable content
                ScrollView {
                    VStack(spacing: 0) {
                        if state.cart.items.isEmpty && state.expiredCartItems.isEmpty && state.expiredOrders.isEmpty {
                            EmptyStateView(
                                message: "Your cart is empty",
                                description: "Looks like you haven't added anything to your cart yet. Explore our products and services to find what you need.",
                                icon: "cart"
                            )
                        } else {
                            // Active Cart Items grouped by category
                            let grouped = Dictionary(grouping: state.cart.items, by: { $0.product.category })
                            let categories = grouped.keys.sorted()

                            ForEach(categories, id: \.self) { category in
                                let items = grouped[category] ?? []
                                if !items.isEmpty {
                                    VStack(spacing: 0) {
                                        StoreHeaderView(storeName: category, items: items, selectedIds: state.selectedItemIds, eventSink: state.eventSink)
                                        ForEach(items, id: \.product.id) { item in
                                            CartItemCardView(
                                                item: item,
                                                isSelected: state.selectedItemIds.contains(item.product.id),
                                                eventSink: state.eventSink
                                            )
                                            .padding(.horizontal, 16)
                                            .padding(.vertical, 4)
                                        }
                                    }
                                }
                            }

                            // Expired Items section
                            if !state.expiredCartItems.isEmpty {
                                VStack(spacing: 0) {
                                    Text("Expired Items")
                                        .font(.title2)
                                        .bold()
                                        .foregroundColor(.black)
                                        .padding(.horizontal, 16)
                                        .padding(.vertical, 8)
                                    
                                    ForEach(state.expiredCartItems, id: \.product.id) { item in
                                        CartItemCardView(
                                            item: item,
                                            isSelected: false,
                                            eventSink: state.eventSink,
                                            isExpired: true
                                        )
                                        .padding(.horizontal, 16)
                                        .padding(.vertical, 4)
                                    }
                                }
                            }

                            // Recent Cancelled Orders section
                            if !state.expiredOrders.isEmpty {
                                VStack(spacing: 0) {
                                    Text("Recent Cancelled Orders")
                                        .font(.title2)
                                        .bold()
                                        .foregroundColor(.black)
                                        .padding(.horizontal, 16)
                                        .padding(.vertical, 8)
                                    
                                    ForEach(state.expiredOrders, id: \.id) { order in
                                        ExpiredOrderCardView(order: order) {
                                            state.eventSink(CartEventRemoveExpiredOrder(orderId: order.id))
                                        }
                                        .padding(.horizontal, 16)
                                        .padding(.vertical, 4)
                                    }
                                }
                            }
                        }
                    }
                    .padding(.bottom, 16)
                }
                .background(Color(white: 0.97))

                // Bottom Bar
                if !state.cart.items.isEmpty {
                    CartBottomBarView(
                        state: state,
                        onCheckout: { state.eventSink(CartEventCheckout()) }
                    )
                }
            }
            .background(MiraTheme.background)
        }
    }
}

private struct StoreHeaderView: View {
    let storeName: String
    let items: [CartItem]
    let selectedIds: Set<String>
    let eventSink: (CartEvent) -> Void
    
    var body: some View {
        HStack(spacing: 12) {
            let allSelected = items.allSatisfy { selectedIds.contains($0.product.id) }
            CircularCheckboxView(checked: allSelected, activeColor: MiraTheme.primary) {
                eventSink(CartEventToggleStoreSelection(storeName: storeName, isSelected: !allSelected))
            }

            Image(systemName: "house.fill")
                .foregroundColor(.black)

            Text(storeName)
                .font(.headline)
                .bold()
                .foregroundColor(.black)

            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color(white: 0.97))
    }
}

private struct CartItemCardView: View {
    let item: CartItem
    let isSelected: Bool
    let eventSink: (CartEvent) -> Void
    var isExpired: Bool = false

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .center, spacing: 12) {
                if !isExpired {
                    CircularCheckboxView(checked: isSelected, activeColor: .gray.opacity(0.3)) {
                        eventSink(CartEventToggleSelection(productId: item.product.id))
                    }
                } else {
                    Spacer().frame(width: 22)
                }
                
                HStack(alignment: .top, spacing: 12) {
                    let resolvedUrl = ApiEndpoints.shared.resolveImageUrl(imagePath: item.product.imageUrl)
                    AsyncImage(url: URL(string: resolvedUrl ?? "")) { phase in
                        switch phase {
                        case .success(let image):
                            image.resizable().aspectRatio(contentMode: .fill)
                        case .failure(let error):
                            let _ = print("CartView: Image load failed for \(resolvedUrl ?? "nil"): \(error.localizedDescription)")
                            Color.gray.opacity(0.1)
                        case .empty:
                            Color.gray.opacity(0.1)
                        @unknown default:
                            Color.gray.opacity(0.1)
                        }
                    }
                    .frame(width: 80, height: 80)
                    .cornerRadius(8)
                    .clipped()
                    .onAppear {
                        if let resolvedUrl = resolvedUrl {
                            print("CartView: Attempting to load \(resolvedUrl)")
                        }
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        Text(item.product.name)
                            .font(.subheadline)
                            .fontWeight(.medium)
                            .foregroundColor(isExpired ? .gray : .black)
                            .lineLimit(2)
                        
                        Text(item.product.description_)
                            .font(.caption)
                            .foregroundColor(.gray)
                            .lineLimit(1)
                        
                        Spacer()

                        HStack {
                            Text("$\(Int(item.product.discountedPrice))")
                                .font(.headline)
                                .bold()
                                .foregroundColor(isExpired ? .gray : MiraTheme.primary)
                            
                            Spacer()

                            if isExpired {
                                HStack(spacing: 4) {
                                    Text("EXPIRED")
                                        .font(.system(size: 10, weight: .bold))
                                }
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .foregroundColor(.red)
                                .background(Color.red.opacity(0.1))
                                .cornerRadius(4)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 4)
                                        .stroke(Color.red, lineWidth: 1)
                                )
                            } else if Int(item.quantity) > Int(item.product.stockQuantity) {
                                HStack(spacing: 4) {
                                    Text("OUT OF STOCK")
                                        .font(.system(size: 10, weight: .bold))
                                }
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .foregroundColor(.red)
                                .background(Color.red.opacity(0.1))
                                .cornerRadius(4)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 4)
                                        .stroke(Color.red, lineWidth: 1)
                                )
                            } else {
                                QuantityControlView(
                                    quantity: Int(item.quantity),
                                    onDecrease: { if item.quantity > 1 { eventSink(CartEventUpdateQuantity(productId: item.product.id, quantity: Int32(item.quantity - 1))) } },
                                    onIncrease: { eventSink(CartEventUpdateQuantity(productId: item.product.id, quantity: Int32(item.quantity + 1))) }
                                )
                            }
                        }
                    }
                }
                .padding(12)
                .background(isExpired ? Color.white.opacity(0.6) : Color.white)
                .cornerRadius(16)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 4)
        }
    }
}

private struct ExpiredOrderCardView: View {
    let order: Order
    let onDelete: () -> Void
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                CircularCheckboxView(checked: true, activeColor: MiraTheme.primary) {}
                Image(systemName: "house.fill")
                Text(order.items.first?.product.category ?? "Products")
                    .font(.headline)
                    .bold()
                Spacer()
            }

            ForEach(order.items, id: \.product.id) { item in
                HStack(alignment: .top, spacing: 12) {
                    let resolvedUrl = ApiEndpoints.shared.resolveImageUrl(imagePath: item.product.imageUrl)
                    AsyncImage(url: URL(string: resolvedUrl ?? "")) { phase in
                        switch phase {
                        case .success(let image):
                            image.resizable().aspectRatio(contentMode: .fill)
                        case .failure(let error):
                            let _ = print("ExpiredOrderCardView: Image load failed for \(resolvedUrl ?? "nil"): \(error.localizedDescription)")
                            Color.gray.opacity(0.1)
                        case .empty:
                            Color.gray.opacity(0.1)
                        @unknown default:
                            Color.gray.opacity(0.1)
                        }
                    }
                    .frame(width: 60, height: 60)
                    .cornerRadius(8)
                    .clipped()
                    .onAppear {
                        if let resolvedUrl = resolvedUrl {
                            print("ExpiredOrderCardView: Attempting to load \(resolvedUrl)")
                        }
                    }

                    VStack(alignment: .leading, spacing: 4) {
                        Text(item.product.name)
                            .font(.subheadline)
                        Text(item.product.description_)
                            .font(.caption)
                            .foregroundColor(.gray)

                        HStack {
                            VStack(alignment: .leading) {
                                Text("$\(Int(item.product.discountedPrice))")
                                    .font(.headline)
                                    .bold()
                                    .foregroundColor(.gray)
                                Text("Qty: \(Int(item.quantity))")
                                    .font(.caption)
                                    .foregroundColor(.gray)
                            }

                            Spacer()

                            Text("EXPIRED")
                                .font(.system(size: 10, weight: .bold))
                                .foregroundColor(.gray)

                            Button(action: onDelete) {
                                Image(systemName: "trash")
                                    .foregroundColor(.gray)
                            }
                        }
                    }
                }
            }
        }
        .padding(12)
        .background(Color.white.opacity(0.6))
        .cornerRadius(16)
        .padding(.horizontal, 16)
        .padding(.vertical, 4)
    }
}

private struct CircularCheckboxView: View {
    let checked: Bool
    let activeColor: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            ZStack {
                Circle()
                    .stroke(checked ? activeColor : Color.gray.opacity(0.3), lineWidth: 1)
                    .frame(width: 22, height: 22)

                if checked {
                    Circle()
                        .fill(activeColor)
                        .frame(width: 22, height: 22)
                    Image(systemName: "check")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(.white)
                }
            }
        }
        .buttonStyle(.plain)
    }
}

private struct QuantityControlView: View {
    let quantity: Int
    let onDecrease: () -> Void
    let onIncrease: () -> Void

    var body: some View {
        HStack(spacing: 8) {
            Button(action: onDecrease) {
                Image(systemName: "minus")
                    .font(.system(size: 10))
                    .frame(width: 24, height: 24)
            }
            .disabled(quantity <= 1)
            
            Text("\(quantity)")
                .font(.subheadline)
                .frame(minWidth: 20)
            
            Button(action: onIncrease) {
                Image(systemName: "plus")
                    .font(.system(size: 10))
                    .frame(width: 24, height: 24)
            }
        }
        .padding(.horizontal, 4)
        .overlay(RoundedRectangle(cornerRadius: 4).stroke(Color.gray.opacity(0.2), lineWidth: 1))
    }
}

private struct CartBottomBarView: View {
    let state: CartState
    let onCheckout: () -> Void
    
    var body: some View {
        VStack(spacing: 0) {
            Divider()
            VStack(spacing: 12) {
                // Promo Code Section
                HStack(spacing: 8) {
                    TextField("Promo Code", text: Binding(
                        get: { state.promoCode },
                        set: { state.eventSink(CartEventPromoCodeChanged(code: $0)) }
                    ))
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .autocapitalization(.allCharacters)
                    .disableAutocorrection(true)
                    .submitLabel(.done)
                    .onSubmit {
                        if !state.promoCode.isEmpty {
                            state.eventSink(CartEventApplyCoupon(code: state.promoCode))
                        }
                    }

                    Button(action: {
                        if state.cart.couponCode != nil {
                            state.eventSink(CartEventRemoveCoupon())
                        } else {
                            state.eventSink(CartEventApplyCoupon(code: state.promoCode))
                        }
                    }) {
                        Text(state.cart.couponCode != nil ? "Remove" : "Apply")
                            .font(.subheadline)
                            .bold()
                            .padding(.horizontal, 16)
                            .padding(.vertical, 8)
                            .background(state.cart.couponCode != nil ? Color.gray : MiraTheme.primary)
                            .foregroundColor(.white)
                            .cornerRadius(8)
                    }
                }

                if let error = state.error {
                    Text(error)
                        .font(.caption)
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                // Price Breakdown
                if state.cart.discountAmount > 0 {
                    HStack {
                        Text("Subtotal")
                            .font(.subheadline)
                            .foregroundColor(.gray)
                        Spacer()
                        Text("US $\(String(format: "%.2f", state.cart.subtotal))")
                            .font(.subheadline)
                    }
                    HStack {
                        Text("Discount")
                            .font(.subheadline)
                            .foregroundColor(MiraTheme.primary)
                        Spacer()
                        Text("- US $\(String(format: "%.2f", state.cart.discountAmount))")
                            .font(.subheadline)
                            .foregroundColor(MiraTheme.primary)
                    }
                }

                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Total")
                            .font(.caption)
                            .foregroundColor(.gray)
                        Text("US $\(String(format: "%.2f", state.cart.total))")
                            .font(.title3)
                            .bold()
                    }

                    Spacer()

                    Button(action: onCheckout) {
                        HStack {
                            Text("CHECKOUT")
                                .font(.headline)
                                .bold()
                            Image(systemName: "arrow.right")
                        }
                        .foregroundColor(.white)
                        .padding(.horizontal, 24)
                        .frame(height: 50)
                        .background(state.hasOutOfStockItems ? Color.gray : MiraTheme.primary)
                        .cornerRadius(8)
                    }
                    .disabled(state.hasOutOfStockItems)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .background(Color.white)
        }
    }
}

private struct EmptyCartView: View {
    var body: some View {
        VStack(spacing: 16) {
            Spacer().frame(height: 100)
            Image(systemName: "cart")
                .font(.system(size: 64))
                .foregroundColor(.gray.opacity(0.3))
            Text("Your cart is empty")
                .font(.headline)
                .foregroundColor(.gray)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }
}
import SwiftUI
import ComposeApp

struct CartCheckoutView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    let screen: any Circuit_runtime_screenScreen

    var body: some View {
        CircuitView(screen: screen, navigator: navigation) { (state: CheckoutState) in
            VStack(spacing: 0) {
                MiraTopBar {
                    Text("Checkout")
                        .font(MiraType.headlineSmall.weight(.bold))
                }

                ScrollView {
                    VStack(alignment: .leading, spacing: MiraTheme.spacingLarge) {
                        Text("Order Summary")
                            .font(MiraType.titleMedium.weight(.bold))

                        VStack(spacing: 12) {
                            ForEach(state.cart.items, id: \.product.id) { item in
                                HStack {
                                    Text("\(item.product.name) x\(item.quantity)")
                                        .font(MiraType.bodyMedium)
                                        .foregroundColor(MiraTheme.onSurfaceVariant)
                                    Spacer()
                                    Text((Double(item.product.discountedPrice) * Double(item.quantity)).miraPrice())
                                        .font(MiraType.bodyMedium)
                                }
                            }
                        }

                        Divider()

                        VStack(spacing: 8) {
                            HStack {
                                Text("Subtotal")
                                    .font(MiraType.bodyMedium)
                                    .foregroundColor(MiraTheme.onSurfaceVariant)
                                Spacer()
                                Text(Double(state.cart.subtotal).miraPrice())
                                    .font(MiraType.bodyMedium)
                            }

                            if state.cart.discountAmount > 0 {
                                HStack {
                                    Text("Discount")
                                        .font(MiraType.bodyMedium)
                                        .foregroundColor(MiraTheme.onSurfaceVariant)
                                    Spacer()
                                    Text("-\(Double(state.cart.discountAmount).miraPrice())")
                                        .font(MiraType.bodyMedium)
                                        .foregroundColor(MiraTheme.primary)
                                }
                            }

                            HStack {
                                Text("Delivery")
                                    .font(MiraType.bodyMedium)
                                    .foregroundColor(MiraTheme.onSurfaceVariant)
                                Spacer()
                                Text(Double(state.deliveryFee).miraPrice())
                                    .font(MiraType.bodyMedium)
                            }

                            Divider()
                                .padding(.vertical, 4)

                            HStack {
                                Text("Total")
                                    .font(MiraType.titleMedium.weight(.bold))
                                Spacer()
                                let finalTotal = Double(state.cart.total) + Double(state.deliveryFee)
                                Text(finalTotal.miraPrice())
                                    .font(MiraType.titleMedium.weight(.bold))
                                    .foregroundColor(MiraTheme.primary)
                            }
                        }
                    }
                    .padding()
                }

                MiraButton(
                    text: state.currentStep == .review ? "Place Order" : "Continue",
                    onClick: {
                        if state.currentStep == .review {
                            state.eventSink(CheckoutEventPlaceOrder())
                        } else if state.currentStep == .shipping {
                            state.eventSink(CheckoutEventProceedToPayment())
                        } else {
                            state.eventSink(CheckoutEventProceedToReview())
                        }
                    },
                    enabled: !state.isPlacingOrder && !state.hasOutOfStockItems,
                    isLoading: state.isPlacingOrder
                )
                .padding()
            }
            .background(MiraTheme.background)
        }
    }
}
