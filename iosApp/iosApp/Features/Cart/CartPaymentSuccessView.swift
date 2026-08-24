import SwiftUI
import ComposeApp

struct CartPaymentSuccessView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    let screen: CartRoutePaymentSuccess

    private let bg = Color(red: 0xF8 / 255.0, green: 0xF9 / 255.0, blue: 0xFA / 255.0)

    var body: some View {
        CircuitView(screen: screen, navigator: navigation) { (state: CartPaymentSuccessState) in
            VStack(spacing: 0) {
                // Top bar with back button
                HStack {
                    Button(action: {
                        state.eventSink(CartPaymentSuccessEventBack())
                    }) {
                        Image(systemName: "chevron.left")
                            .font(.title2)
                            .foregroundColor(MiraTheme.onSurface)
                    }
                    Spacer()
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
                if state.isLoading {
                    Spacer()
                    ProgressView()
                    Spacer()
                } else if let order = state.order {
                    ScrollView {
                        VStack(spacing: 0) {
                            Spacer().frame(height: 48)

                            // Success icon
                            ZStack {
                                Circle().fill(MiraTheme.brandBlue).frame(width: 80, height: 80)
                                Image(systemName: "checkmark")
                                    .font(.system(size: 40, weight: .bold))
                                    .foregroundColor(MiraTheme.onPrimary)
                            }

                            Spacer().frame(height: 24)

                            Text(!order.userName.isEmpty ? "Thank you for your order, \(order.userName)!" : "Thank you for your order!")
                                .font(.title2)
                                .bold()
                                .multilineTextAlignment(.center)
                                .foregroundColor(MiraTheme.onBackground)

                            Text(MiraDateFormat.successDate(epochMillis: order.placedAtEpochSeconds * 1000))
                                .font(.subheadline)
                                .foregroundColor(MiraTheme.onSurfaceVariant)
                                .padding(.top, 8)

                            Spacer().frame(height: 12)

                            Text("Your order will be shipped within 24 hours or 1-2 business days")
                                .font(.caption)
                                .foregroundColor(MiraTheme.onSurfaceVariant)
                                .multilineTextAlignment(.center)

                            Spacer().frame(height: 32)

                            OrderSummaryCard(order: order)

                            Spacer().frame(height: 32)

                            Text("Order Details")
                                .font(.subheadline)
                                .bold()
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .foregroundColor(MiraTheme.onBackground)

                            Spacer().frame(height: 16)

                            VStack(spacing: 16) {
                                MiraDetailRow(label: "Order ID", value: String(order.id.prefix(8)).uppercased())
                                MiraDetailRow(label: "Subtotal", value: Double(order.subtotal).miraPrice())
                                if order.discount > 0 {
                                    MiraDetailRow(label: "Discount", value: "-\(Double(order.discount).miraPrice())")
                                }
                                if order.shippingFees > 0 {
                                    MiraDetailRow(label: "Delivery", value: Double(order.shippingFees).miraPrice())
                                }

                                let total = order.total > 0 ? order.total : (order.subtotal + order.tax + order.shippingFees - order.discount)
                                MiraDetailRow(label: "Amount Paid", value: Double(total).miraPrice())

                                MiraDetailRow(label: "Payment Method", value: order.paymentMethod ?? "Card")
                                MiraDetailRow(label: "Name", value: order.userName ?? "Guest")
                                MiraDetailRow(label: "Email", value: order.userEmail ?? "")
                                MiraDetailRow(label: "Status", value: "Success", valueColor: MiraTheme.brandBlue)
                            }

                            Spacer().frame(height: 32)
                        }
                        .padding(.horizontal, 24)
                    }

                    // Action button
                    VStack(spacing: 12) {
                        MiraButton(text: "View My Orders") {
                            state.eventSink(CartPaymentSuccessEventContinue())
                        }
                    }
                    .padding(24)
                } else {
                    Spacer()
                    Text("Order not found")
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                    Spacer()
                }
            }
            .background(MiraTheme.background.ignoresSafeArea())
        }
    }
}

private struct OrderSummaryCard: View {
    let order: Order

    var body: some View {
        HStack(spacing: 16) {
            let firstItem = order.items.first
            let resolvedUrl = ApiEndpoints.shared.resolveImageUrl(imagePath: firstItem?.product.imageUrl)
            AsyncImage(url: URL(string: resolvedUrl ?? "")) { image in
                image.resizable().aspectRatio(contentMode: .fill)
            } placeholder: {
                MiraTheme.surfaceVariant
            }
            .frame(width: 80, height: 80)
            .cornerRadius(MiraTheme.radiusCard)

            VStack(alignment: .leading, spacing: 4) {
                let title = order.items.count > 1 ? "\(firstItem?.product.name ?? "Item") + \(order.items.count - 1) more" : (firstItem?.product.name ?? "Items")
                Text(title)
                    .font(.body)
                    .bold()
                    .foregroundColor(MiraTheme.onSurface)
                HStack(spacing: 4) {
                    Text("Mira Salon Store")
                        .font(.caption)
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 14))
                        .foregroundColor(MiraTheme.brandBlue)
                }
                Spacer().frame(height: 4)
                HStack {
                    Text("Amount Paid")
                        .font(.caption2)
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                    Spacer()
                    let total = order.total > 0 ? order.total : (order.subtotal + order.tax + order.shippingFees - order.discount)
                    Text(Double(total).miraPrice())
                        .font(.subheadline)
                        .bold()
                        .foregroundColor(MiraTheme.onSurface)
                }
            }
        }
        .padding(.vertical, 12)
    }
}

