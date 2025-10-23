import SwiftUI
import ComposeApp

struct CheckoutView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    let screen: any Circuit_runtime_screenScreen
    
    var body: some View {
        CircuitView(screen: screen, navigator: navigation) { (state: AppointmentCheckoutState) in
            VStack(spacing: 0) {
                // Top App Bar
                MiraTopBar {
                    Text("Review Booking")
                        .font(MiraType.headlineSmall.weight(.bold))
                }
                
                if state.isLoading {
                    Spacer()
                    ProgressView()
                    Spacer()
                } else {
                    ScrollView {
                        VStack(alignment: .leading, spacing: MiraTheme.spacingLarge) {
                            // Summary Section
                            VStack(alignment: .leading, spacing: MiraTheme.spacingMedium) {
                                Text("Appointment Summary")
                                    .font(MiraType.titleMedium.weight(.bold))
                                    .foregroundColor(MiraTheme.onSurface)
                                
                                ForEach(state.services, id: \.id) { service in
                                    HStack {
                                        VStack(alignment: .leading, spacing: 4) {
                                            Text(service.name)
                                                .font(MiraType.bodyLarge.weight(.medium))
                                                .foregroundColor(MiraTheme.onSurface)
                                            Text("\(service.durationMinutes) min")
                                                .font(MiraType.bodySmall)
                                                .foregroundColor(MiraTheme.onSurfaceVariant)
                                        }
                                        Spacer()
                                        Text(Double(service.discountedPrice).miraPrice())
                                            .font(MiraType.bodyLarge.weight(.bold))
                                            .foregroundColor(MiraTheme.primary)
                                    }
                                }
                                
                                Divider()
                                    .background(MiraTheme.outlineVariant)
                                
                                HStack {
                                    Image(systemName: "calendar")
                                        .foregroundColor(MiraTheme.onSurfaceVariant)
                                        .font(.system(size: MiraTheme.iconSizeMedium))
                                    
                                    let dateLabel = MiraDateFormat.separator(epochSeconds: state.dateTime / 1000) + ", " + MiraDateFormat.time(epochSeconds: state.dateTime / 1000)
                                    
                                    Text(dateLabel)
                                        .font(MiraType.bodyMedium)
                                        .foregroundColor(MiraTheme.onSurface)
                                    
                                    Spacer()
                                    
                                    let totalDuration = state.services.reduce(0) { $0 + Int($1.durationMinutes) }
                                    Text("\(totalDuration) min total")
                                        .font(MiraType.bodySmall)
                                        .foregroundColor(MiraTheme.onSurfaceVariant)
                                }
                            }
                            .padding(MiraTheme.spacingMedium)
                            .background(MiraTheme.surface)
                            .cornerRadius(MiraTheme.radiusMedium)
                            .overlay(
                                RoundedRectangle(cornerRadius: MiraTheme.radiusMedium)
                                    .stroke(MiraTheme.outlineVariant, lineWidth: MiraTheme.strokeThin)
                            )
                            
                            // Payment Section
                            VStack(alignment: .leading, spacing: MiraTheme.spacingMedium) {
                                ForEach(state.paymentMethods, id: \.id) { method in
                                    PaymentMethodRow(
                                        method: method,
                                        isSelected: state.selectedPaymentMethodId == method.id,
                                        onClick: {
                                            state.eventSink(AppointmentCheckoutEventPaymentMethodSelected(id: method.id))
                                        }
                                    )
                                    
                                    if method.id != state.paymentMethods.last?.id {
                                        Divider()
                                            .background(MiraTheme.outlineVariant)
                                            .padding(.vertical, MiraTheme.spacingDefault)
                                    }
                                }
                                
                                Button {
                                    state.eventSink(AppointmentCheckoutEventAddPaymentMethod())
                                } label: {
                                    Text("Add Payment Method")
                                        .font(MiraType.bodyMedium.weight(.bold))
                                        .foregroundColor(MiraTheme.onBackground)
                                        .frame(maxWidth: .infinity)
                                        .frame(height: 48)
                                        .background(
                                            RoundedRectangle(cornerRadius: MiraTheme.radiusMedium)
                                                .stroke(MiraTheme.outline, lineWidth: MiraTheme.strokeThin)
                                        )
                                }
                            }
                            .padding(MiraTheme.spacingMedium)
                            .background(MiraTheme.surface)
                            .cornerRadius(MiraTheme.radiusMedium)
                            .overlay(
                                RoundedRectangle(cornerRadius: MiraTheme.radiusMedium)
                                    .stroke(MiraTheme.outlineVariant, lineWidth: MiraTheme.strokeThin)
                            )
                            
                            // Policies
                            PolicySection(
                                title: "Cancellation Policy",
                                content: "Cancellations must be made at least 48 hours in advance to receive a refund."
                            )
                            
                            RulesSection()
                            
                            Spacer().frame(height: 120)
                        }
                        .padding(MiraTheme.spacingMedium)
                    }
                }
                
                // Bottom Bar
                VStack(spacing: MiraTheme.spacingMedium) {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            HStack(alignment: .firstTextBaseline, spacing: MiraTheme.spacingSmall) {
                                if state.discountedAmount < state.totalAmount {
                                    Text(Double(state.totalAmount).miraPrice())
                                        .font(MiraType.titleLarge)
                                        .strikethrough()
                                        .foregroundColor(MiraTheme.onSurfaceVariant)
                                }
                                Text(Double(state.discountedAmount).miraPrice())
                                    .font(MiraType.headlineSmall.weight(.bold))
                                    .foregroundColor(MiraTheme.primary)
                                Text(" /Session")
                                    .font(MiraType.bodyMedium)
                                    .foregroundColor(MiraTheme.onSurfaceVariant)
                            }
                            Text("Includes taxes and other fees.")
                                .font(MiraType.bodySmall)
                                .foregroundColor(MiraTheme.onSurfaceVariant)
                        }
                        Spacer()
                    }
                    
                    MiraButton(
                        text: "Continue",
                        onClick: { state.eventSink(AppointmentCheckoutEventContinue()) },
                        enabled: !state.isBooking,
                        isLoading: state.isBooking
                    )
                }
                .padding(MiraTheme.spacingMedium)
                .background(MiraTheme.surface)
                .overlay(
                    Rectangle()
                        .frame(height: MiraTheme.bottomNavDividerThickness)
                        .foregroundColor(MiraTheme.outlineVariant),
                    alignment: .top
                )
            }
            .background(MiraTheme.background)
            .sheet(isPresented: Binding(
                get: { state.showAddPaymentSheet },
                set: { if !$0 { state.eventSink(AppointmentCheckoutEventDismissAddPaymentSheet()) } }
            )) {
                AddPaymentBottomSheet(state: state)
                    .presentationDetents([.medium, .large])
            }
        }
    }
}

private struct PaymentMethodRow: View {
    let method: PaymentMethod
    let isSelected: Bool
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 12) {
                // Card Icon Placeholder
                ZStack {
                    RoundedRectangle(cornerRadius: 4)
                        .stroke(MiraTheme.outlineVariant, lineWidth: 1)
                        .background(MiraTheme.surface)
                    
                    Image(systemName: "creditcard")
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                }
                .frame(width: 44, height: 32)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text("\(method.type.displayName) ****\(method.last4Digits ?? "0000")")
                        .font(MiraType.bodyLarge.weight(.medium))
                        .foregroundColor(MiraTheme.onBackground)
                    Text("Exp: \(method.expiryDate ?? "MM/YY")")
                        .font(MiraType.bodySmall)
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                }
                
                Spacer()
                
                MiraRadioButton(isSelected: isSelected, onSelect: onClick)
            }
            .padding(.vertical, 4)
        }
        .buttonStyle(.plain)
    }
}

private struct PolicySection: View {
    let title: String
    let content: String
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(MiraType.titleMedium)
                .foregroundColor(MiraTheme.onSurfaceVariant)
            Text(content)
                .font(MiraType.bodyMedium)
                .lineSpacing(4)
                .foregroundColor(MiraTheme.onBackground)
                .padding(MiraTheme.spacingMedium)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(
                    RoundedRectangle(cornerRadius: MiraTheme.radiusMedium)
                        .stroke(MiraTheme.outlineVariant, lineWidth: 1)
                )
        }
    }
}

private struct RulesSection: View {
    var body: some View {
        VStack(alignment: .leading, spacing: MiraTheme.spacingDefault) {
            Text("Rules")
                .font(MiraType.titleMedium)
                .foregroundColor(MiraTheme.onSurfaceVariant)
            
            VStack(alignment: .leading, spacing: MiraTheme.spacingDefault) {
                Text("Please adhere to the following salon rules for a pleasant experience:")
                    .font(MiraType.bodyMedium.weight(.medium))
                    .foregroundColor(MiraTheme.onBackground)
                
                RuleItemRow(title: "No Smoking Policy", description: "Smoking is strictly prohibited...")
                RuleItemRow(title: "Quiet Hours", description: "Maintain peace between 8 AM and 7...")
                RuleItemRow(title: "Guest Behavior", description: "Respect staff and fellow guests at...")
            }
            .padding(MiraTheme.spacingMedium)
            .background(
                RoundedRectangle(cornerRadius: MiraTheme.radiusMedium)
                    .stroke(MiraTheme.outlineVariant, lineWidth: 1)
            )
        }
    }
}

private struct RuleItemRow: View {
    let title: String
    let description: String
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Circle()
                .fill(MiraTheme.primary)
                .frame(width: 8, height: 8)
                .padding(.top, 6)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(MiraType.bodyMedium.weight(.bold))
                    .foregroundColor(MiraTheme.onBackground)
                Text(description)
                    .font(MiraType.bodySmall)
                    .foregroundColor(MiraTheme.onSurfaceVariant)
                    .lineLimit(1)
            }
        }
    }
}

private struct AddPaymentBottomSheet: View {
    let state: AppointmentCheckoutState
    
    @State private var cardType = "Visa"
    @State private var nameOnCard = ""
    @State private var cardNumber = ""
    @State private var expiry = ""
    @State private var cvc = ""
    @FocusState private var focusedField: Field?

    enum Field {
        case name, number, expiry, cvc
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text("Add New Card")
                .font(MiraType.headlineSmall.weight(.bold))
                .foregroundColor(MiraTheme.onSurface)
            
            HStack(spacing: 8) {
                ForEach(["Visa", "Master Card"], id: \.self) { type in
                    MiraChip(label: type, isSelected: cardType == type) {
                        cardType = type
                    }
                }
            }
            
            VStack(alignment: .leading, spacing: MiraTheme.spacingSmall) {
                Text("Name on card")
                    .font(MiraType.bodyMedium.weight(.medium))
                TextField("John Doe", text: $nameOnCard)
                    .focused($focusedField, equals: .name)
                    .submitLabel(.next)
                    .onSubmit { focusedField = .number }
                    .padding()
                    .background(MiraTheme.surface)
                    .cornerRadius(MiraTheme.radiusMedium)
                    .overlay(RoundedRectangle(cornerRadius: MiraTheme.radiusMedium).stroke(MiraTheme.outlineVariant))
            }
            
            VStack(alignment: .leading, spacing: MiraTheme.spacingSmall) {
                Text("Card number")
                    .font(MiraType.bodyMedium.weight(.medium))
                TextField("0000 0000 0000 0000", text: $cardNumber)
                    .focused($focusedField, equals: .number)
                    .submitLabel(.next)
                    .onSubmit { focusedField = .expiry }
                    .keyboardType(.numberPad)
                    .padding()
                    .background(MiraTheme.surface)
                    .cornerRadius(MiraTheme.radiusMedium)
                    .overlay(RoundedRectangle(cornerRadius: MiraTheme.radiusMedium).stroke(MiraTheme.outlineVariant))
            }
            
            HStack(spacing: MiraTheme.spacingMedium) {
                VStack(alignment: .leading, spacing: MiraTheme.spacingSmall) {
                    Text("Expiry Date")
                        .font(MiraType.bodyMedium.weight(.medium))
                    TextField("MM/YY", text: $expiry)
                        .focused($focusedField, equals: .expiry)
                        .submitLabel(.next)
                        .onSubmit { focusedField = .cvc }
                        .padding()
                        .background(MiraTheme.surface)
                        .cornerRadius(MiraTheme.radiusMedium)
                        .overlay(RoundedRectangle(cornerRadius: MiraTheme.radiusMedium).stroke(MiraTheme.outlineVariant))
                }
                
                VStack(alignment: .leading, spacing: MiraTheme.spacingSmall) {
                    Text("Security code")
                        .font(MiraType.bodyMedium.weight(.medium))
                    TextField("CVC", text: $cvc)
                        .focused($focusedField, equals: .cvc)
                        .submitLabel(.done)
                        .onSubmit { focusedField = nil }
                        .keyboardType(.numberPad)
                        .padding()
                        .background(MiraTheme.surface)
                        .cornerRadius(MiraTheme.radiusMedium)
                        .overlay(RoundedRectangle(cornerRadius: MiraTheme.radiusMedium).stroke(MiraTheme.outlineVariant))
                }
            }
            
            MiraButton(
                text: "Save Card",
                onClick: {
                    state.eventSink(AppointmentCheckoutEventSavePaymentMethod(
                        type: cardType,
                        nameOnCard: nameOnCard,
                        cardNumber: cardNumber,
                        expiry: expiry,
                        cvc: cvc
                    ))
                },
                enabled: cardNumber.count >= 12 && expiry.count >= 4 && !nameOnCard.isEmpty
            )
            .padding(.top, 12)
            
            Spacer()
        }
        .padding(24)
        .background(MiraTheme.surface)
    }
}

