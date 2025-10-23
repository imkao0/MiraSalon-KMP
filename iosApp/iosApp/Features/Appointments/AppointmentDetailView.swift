import SwiftUI
import ComposeApp

/// Mirrors Android `AppointmentDetailScreen.kt`.
struct AppointmentDetailView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    let appointmentId: String

    var body: some View {
        CircuitView(screen: AppointmentRouteAppointmentDetail(appointmentId: appointmentId), navigator: navigation) { (state: AppointmentDetailState) in
            VStack(spacing: 0) {
                MiraTopBar {
                    Text("Appointment Details")
                        .font(.headline)
                        .bold()
                }

                if state.isLoading {
                    Spacer()
                    ProgressView()
                    Spacer()
                } else if let appointment = state.appointment {
                    ScrollView {
                        VStack(spacing: 0) {
                            Text(appointment.salonName)
                                .font(.title2)
                                .bold()

                            Text("With \(appointment.specialistName)")
                                .font(.subheadline)
                                .foregroundColor(MiraTheme.primary)
                                .onTapGesture {
                                    state.eventSink(AppointmentDetailEventSpecialistClicked(specialistId: appointment.specialistId))
                                }

                            Spacer().frame(height: 24)

                            // Services card
                            VStack(alignment: .leading, spacing: 8) {
                                Text("Services")
                                    .bold()
                                ForEach(appointment.services, id: \.id) { service in
                                    HStack {
                                        Text(service.name)
                                        Spacer()
                                        Text("\(service.price)")
                                    }
                                    .padding(.vertical, 4)
                                }
                                Divider().padding(.vertical, 8)
                                HStack {
                                    Text("Total").bold()
                                    Spacer()
                                    Text("\(appointment.totalAmount)").bold()
                                }
                            }
                            .padding(16)
                            .frame(maxWidth: .infinity)
                            .background(MiraTheme.surfaceVariant.opacity(0.5))
                            .cornerRadius(MiraTheme.radiusCard)

                            Spacer().frame(height: 24)

                            Button {
                                state.eventSink(AppointmentDetailEventViewMap())
                            } label: {
                                HStack {
                                    Image(systemName: "location")
                                    Text("View on Map")
                                }
                                .foregroundColor(MiraTheme.onPrimary)
                                .frame(maxWidth: .infinity)
                                .frame(height: 48)
                                .background(MiraTheme.primary)
                                .cornerRadius(MiraTheme.radiusCard)
                            }

                            Spacer().frame(height: 16)

                            Button {
                                state.eventSink(AppointmentDetailEventCancel())
                            } label: {
                                Text("Cancel Appointment")
                                    .foregroundColor(MiraTheme.error)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 48)
                                    .background(
                                        RoundedRectangle(cornerRadius: MiraTheme.radiusCard)
                                            .stroke(MiraTheme.error, lineWidth: 1)
                                    )
                            }
                        }
                        .padding(16)
                    }
                } else if let error = state.error {
                    Spacer()
                    Text(error).foregroundColor(MiraTheme.error)
                    Spacer()
                }
            }
            .background(MiraTheme.background.ignoresSafeArea())
        }
    }
}
