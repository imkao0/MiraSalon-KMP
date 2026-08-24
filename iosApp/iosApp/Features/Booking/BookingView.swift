import SwiftUI
import ComposeApp

struct BookingView: View {
    @EnvironmentObject private var navigation: CircuitNavigation
    let screen: BookingRouteBooking

    var body: some View {
        CircuitView(screen: screen, navigator: navigation) { (state: BookingState) in
            VStack(spacing: 0) {
                // Top App Bar
                MiraTopBar {
                    Text("Book Appointment")
                        .font(.headline)
                        .bold()
                } navigationIcon: {
                    Button {
                        state.eventSink(BookingEventBack())
                    } label: {
                        Image(systemName: "chevron.left")
                            .foregroundColor(MiraTheme.onBackground)
                    }
                }
                
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        CalendarSection(
                            state: state,
                            onDateSelected: { date in
                                state.eventSink(BookingEventDateSelected(date: date))
                            },
                            onToggle: {
                                state.eventSink(BookingEventToggleCalendar())
                            }
                        )
                        
                        BookingDropdownSheetView(
                            expanded: state.sheetExpanded,
                            date: state.selectedDate,
                            bookings: state.selectedDateBookings,
                            onMessageClick: { bookingId in
                                // Handle message click
                            }
                        )
                        
                        SpecialistSection(state: state) { specialistId in
                            state.eventSink(BookingEventSpecialistSelected(specialistId: specialistId))
                        }
                        
                        TimeSlotSection(state: state) { slot in
                            state.eventSink(BookingEventSlotSelected(slot: slot))
                        }
                        
                        SummaryAndBookBar(state: state) {
                            state.eventSink(BookingEventContinue())
                        }
                        
                        Spacer().frame(height: 32)
                    }
                    .padding(16)
                }
            }
            .background(MiraTheme.background)
        }
    }
}
