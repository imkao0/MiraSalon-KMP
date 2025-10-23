import SwiftUI
import ComposeApp

/// Mirrors Android `AppointmentsScreen.kt` (grouped appointment list with pull-to-refresh).
struct AppointmentsView: View {
    @EnvironmentObject private var navigation: CircuitNavigation

    var body: some View {
        CircuitView(screen: AppointmentRouteAppointments(), navigator: navigation) { (state: AppointmentsState) in
            VStack(spacing: 0) {
                MiraTopBar {
                    Text("Appointments")
                        .font(.headline)
                        .bold()
                }

                if state.groupedAppointments.isEmpty && !state.isLoading {
                    EmptyStateView(
                        message: "No appointments found",
                        description: "You don't have any appointments scheduled yet. Book your first appointment to see it here.",
                        icon: "calendar"
                    )
                } else {
                    List {
                        // Kotlin maps preserve insertion order for grouping headers
                        ForEach(Array(state.groupedAppointments.keys), id: \.self) { header in
                            Section(header:
                                Text(header)
                                    .font(.subheadline)
                                    .bold()
                                    .foregroundColor(MiraTheme.primary)
                            ) {
                                ForEach(state.groupedAppointments[header] ?? [], id: \.id) { appointment in
                                    AppointmentItemRow(
                                        appointment: appointment,
                                        onClick: { state.eventSink(AppointmentsEventAppointmentClicked(id: appointment.id)) },
                                        onSpecialistClick: { state.eventSink(AppointmentsEventSpecialistClicked(id: $0)) }
                                    )
                                    .listRowSeparator(.hidden)
                                    .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                                }
                            }
                        }
                    }
                    .listStyle(.plain)
                    .refreshable {
                        state.eventSink(AppointmentsEventRefresh())
                    }
                }
            }
            .background(MiraTheme.background.ignoresSafeArea())
        }
    }
}

private struct AppointmentItemRow: View {
    let appointment: Appointment
    let onClick: () -> Void
    let onSpecialistClick: (String) -> Void

    var body: some View {
        Button(action: onClick) {
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(appointment.salonName)
                        .font(.subheadline)
                        .bold()
                        .foregroundColor(MiraTheme.onSurface)
                        .lineLimit(1)

                    Text("Specialist: \(appointment.specialistName)")
                        .font(.caption)
                        .foregroundColor(MiraTheme.primary)
                        .onTapGesture { onSpecialistClick(appointment.specialistId) }

                    Text(appointment.services.map { $0.name }.joined(separator: ", "))
                        .font(.caption)
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                        .lineLimit(1)

                    Text(MiraDateFormat.shortAt(epochMillis: appointment.dateTime))
                        .font(.caption2)
                        .foregroundColor(MiraTheme.primary)
                }
                Spacer()
                MiraStatusChip(status: appointment.status.name)
            }
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(MiraTheme.surface)
            .cornerRadius(MiraTheme.radiusCard)
            .shadow(color: Color.black.opacity(0.06), radius: 2, x: 0, y: 1)
        }
        .buttonStyle(.plain)
    }
}
