import SwiftUI
import ComposeApp

/// Mirrors Android `MyBookingsScreen.kt` (the Booking tab root, route BottomNavKey.Booking).
struct MyBookingsView: View {
    @EnvironmentObject private var navigation: CircuitNavigation

    var body: some View {
        CircuitView(screen: BottomNavKeyBooking(), navigator: navigation) { (state: MyBookingsState) in
            VStack(spacing: 0) {
                // Top bar + tabs
                VStack(spacing: 0) {
                    MiraTopBar {
                        Text("Appointments")
                            .font(.headline)
                            .bold()
                    }

                    BookingTabBar(selected: state.selectedStatus) { status in
                        state.eventSink(MyBookingsEventTabSelected(status: status))
                    }
                }
                .background(MiraTheme.surface)

                let bookings = state.filteredBookings
                if bookings.isEmpty && !state.isLoading {
                    EmptyStateView(
                        message: "No \(state.selectedStatus.name.lowercased()) bookings found",
                        description: "You don't have any \(state.selectedStatus.name.lowercased()) appointments yet.",
                        icon: "calendar"
                    )
                } else {
                    ScrollView {
                        LazyVStack(spacing: 16) {
                            ForEach(bookings, id: \.id) { booking in
                                BookingCard(
                                    booking: booking,
                                    currentTimeMillis: state.currentTimeMillis,
                                    selectedStatus: state.selectedStatus,
                                    onReminderToggled: { enabled in
                                        state.eventSink(MyBookingsEventReminderToggled(id: booking.id, enabled: enabled))
                                    },
                                    onPrimaryAction: {
                                        switch state.selectedStatus {
                                        case .confirmed:
                                            state.eventSink(MyBookingsEventEReceiptClicked(id: booking.id))
                                        case .completed:
                                            state.eventSink(MyBookingsEventAddReviewClicked(id: booking.id))
                                        case .cancelled:
                                            state.eventSink(MyBookingsEventRebookClicked(booking: booking))
                                        default: break
                                        }
                                    },
                                    onSecondaryAction: {
                                        switch state.selectedStatus {
                                        case .confirmed:
                                            state.eventSink(MyBookingsEventCancelClicked(id: booking.id))
                                        case .completed:
                                            state.eventSink(MyBookingsEventRebookClicked(booking: booking))
                                        default: break
                                        }
                                    }
                                )
                            }
                        }
                        .padding(.horizontal, 20)
                        .padding(.vertical, 16)
                    }
                    .refreshable {
                        state.eventSink(MyBookingsEventRefresh())
                    }
                }
            }
            .background(MiraTheme.background.ignoresSafeArea())
        }
    }
}

// MARK: - Tab bar (Upcoming / Completed / Cancelled)
private struct BookingTabBar: View {
    let selected: BookingStatus
    let onSelect: (BookingStatus) -> Void

    private let tabs: [(BookingStatus, String)] = [
        (.confirmed, "Upcoming"),
        (.completed, "Completed"),
        (.cancelled, "Cancelled")
    ]

    var body: some View {
        HStack(spacing: 0) {
            ForEach(tabs, id: \.0.name) { status, label in
                let isSelected = status == selected
                Button {
                    onSelect(status)
                } label: {
                    VStack(spacing: 8) {
                        Text(label)
                            .font(.system(size: 15, weight: isSelected ? .semibold : .regular))
                            .foregroundColor(isSelected ? MiraTheme.primary : MiraTheme.onSurfaceVariant)
                        Rectangle()
                            .fill(isSelected ? MiraTheme.primary : Color.clear)
                            .frame(height: 3)
                    }
                    .frame(maxWidth: .infinity)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.top, 4)
    }
}

// MARK: - Booking card
private struct BookingCard: View {
    let booking: ConfirmedBooking
    let currentTimeMillis: Int64
    let selectedStatus: BookingStatus
    let onReminderToggled: (Bool) -> Void
    let onPrimaryAction: () -> Void
    let onSecondaryAction: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            // Date + reminder
            HStack {
                Text(MiraDateFormat.bookingDateTime(epochMillis: booking.dateTime))
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(MiraTheme.onSurface)
                Spacer()
                if booking.status == .confirmed {
                    HStack(spacing: 8) {
                        Text("Remind me")
                            .font(.system(size: 13))
                            .foregroundColor(MiraTheme.onSurfaceVariant)
                        MiraRectangularSwitch(isOn: booking.reminderEnabled, onToggle: onReminderToggled)
                    }
                }
            }

            Spacer().frame(height: 14)

            // Salon info
            HStack(spacing: 14) {
                let imageUrl = booking.salonImageUrl ?? booking.serviceImageUrl
                let resolvedUrl = ApiEndpoints.shared.resolveImageUrl(imagePath: imageUrl)
                AsyncImage(url: URL(string: resolvedUrl ?? "")) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    MiraTheme.surfaceVariant
                }
                .frame(width: 64, height: 64)
                .cornerRadius(2)

                VStack(alignment: .leading, spacing: 2) {
                    Text(booking.services.first?.name ?? "Service")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(MiraTheme.onSurface)
                    Text(booking.salonName)
                        .font(.system(size: 13))
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                    Text("Specialist: \(booking.specialistName)")
                        .font(.system(size: 12))
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                    Text("Service ID: # \(String(booking.id.prefix(8)).uppercased())")
                        .font(.system(size: 12))
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                }
                Spacer()
            }

            Spacer().frame(height: 16)

            // Actions per status
            switch booking.status {
            case .confirmed:
                HStack(spacing: 12) {
                    BookingOutlineButton(
                        title: "Cancel",
                        isEnabled: booking.canCancel(currentTimeMillis: currentTimeMillis),
                        action: onSecondaryAction
                    )
                    BookingFilledButton(title: "E-Receipt", action: onPrimaryAction)
                }
            case .completed:
                HStack(spacing: 12) {
                    BookingOutlineButton(title: "Re - book", action: onSecondaryAction)
                    BookingFilledButton(
                        title: booking.isReviewed ? "Completed" : "Add Review",
                        isEnabled: !booking.isReviewed,
                        action: onPrimaryAction
                    )
                }
            default: // cancelled
                BookingFilledButton(title: "Re - Book", action: onPrimaryAction)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity)
        .background(MiraTheme.surface)
        .cornerRadius(2)
        .overlay(
            RoundedRectangle(cornerRadius: 2)
                .stroke(MiraTheme.onSurface.opacity(0.1), lineWidth: 1)
        )
    }
}

private struct BookingFilledButton: View {
    let title: String
    var isEnabled: Bool = true
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(isEnabled ? MiraTheme.onPrimary : MiraTheme.onSurfaceVariant)
                .frame(maxWidth: .infinity)
                .frame(height: 44)
                .background(isEnabled ? MiraTheme.primary : MiraTheme.surfaceVariant)
                .cornerRadius(2)
        }
        .disabled(!isEnabled)
    }
}

private struct BookingOutlineButton: View {
    let title: String
    var isEnabled: Bool = true
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(isEnabled ? MiraTheme.primary : MiraTheme.onSurfaceVariant)
                .frame(maxWidth: .infinity)
                .frame(height: 44)
                .background(
                    RoundedRectangle(cornerRadius: 2)
                        .stroke(isEnabled ? MiraTheme.primary : MiraTheme.onSurfaceVariant.opacity(0.3), lineWidth: 1)
                )
        }
        .disabled(!isEnabled)
    }
}
