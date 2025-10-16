import SwiftUI
import ComposeApp

struct OrderDetailView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    let screen: CartRouteOrderDetail
    
    var body: some View {
        CircuitView(screen: screen, navigator: navigation) { (state: OrderDetailState) in
            VStack(spacing: 0) {
                MiraTopBar {
                    Text("Order Details")
                        .font(MiraType.titleLarge.weight(.bold))
                }
                
                if state.isLoading {
                    MiraLoadingView()
                } else if let error = state.error {
                    MiraErrorView(message: error)
                } else if let order = state.order {
                    ScrollView {
                        VStack(alignment: .center, spacing: 24) {
                            if state.fromCheckout {
                                VStack(spacing: 16) {
                                    Image(systemName: "checkmark.circle")
                                        .font(.system(size: 80))
                                        .foregroundColor(Color(argb: 0xFF4CAF50))
                                    
                                    Text("Order Placed Successfully!")
                                        .font(MiraType.headlineSmall.weight(.bold))
                                    
                                    Text("Thank you for your purchase.")
                                        .font(MiraType.bodyMedium)
                                        .foregroundColor(MiraTheme.onSurfaceVariant)
                                }
                                .padding(.vertical, 32)
                            }
                            
                            OrderDetailSection(title: "Order Info") {
                                MiraDetailRow(label: "Order ID", value: String(order.id.suffix(4)).uppercased())
                                MiraDetailRow(label: "Status", value: order.status.name)
                                MiraDetailRow(label: "Placed At", value: state.placedAt)
                            }
                            
                            OrderDetailSection(title: "Customer Info") {
                                MiraDetailRow(label: "Customer ID", value: String(order.userId.suffix(4)).uppercased())
                                MiraDetailRow(label: "Name", value: order.userName)
                                MiraDetailRow(label: "Email", value: order.userEmail)
                                if let userPhone = order.userPhone {
                                    MiraDetailRow(label: "Phone", value: userPhone)
                                }
                            }
                            
                            OrderDetailSection(title: "Items") {
                                ForEach(order.items, id: \.product.id) { item in
                                    VStack(alignment: .leading, spacing: 4) {
                                        HStack {
                                            Text("\(item.product.name) x\(item.quantity)")
                                                .font(MiraType.bodyMedium)
                                                .foregroundColor(MiraTheme.onSurface)
                                            Spacer()
                                            Text((Double(item.product.discountedPrice) * Double(item.quantity)).miraPrice())
                                                .font(MiraType.bodyMedium)
                                        }
                                        Text("SKU: \(String(item.product.id.suffix(4)).uppercased())")
                                            .font(MiraType.bodySmall)
                                            .foregroundColor(MiraTheme.onSurfaceVariant)
                                    }
                                    .padding(.vertical, 4)
                                }
                            }
                            
                            OrderDetailSection(title: "Payment ") {
                                MiraDetailRow(label: "Subtotal", value: Double(order.subtotal).miraPrice())
                                MiraDetailRow(label: "Shipping Fees", value: Double(order.shippingFees).miraPrice())
                                MiraDetailRow(label: "Taxes", value: Double(order.tax).miraPrice())
                                if order.discount > 0 {
                                    MiraDetailRow(label: "Discount", value: "-\(Double(order.discount).miraPrice())")
                                }
                                
                                Divider()
                                    .padding(.vertical, 8)
                                
                                HStack {
                                    Text("Total")
                                        .font(MiraType.bodyLarge.weight(.bold))
                                    Spacer()
                                    Text(Double(order.total).miraPrice())
                                        .font(MiraType.bodyLarge.weight(.bold))
                                        .foregroundColor(MiraTheme.primary)
                                }
                                
                                if let paymentMethod = order.paymentMethod {
                                    MiraDetailRow(label: "Payment Method", value: paymentMethod)
                                        .padding(.top, 8)
                                }
                                
                                if let address = order.shippingAddress {
                                    MiraDetailRow(label: "Shipping Address", value: address)
                                }
                            }
                            
                            OrderDetailSection(title: "Fulfillment") {
                                if let tracking = order.trackingNumber {
                                    MiraDetailRow(label: "Tracking Number", value: tracking)
                                }
                                if let instructions = order.specialInstructions {
                                    MiraDetailRow(label: "Special Instructions", value: instructions)
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

struct OrderDetailSection<Content: View>: View {
    let title: String
    let content: Content
    
    init(title: String, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(MiraType.titleMedium.weight(.bold))
            
            Divider()
            
            content
        }
        .padding(16)
        .background(MiraTheme.surfaceVariant.opacity(0.3))
        .cornerRadius(MiraTheme.radiusMedium)
    }
}

struct MiraDetailRow: View {
    let label: String
    let value: String
    var valueColor: Color = MiraTheme.onSurface
    
    var body: some View {
        HStack(alignment: .top) {
            Text(label)
                .font(MiraType.bodyMedium)
                .foregroundColor(MiraTheme.onSurfaceVariant)
            Spacer()
            Text(value)
                .font(MiraType.bodyMedium.weight(.medium))
                .foregroundColor(valueColor)
                .multilineTextAlignment(.trailing)
        }
        .padding(.vertical, 2)
    }
}
