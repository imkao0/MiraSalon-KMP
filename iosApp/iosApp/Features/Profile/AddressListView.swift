import SwiftUI
import ComposeApp

/// Mirrors Android `AddressListScreen.kt`.
struct AddressListView: View {
    @EnvironmentObject private var navigation: CircuitNavigation

    var body: some View {
        CircuitView(screen: ProfileRouteAddresses(), navigator: navigation) { (state: AddressListState) in
            VStack(spacing: 0) {
                MiraTopBar {
                    Text("Address")
                        .font(.headline)
                        .bold()
                }

                ScrollView {
                    if state.addresses.isEmpty {
                        EmptyStateView(
                            message: "No addresses saved",
                            description: "Add your address to get services at your doorstep.",
                            icon: "mappin.and.ellipse"
                        )
                        .padding(.top, 50)
                    } else {
                        LazyVStack(spacing: 0) {
                            ForEach(state.addresses, id: \.id) { address in
                                AddressItemRow(
                                    address: address,
                                    onSelect: { state.eventSink(AddressListEventSetDefault(id: address.id)) },
                                    onEdit: { state.eventSink(AddressListEventEditAddress(id: address.id)) },
                                    onRemove: { state.eventSink(AddressListEventDeleteAddress(id: address.id)) }
                                )
                                Divider().padding(.vertical, 8)
                            }
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 8)
                    }

                    // Add new address
                    Button {
                            state.eventSink(AddressListEventAddAddress())
                        } label: {
                            HStack(spacing: 12) {
                                Image(systemName: "mappin.and.ellipse")
                                    .foregroundColor(MiraTheme.primary)
                                    .font(.system(size: 22))
                                Text("Add New Address")
                                    .font(.subheadline)
                                    .bold()
                                    .foregroundColor(MiraTheme.primary)
                                Spacer()
                            }
                            .padding(.vertical, 16)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                }

                // Bottom Save button
                Button {
                    state.eventSink(AddressListEventBack())
                } label: {
                    Text("Save")
                        .font(.headline)
                        .foregroundColor(MiraTheme.onPrimary)
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(MiraTheme.primary)
                        .cornerRadius(8)
                }
                .padding(16)
            }
            .background(MiraTheme.background.ignoresSafeArea())
        }
    }

private struct AddressItemRow: View {
    let address: Address_
    let onSelect: () -> Void
    let onEdit: () -> Void
    let onRemove: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            MiraRadioButton(isSelected: address.isDefault, onSelect: onSelect)
                .padding(.top, 2)

            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 8) {
                    Text("\(address.firstName) \(address.lastName)")
                        .font(.subheadline)
                        .bold()
                        .foregroundColor(MiraTheme.onSurface)
                    AddressLabelBadge(label: address.label.displayName)
                }

                Text("\(address.firstName) \(address.lastName) \(address.phoneNumber)")
                    .font(.subheadline)
                    .foregroundColor(MiraTheme.onSurfaceVariant)
                Text("\(address.streetAddress) \(address.number), \(address.state)")
                    .font(.subheadline)
                    .foregroundColor(MiraTheme.onSurfaceVariant)

                HStack(spacing: 16) {
                    Text("Edit")
                        .font(.subheadline)
                        .bold()
                        .foregroundColor(MiraTheme.primary)
                        .onTapGesture(perform: onEdit)
                    Rectangle()
                        .fill(MiraTheme.onSurfaceVariant)
                        .frame(width: 1, height: 16)
                    Text("Remove")
                        .font(.subheadline)
                        .bold()
                        .foregroundColor(MiraTheme.error)
                        .onTapGesture(perform: onRemove)
                }
                .padding(.top, 4)
            }
            Spacer()
        }
        .contentShape(Rectangle())
        .onTapGesture(perform: onSelect)
        .padding(.vertical, 12)
    }
}

private struct AddressLabelBadge: View {
    let label: String
    var body: some View {
        Text(label)
            .font(.system(size: 10))
            .foregroundColor(MiraTheme.primary)
            .padding(.horizontal, 6)
            .padding(.vertical, 2)
            .background(MiraTheme.primaryContainer.opacity(0.1))
            .cornerRadius(4)
            .overlay(
                RoundedRectangle(cornerRadius: 4)
                    .stroke(MiraTheme.primary.opacity(0.3), lineWidth: 1)
            )
    }
}
