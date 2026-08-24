import SwiftUI
import ComposeApp

/// Mirrors Android `PaymentSuccessScreen.kt` (booking flow).
struct PaymentSuccessView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    let appointmentId: String

    private let bg = Color(red: 0xF8 / 255.0, green: 0xF9 / 255.0, blue: 0xFA / 255.0)

    var body: some View {
        CircuitView(screen: BookingRoutePaymentSuccess(appointmentId: appointmentId), navigator: navigation) { (state: PaymentSuccessState) in
            VStack(spacing: 0) {
                let booking = state.booking
                if booking == nil {
                    Spacer()
                    if state.isLoading {
                        ProgressView()
                    } else {
                        Text("Booking not found")
                            .foregroundColor(MiraTheme.onSurfaceVariant)
                    }
                    Spacer()
                } else {
                    let b = booking!
                    ScrollView {
                        VStack(spacing: 0) {
                            Spacer().frame(height: 48)

                            // Success icon
                            ZStack {
                                Circle().fill(MiraTheme.brandBlue).frame(width: 80, height: 80)
                                Image(systemName: "checkmark")
                                    .font(.system(size: 40, weight: .bold))
                                    .foregroundColor(.white)
                            }

                            Spacer().frame(height: 24)

                            Text(b.customerName.isEmpty ? "Thank you for your booking!" : "Thank you for your booking, \(b.customerName)!")
                                .font(.title2)
                                .bold()
                                .multilineTextAlignment(.center)

                            Text(MiraDateFormat.successDate(epochMillis: b.dateTime))
                                .font(.subheadline)
                                .foregroundColor(.gray)
                                .padding(.top, 8)

                            Spacer().frame(height: 32)

                            BookingSummaryCard(booking: b)

                            Spacer().frame(height: 32)

                            Text("Booking Details")
                                .font(.subheadline)
                                .bold()
                                .frame(maxWidth: .infinity, alignment: .leading)

                            Spacer().frame(height: 16)

                            VStack(spacing: 16) {
                                MiraDetailRow(label: "Time", value: MiraDateFormat.dotTime(epochMillis: b.dateTime))

                                MiraDetailRow(label: "Subtotal", value: b.subtotalAmount.miraPrice())
                                if b.discountAmount > 0 {
                                    MiraDetailRow(label: "Discount", value: "-\(b.discountAmount.miraPrice())")
                                }
                                if b.taxAmount > 0 {
                                    MiraDetailRow(label: "Taxes", value: b.taxAmount.miraPrice())
                                }
                                MiraDetailRow(label: "Amount Paid", value: b.totalAmount.miraPrice())

                                MiraDetailRow(label: "Payment Method", value: "Visa ****4325")
                                MiraDetailRow(label: "Name", value: b.customerName)
                                MiraDetailRow(label: "Email", value: b.customerEmail)
                                MiraDetailRow(label: "Status", value: "Success", valueColor: MiraTheme.brandBlue)
                            }

                            Spacer().frame(height: 32)
                        }
                        .padding(.horizontal, 24)
                    }

                    // Action buttons
                    VStack(spacing: 12) {
                        Button {
                            state.eventSink(PaymentSuccessEventViewReceipt())
                        } label: {
                            Text("E-Receipt")
                                .bold()
                                .foregroundColor(MiraTheme.primary)
                                .frame(maxWidth: .infinity)
                                .frame(height: 56)
                                .background(
                                    RoundedRectangle(cornerRadius: MiraTheme.radiusPill)
                                        .stroke(MiraTheme.primary, lineWidth: 1)
                                )
                        }

                        Button {
                            state.eventSink(PaymentSuccessEventContinue())
                        } label: {
                            Text("Continue")
                                .bold()
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .frame(height: 56)
                                .background(MiraTheme.brandBlue)
                                .cornerRadius(MiraTheme.radiusPill)
                        }
                    }
                    .padding(24)
                }
            }
            .background(MiraTheme.background.ignoresSafeArea())
        }
    }
}

private struct BookingSummaryCard: View {
    let booking: ConfirmedBooking

    var body: some View {
        HStack(spacing: 16) {
            let imageUrl = booking.specialistImageUrl ?? booking.salonImageUrl
            let resolvedUrl = ApiEndpoints.shared.resolveImageUrl(imagePath: imageUrl)
            AsyncImage(url: URL(string: resolvedUrl ?? "")) { image in
                image.resizable().aspectRatio(contentMode: .fill)
            } placeholder: {
                MiraTheme.surfaceVariant
            }
            .frame(width: 80, height: 80)
            .cornerRadius(MiraTheme.radiusCard)

            VStack(alignment: .leading, spacing: 4) {
                Text(booking.specialistName)
                    .font(.body)
                    .bold()
                    .foregroundColor(MiraTheme.onSurface)
                HStack(spacing: 4) {
                    Text(booking.salonName)
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
                    Text(booking.totalAmount.miraPrice())
                        .font(.subheadline)
                        .bold()
                        .foregroundColor(MiraTheme.onSurface)
                }
            }
        }
        .padding(.vertical, 12)
    }
}

