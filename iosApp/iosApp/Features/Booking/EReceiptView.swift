import SwiftUI
import ComposeApp

/// Mirrors Android `EReceiptScreen.kt`.
struct EReceiptView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    let appointmentId: String

    var body: some View {
        CircuitView(screen: BookingRouteEReceipt(appointmentId: appointmentId), navigator: navigation) { (state: EReceiptState) in
            VStack(spacing: 0) {
                if let success = state as? EReceiptStateSuccess {
                    successView(success)
                } else if let error = state as? EReceiptStateError {
                    errorView(error)
                } else {
                    loadingView()
                }
            }
            .background(MiraTheme.background.ignoresSafeArea())
        }
    }

    @ViewBuilder
    private func successView(_ state: EReceiptStateSuccess) -> some View {
        let booking = state.booking
        VStack(spacing: 0) {
            MiraTopBar {
                Text("E-Receipt")
                    .font(.headline)
                    .bold()
            } navigationIcon: {
                Button { state.eventSink(EReceiptEventCloseClicked()) } label: {
                    Image(systemName: "xmark")
                        .foregroundColor(MiraTheme.onBackground)
                }
            }

            ScrollView {
                VStack(spacing: 0) {
                    Spacer().frame(height: 24)

                    MiraQRCode(data: booking.qrPayload.isEmpty ? String(booking.id.prefix(6)) : booking.qrPayload, size: 140)

                    Spacer().frame(height: 24)

                    Text("Please scan your QR code at the\nsalon's scanner machine")
                        .font(.subheadline)
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                        .multilineTextAlignment(.center)

                    Spacer().frame(height: 32)

                    VStack(spacing: 24) {
                        ReceiptGridRow(
                            leftLabel: "Booking ID", leftValue: String(booking.id.prefix(6)),
                            rightLabel: "Customer", rightValue: booking.customerName
                        )
                        ReceiptGridRow(
                            leftLabel: "Phone", leftValue: booking.customerPhone.isEmpty ? "-" : booking.customerPhone,
                            rightLabel: "Salon", rightValue: booking.salonName.isEmpty ? "Mira Salon" : booking.salonName
                        )
                        
                        let formattedDate: String = {
                            let bookingDate = Date(timeIntervalSince1970: TimeInterval(booking.dateTime / 1000))
                            let dateFormatter = DateFormatter()
                            dateFormatter.dateFormat = "MMMM d, yyyy"
                            return dateFormatter.string(from: bookingDate)
                        }()
                        
                        let formattedTime: String = {
                            let bookingDate = Date(timeIntervalSince1970: TimeInterval(booking.dateTime / 1000))
                            let timeFormatter = DateFormatter()
                            timeFormatter.dateFormat = "HH:mm"
                            return booking.timeSlotLabel.isEmpty ? timeFormatter.string(from: bookingDate) : booking.timeSlotLabel
                        }()
                        
                        ReceiptGridRow(
                            leftLabel: "Booking Date", leftValue: formattedDate,
                            rightLabel: "Booking Time", rightValue: formattedTime
                        )
                        
                        if booking.createdAt > 0 {
                            let formattedCreatedDate: String = {
                                let createdDate = Date(timeIntervalSince1970: TimeInterval(booking.createdAt / 1000))
                                let df = DateFormatter()
                                df.dateFormat = "MMMM d, yyyy"
                                return df.string(from: createdDate)
                            }()
                            
                            ReceiptField(label: "Booking Placed", value: formattedCreatedDate)
                        }

                        ForEach(Array(booking.services.enumerated()), id: \.element.id) { index, service in
                            ReceiptGridRow(
                                leftLabel: index == 0 ? "Service" : nil,
                                leftValue: service.name,
                                rightLabel: index == 0 ? "Stylist" : nil,
                                rightValue: booking.specialistName
                            )
                        }
                    }
                    .frame(maxWidth: .infinity)

                    Spacer().frame(height: 32)

                    VStack(spacing: 12) {
                        ReceiptTotalRow(label: "Sub Total", value: booking.subtotalAmount.miraPrice())
                        if booking.discountAmount > 0 {
                            ReceiptTotalRow(label: "Discount", value: "-\(booking.discountAmount.miraPrice())")
                        }
                        if booking.taxAmount > 0 {
                            ReceiptTotalRow(label: "Sales Tax (\(Int(booking.taxRatePercent))%)", value: booking.taxAmount.miraPrice())
                        }
                        ReceiptTotalRow(label: "Total Amount", value: booking.totalAmount.miraPrice(), emphasized: true)
                    }

                    Spacer().frame(height: 24)
                }
                .padding(.horizontal, 24)
            }

            // Done button
            Button {
                state.eventSink(EReceiptEventViewBookingsClicked())
            } label: {
                Text("Done")
                    .font(.subheadline)
                    .bold()
                    .foregroundColor(MiraTheme.onPrimary)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(MiraTheme.primary)
                    .cornerRadius(MiraTheme.radiusCard)
            }
            .padding(.horizontal, 24)
            .padding(.vertical, 16)
        }
    }

    @ViewBuilder
    private func errorView(_ state: EReceiptStateError) -> some View {
        VStack(spacing: 0) {
            MiraTopBar {
                Text("Error")
                    .font(.headline)
                    .bold()
            } navigationIcon: {
                Button { state.eventSink(EReceiptEventBackClicked()) } label: {
                    Image(systemName: "chevron.left")
                        .foregroundColor(MiraTheme.onBackground)
                }
            }
            Spacer()
            Text(state.message)
                .foregroundColor(MiraTheme.onSurfaceVariant)
            Spacer()
        }
    }

    @ViewBuilder
    private func loadingView() -> some View {
        VStack(spacing: 0) {
            MiraTopBar {
                Text("E-Receipt")
                    .font(.headline)
                    .bold()
            }
            Spacer()
            ProgressView()
            Spacer()
        }
    }
}

private struct ReceiptGridRow: View {
    var leftLabel: String?
    let leftValue: String
    var rightLabel: String?
    let rightValue: String

    var body: some View {
        HStack(alignment: .top) {
            ReceiptField(label: leftLabel, value: leftValue)
                .frame(maxWidth: .infinity, alignment: .leading)
            Spacer().frame(width: 16)
            ReceiptField(label: rightLabel, value: rightValue)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

private struct ReceiptField: View {
    var label: String?
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if let label = label {
                Text(label)
                    .font(.caption)
                    .foregroundColor(MiraTheme.onSurfaceVariant)
                    .lineLimit(1)
            }
            Text(value)
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(MiraTheme.onSurface)
                .lineLimit(1)
        }
    }
}

private struct ReceiptTotalRow: View {
    let label: String
    let value: String
    var emphasized: Bool = false

    var body: some View {
        HStack {
            Text(label)
                .font(emphasized ? .subheadline : .subheadline)
                .fontWeight(emphasized ? .bold : .regular)
            Spacer()
            Text(value)
                .font(.subheadline)
                .fontWeight(emphasized ? .bold : .semibold)
        }
    }
}
