import SwiftUI
import ComposeApp

struct OrdersView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    let screen: CartRouteOrders
    
    var body: some View {
        CircuitView(screen: screen, navigator: navigation) { (state: OrdersState) in
            VStack(spacing: 0) {
                MiraTopAppBar(
                    title: "Order history",
                    onBackClick: { state.eventSink(OrdersEventBack()) }
                )
                
                if state.isLoading {
                    MiraLoadingView()
                } else if let error = state.errorMessage {
                    MiraErrorView(message: error) {
                        state.eventSink(OrdersEventRetry())
                    }
                } else if state.orders.isEmpty {
                    EmptyStateView(
                        message: "You have no orders yet.",
                        description: "Once you place an order, it will appear here in your order history.",
                        icon: "doc.text.magnifyingglass"
                    )
                } else {
                    ScrollView {
                        LazyVStack(spacing: 16) {
                            ForEach(state.orders, id: \.order.id) { orderUi in
                                OrderCard(orderUi: orderUi) {
                                    state.eventSink(OrdersEventOrderClicked(orderId: orderUi.order.id))
                                }
                                .swipeActions(edge: .trailing) {
                                    Button(role: .destructive) {
                                        state.eventSink(OrdersEventRemoveOrder(orderId: orderUi.order.id))
                                    } label: {
                                        Label("Delete", systemImage: "trash")
                                    }
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

private struct OrderCard: View {
    let orderUi: OrderUiModel
    let onClick: () -> Void
    
    var body: some View {
        let order = orderUi.order
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("#\(order.id.prefix(8).uppercased())")
                        .font(MiraType.titleMedium.weight(.bold))
                        .foregroundColor(MiraTheme.onSurface)
                    
                    Text("Order placed \(orderUi.formattedDate)")
                        .font(MiraType.bodyMedium)
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                }
                
                Spacer()
                
                VStack(alignment: .trailing, spacing: 4) {
                    Text(String(format: "$%.2f", order.total))
                        .font(MiraType.titleMedium.weight(.bold))
                        .foregroundColor(MiraTheme.primary)
                    
                    StatusBadge(text: order.status.name.lowercased().capitalized)
                }
            }
            
            VStack(spacing: 8) {
                ForEach(order.items.prefix(3), id: \.product.id) { item in
                    OrderItemPreviewRow(item: item)
                }
                
                if order.items.count > 3 {
                    Text("+ \(order.items.count - 3) more items")
                        .font(MiraType.bodySmall)
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                        .frame(maxWidth: .infinity, alignment: .center)
                }
            }
            
            Button(action: onClick) {
                Text("View Details")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(MiraTheme.primary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .background(MiraTheme.primary.opacity(0.1))
                    .cornerRadius(MiraTheme.radiusSmall)
            }
            .buttonStyle(.plain)
        }
        .padding(16)
        .background(MiraTheme.surface)
        .cornerRadius(MiraTheme.radiusSmall)
    }
}

private struct OrderItemPreviewRow: View {
    let item: CartItem

    var body: some View {
        HStack(spacing: 12) {
            let resolvedUrl = ApiEndpoints.shared.resolveImageUrl(imagePath: item.product.imageUrl)
            AsyncImage(url: URL(string: resolvedUrl ?? "")) { image in
                image.resizable().aspectRatio(contentMode: .fill)
            } placeholder: {
                MiraTheme.surfaceVariant
            }
            .frame(width: 60, height: 60)
            .cornerRadius(MiraTheme.radiusSmall)

            VStack(alignment: .leading, spacing: 4) {
                Text(item.product.name)
                    .font(MiraType.bodyMedium.weight(.semibold))
                    .foregroundColor(MiraTheme.onSurface)
                    .lineLimit(1)

                Text("Qty: \(item.quantity)")
                    .font(MiraType.bodySmall)
                    .foregroundColor(MiraTheme.onSurfaceVariant)

                Text(String(format: "$%.2f", item.product.price * Double(item.quantity)))
                    .font(MiraType.bodySmall.weight(.medium))
                    .foregroundColor(MiraTheme.primary)
            }

            Spacer()
        }
        .padding(8)
        .background(MiraTheme.surfaceVariant.opacity(0.3))
        .cornerRadius(MiraTheme.radiusSmall)
    }
}

private struct StatusBadge: View {
    let text: String
    var body: some View {
        Text(text)
            .font(.system(size: 12))
            .foregroundColor(MiraTheme.onSurfaceVariant)
            .padding(.horizontal, 12)
            .padding(.vertical, 4)
            .background(MiraTheme.surface)
            .cornerRadius(MiraTheme.radiusPill)
            .overlay(
                RoundedRectangle(cornerRadius: MiraTheme.radiusPill)
                    .stroke(MiraTheme.outlineVariant.opacity(0.5), lineWidth: 1)
            )
    }
}
