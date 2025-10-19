import SwiftUI
import ComposeApp

/// Mirrors Android `AddressFormScreen.kt`.
struct AddressFormView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    let addressId: String?
    @FocusState private var focusedField: Field?

    enum Field {
        case firstName, lastName, phoneNumber, streetAddress, number, state
    }

    var body: some View {
        CircuitView(screen: ProfileRouteAddressForm(addressId: addressId), navigator: navigation) { (state: AddressFormState) in
            VStack(spacing: 0) {
                MiraTopBar {
                    Text(state.isEditing ? "Edit Address" : "Add New Address")
                        .font(.headline)
                        .bold()
                }

                ScrollView {
                    VStack(spacing: 16) {
                        if let error = state.error {
                            Text(error)
                                .foregroundColor(MiraTheme.error)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }

                        AddressField(label: "First Name", text: Binding(
                            get: { state.firstName },
                            set: { state.eventSink(AddressFormEventFirstNameChanged(value: $0)) }
                        ), submitLabel: .next, onSubmit: { focusedField = .lastName })
                        .focused($focusedField, equals: .firstName)

                        AddressField(label: "Last Name", text: Binding(
                            get: { state.lastName },
                            set: { state.eventSink(AddressFormEventLastNameChanged(value: $0)) }
                        ), submitLabel: .next, onSubmit: { focusedField = .phoneNumber })
                        .focused($focusedField, equals: .lastName)

                        AddressField(label: "Phone Number", text: Binding(
                            get: { state.phoneNumber },
                            set: { state.eventSink(AddressFormEventPhoneNumberChanged(value: $0)) }
                        ), keyboard: .phonePad, submitLabel: .next, onSubmit: { focusedField = .streetAddress })
                        .focused($focusedField, equals: .phoneNumber)

                        AddressField(label: "Street Address", text: Binding(
                            get: { state.streetAddress },
                            set: { state.eventSink(AddressFormEventStreetAddressChanged(value: $0)) }
                        ), submitLabel: .next, onSubmit: { focusedField = .number })
                        .focused($focusedField, equals: .streetAddress)

                        AddressField(label: "Number", text: Binding(
                            get: { state.number },
                            set: { state.eventSink(AddressFormEventNumberChanged(value: $0)) }
                        ), submitLabel: .next, onSubmit: { focusedField = .state })
                        .focused($focusedField, equals: .number)

                        AddressField(label: "State", text: Binding(
                            get: { state.state },
                            set: { state.eventSink(AddressFormEventStateChanged(value: $0)) }
                        ), submitLabel: .done, onSubmit: { focusedField = nil })
                        .focused($focusedField, equals: .state)

                        // Label selector
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Label")
                                .font(.subheadline)
                                .bold()
                            HStack(spacing: 8) {
                                labelChip(state: state, label: .home, title: "Home")
                                labelChip(state: state, label: .work, title: "Work")
                                labelChip(state: state, label: .other, title: "Other")
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)

                        if state.isEditing {
                            Button {
                                state.eventSink(AddressFormEventDelete())
                            } label: {
                                Text("Delete Address")
                                    .foregroundColor(MiraTheme.error)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 48)
                                    .background(
                                        RoundedRectangle(cornerRadius: 8)
                                            .stroke(MiraTheme.error, lineWidth: 1)
                                    )
                            }
                            .disabled(state.isDeleting)
                        }

                        Spacer().frame(height: 16)
                    }
                    .padding(16)
                }

                // Save button
                Button {
                    state.eventSink(AddressFormEventSave())
                } label: {
                    Group {
                        if state.isSaving {
                            ProgressView().tint(MiraTheme.onPrimary)
                        } else {
                            Text(state.isEditing ? "Update Address" : "Add Address")
                                .font(.headline)
                        }
                    }
                    .foregroundColor(MiraTheme.onPrimary)
                    .frame(maxWidth: .infinity)
                    .frame(height: 56)
                    .background(MiraTheme.primary)
                    .cornerRadius(8)
                }
                .disabled(!(state.isValid && !state.isSaving))
                .padding(16)
            }
            .background(MiraTheme.background.ignoresSafeArea())
        }
    }

    private func labelChip(state: AddressFormState, label: AddressLabel_, title: String) -> some View {
        let selected = state.label == label
        return Button {
            state.eventSink(AddressFormEventLabelSelected(label: label))
        } label: {
            Text(title)
                .font(.subheadline)
                .foregroundColor(selected ? MiraTheme.onPrimary : MiraTheme.onSurface)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(selected ? MiraTheme.primary : MiraTheme.surfaceVariant.opacity(0.5))
                .cornerRadius(8)
        }
        .buttonStyle(.plain)
    }
}

private struct AddressField: View {
    let label: String
    @Binding var text: String
    var keyboard: UIKeyboardType = .default
    var submitLabel: SubmitLabel = .next
    var onSubmit: () -> Void = {}

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.subheadline)
                .bold()
            TextField("", text: $text)
                .keyboardType(keyboard)
                .submitLabel(submitLabel)
                .onSubmit(onSubmit)
                .padding(.horizontal, 16)
                .frame(height: 52)
                .frame(maxWidth: .infinity)
                .background(MiraTheme.surfaceVariant.opacity(0.5))
                .cornerRadius(8)
        }
        .frame(maxWidth: .infinity)
    }
}
