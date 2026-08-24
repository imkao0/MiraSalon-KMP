import SwiftUI
import ComposeApp

// MARK: - Calendar Section
struct CalendarSection: View {
    let state: BookingState
    let onDateSelected: (Kotlinx_datetimeLocalDate) -> Void
    let onToggle: () -> Void
    
    private let weekdays = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
    
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack {
                if let selected = state.selectedDate {
                    Text(getMonthName(selected.monthNumber))
                        .font(.system(size: 28, weight: .light))
                        .foregroundColor(MiraTheme.onSurfaceVariant.opacity(0.5))
                }
                
                Spacer()
                
                Button(action: onToggle) {
                    Image(systemName: state.calendarExpanded ? "chevron.up" : "chevron.down")
                        .foregroundColor(MiraTheme.primary)
                        .padding(8)
                }
            }
            .padding(.bottom, 8)
            
            // Weekday Header
            HStack(spacing: 0) {
                ForEach(weekdays, id: \.self) { day in
                    Text(day)
                        .font(MiraType.labelSmall)
                        .foregroundColor(MiraTheme.onSurfaceVariant.opacity(0.5))
                        .frame(maxWidth: .infinity)
                }
            }
            .padding(.bottom, 12)
            
            // Grid
            let daysToShow = state.calendarExpanded ? state.days : Array(state.days.prefix(7))
            let chunks = daysToShow.chunked(into: 7)
            
            VStack(spacing: 16) {
                ForEach(0..<chunks.count, id: \.self) { rowIndex in
                    let rowDays = chunks[rowIndex]
                    let isLastRowOfCollapsed = !state.calendarExpanded && rowIndex == 0
                    
                    ZStack {
                        HStack(spacing: 16) {
                            ForEach(rowDays, id: \.self) { date in
                                DateCell(
                                    date: date,
                                    isSelected: date == state.selectedDate,
                                    hasBookings: state.datesWithBookings.contains(date.description),
                                    onClick: { onDateSelected(date) }
                                )
                                .frame(maxWidth: .infinity)
                            }
                            
                            // Fill empty slots in last row
                            if rowDays.count < 7 {
                                ForEach(0..<(7 - rowDays.count), id: \.self) { _ in
                                    Spacer().frame(maxWidth: .infinity)
                                }
                            }
                        }
                        
                        // Fade effect for collapsed state
                        if !state.calendarExpanded && rowIndex >= 1 {
                             // This case is actually handled by daysToShow but keeping structure similar to Kotlin
                        }
                    }
                }
            }
        }
    }
    
    private func getMonthName(_ month: Int32) -> String {
        let fmt = DateFormatter()
        return fmt.monthSymbols[Int(month) - 1]
    }
}

struct DateCell: View {
    let date: Kotlinx_datetimeLocalDate
    let isSelected: Bool
    let hasBookings: Bool
    let onClick: () -> Void
    
    var body: some View {
        Button(action: onClick) {
            VStack(spacing: 4) {
                Text("\(date.dayOfMonth)")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(isSelected ? .white : MiraTheme.onSurface)
                
                if hasBookings {
                    Circle()
                        .fill(isSelected ? .white : MiraTheme.primary)
                        .frame(width: 4, height: 4)
                } else {
                    Spacer().frame(height: 4)
                }
            }
            .frame(width: 44, height: 44)
            .background(isSelected ? MiraTheme.primary : MiraTheme.surface)
            .cornerRadius(8)
            .miraOutlineBorder(cornerRadius: 8, color: isSelected ? .clear : MiraTheme.outlineVariant.opacity(0.5))
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Specialist Section
struct SpecialistSection: View {
    let state: BookingState
    let onSpecialistSelected: (String) -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Select Specialist")
                .font(MiraType.titleMedium)
                .bold()
            
            if state.isLoadingSpecialists {
                HStack(spacing: 16) {
                    ForEach(0..<4, id: \.self) { _ in
                        MiraShimmerBlock(width: 72, height: 88, cornerRadius: 36)
                    }
                }
            } else if state.specialists.isEmpty {
                Text("No specialists available for this service.")
                    .font(MiraType.bodyMedium)
                    .foregroundColor(MiraTheme.onSurfaceVariant)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 16) {
                        ForEach(state.specialists, id: \.id) { specialist in
                            SpecialistAvatar(
                                specialist: specialist,
                                isSelected: specialist.id == state.selectedSpecialistId,
                                onClick: { onSpecialistSelected(specialist.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

struct SpecialistAvatar: View {
    let specialist: BookingSpecialist
    let isSelected: Bool
    let onClick: () -> Void
    
    var body: some View {
        Button(action: onClick) {
            VStack(spacing: 4) {
                ZStack {
                    let resolvedUrl = ApiEndpoints.shared.resolveImageUrl(imagePath: specialist.imageUrl)
                    AsyncImage(url: URL(string: resolvedUrl ?? "")) { image in
                        image.resizable().aspectRatio(contentMode: .fill)
                    } placeholder: {
                        Image(systemName: "person.fill")
                            .foregroundColor(MiraTheme.onSurfaceVariant.opacity(0.5))
                    }
                    .frame(width: 64, height: 64)
                    .clipShape(Circle())
                    .padding(2)
                    .overlay(
                        Circle()
                            .stroke(isSelected ? MiraTheme.primary : MiraTheme.outlineVariant.opacity(0.5), lineWidth: isSelected ? 2 : 1)
                    )
                }
                
                Text(specialist.name.components(separatedBy: " ").first ?? "")
                    .font(MiraType.labelSmall)
                    .fontWeight(isSelected ? .bold : .medium)
                    .foregroundColor(isSelected ? MiraTheme.primary : MiraTheme.onSurfaceVariant)
                    .lineLimit(1)
            }
            .frame(width: 72)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Time Slot Section
struct TimeSlotSection: View {
    let state: BookingState
    let onSlotSelected: (BookingTimeSlot) -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Select Time")
                .font(MiraType.titleMedium)
                .bold()
            
            if state.isLoadingSlots {
                HStack(spacing: 8) {
                    ForEach(0..<4, id: \.self) { _ in
                        MiraShimmerBlock(height: 40, cornerRadius: 8)
                            .frame(maxWidth: .infinity)
                    }
                }
            } else if state.timeSlots.isEmpty {
                Text("No available time slots for this date.")
                    .font(MiraType.bodyMedium)
                    .foregroundColor(MiraTheme.onSurfaceVariant)
            } else {
                let grouped = groupSlots(state.timeSlots)
                
                VStack(spacing: 16) {
                    if !grouped.morning.isEmpty {
                        SlotGroup(title: "Morning", slots: grouped.morning, selectedSlot: state.selectedSlot, onSlotSelected: onSlotSelected)
                    }
                    if !grouped.afternoon.isEmpty {
                        SlotGroup(title: "Afternoon", slots: grouped.afternoon, selectedSlot: state.selectedSlot, onSlotSelected: onSlotSelected)
                    }
                    if !grouped.evening.isEmpty {
                        SlotGroup(title: "Evening", slots: grouped.evening, selectedSlot: state.selectedSlot, onSlotSelected: onSlotSelected)
                    }
                }
            }
        }
    }
    
    private func groupSlots(_ slots: [BookingTimeSlot]) -> (morning: [BookingTimeSlot], afternoon: [BookingTimeSlot], evening: [BookingTimeSlot]) {
        var morning: [BookingTimeSlot] = []
        var afternoon: [BookingTimeSlot] = []
        var evening: [BookingTimeSlot] = []
        
        let calendar = Calendar.current
        for slot in slots {
            let date = Date(timeIntervalSince1970: Double(slot.startTime) / 1000.0)
            let hour = calendar.component(.hour, from: date)
            
            if hour < 12 {
                morning.append(slot)
            } else if hour < 17 {
                afternoon.append(slot)
            } else {
                evening.append(slot)
            }
        }
        return (morning, afternoon, evening)
    }
}

struct SlotGroup: View {
    let title: String
    let slots: [BookingTimeSlot]
    let selectedSlot: BookingTimeSlot?
    let onSlotSelected: (BookingTimeSlot) -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(MiraType.labelMedium)
                .foregroundColor(MiraTheme.primary)
                .bold()
            
            let columns = Array(repeating: GridItem(.flexible(), spacing: 8), count: 4)
            LazyVGrid(columns: columns, spacing: 8) {
                ForEach(slots, id: \.self) { slot in
                    TimeSlotChip(
                        slot: slot,
                        isSelected: slot == selectedSlot,
                        onClick: { onSlotSelected(slot) }
                    )
                }
            }
        }
    }
}

struct TimeSlotChip: View {
    let slot: BookingTimeSlot
    let isSelected: Bool
    let onClick: () -> Void
    
    var body: some View {
        Button(action: onClick) {
            Text(slot.formattedTime)
                .font(MiraType.labelMedium)
                .fontWeight(.medium)
                .foregroundColor(isSelected ? .white : (slot.isAvailable ? MiraTheme.onSurface : MiraTheme.onSurfaceVariant.opacity(0.5)))
                .frame(maxWidth: .infinity)
                .frame(height: 40)
                .background(isSelected ? Color(red: 0, green: 122/255, blue: 255/255) : (slot.isAvailable ? MiraTheme.surface : MiraTheme.surfaceVariant))
                .cornerRadius(8)
                .miraOutlineBorder(cornerRadius: 8, color: (isSelected || !slot.isAvailable) ? .clear : MiraTheme.outlineVariant.opacity(0.6))
        }
        .buttonStyle(.plain)
        .disabled(!slot.isAvailable)
    }
}

// MARK: - Booking Dropdown Sheet
struct BookingDropdownSheetView: View {
    let expanded: Bool
    let date: Kotlinx_datetimeLocalDate?
    let bookings: [ConfirmedBooking]
    var onMessageClick: (String) -> Void = { _ in }

    var body: some View {
        if expanded {
            VStack(alignment: .leading, spacing: 12) {
                if let date = date {
                    Text(formatSheetDateLabel(date))
                        .font(MiraType.labelMedium)
                        .bold()
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                }
                
                if bookings.isEmpty {
                    Text("No existing appointments on this day")
                        .font(MiraType.bodyMedium)
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .multilineTextAlignment(.center)
                } else {
                    VStack(spacing: 12) {
                        ForEach(bookings, id: \.id) { booking in
                            SheetAppointmentCard(
                                booking: booking,
                                onMessageClick: onMessageClick
                            )
                        }
                    }
                }
                
                Divider()
                    .background(MiraTheme.outlineVariant.opacity(0.5))
                    .padding(.top, 16)
            }
            .padding(.vertical, 12)
            .transition(.move(edge: .top).combined(with: .opacity))
        }
    }
    
    private func formatSheetDateLabel(_ date: Kotlinx_datetimeLocalDate) -> String {
        let fmt = DateFormatter()
        fmt.dateFormat = "MMM d, EEEE"
        let components = DateComponents(year: Int(date.year), month: Int(date.monthNumber), day: Int(date.dayOfMonth))
        if let d = Calendar.current.date(from: components) {
            return fmt.string(from: d).uppercased()
        }
        return ""
    }
}

struct SheetAppointmentCard: View {
    let booking: ConfirmedBooking
    let onMessageClick: (String) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(booking.timeSlotLabel.isEmpty ? formatBookingTime(booking.dateTime) : booking.timeSlotLabel)
                .font(MiraType.labelSmall)
                .fontWeight(.semibold)
                .foregroundColor(MiraTheme.onSurfaceVariant)

            HStack(alignment: .center, spacing: 10) {
                let resolvedUrl = ApiEndpoints.shared.resolveImageUrl(imagePath: booking.specialistImageUrl ?? booking.salonImageUrl)
                AsyncImage(url: URL(string: resolvedUrl ?? "")) { image in
                    image.resizable().aspectRatio(contentMode: .fill)
                } placeholder: {
                    Image(systemName: "person.fill")
                        .foregroundColor(MiraTheme.onSurfaceVariant.opacity(0.5))
                }
                .frame(width: 44, height: 44)
                .clipShape(RoundedRectangle(cornerRadius: MiraTheme.radiusDefault))
                
                VStack(alignment: .leading, spacing: 0) {
                    Text(booking.specialistName.isEmpty ? booking.salonName : booking.specialistName)
                        .font(MiraType.titleSmall)
                        .bold()
                        .foregroundColor(MiraTheme.onSurface)
                        .lineLimit(1)

                    Text(booking.services.map { $0.name }.joined(separator: ", ").isEmpty ? "Service" : booking.services.map { $0.name }.joined(separator: ", "))
                        .font(MiraType.bodySmall)
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                        .lineLimit(1)
                }
            }
            .padding(.top, 10)
            
            HStack(alignment: .center) {
                StatusPill(label: booking.status.name, isPaid: booking.status == .confirmed)

                Spacer()

                Button(action: { onMessageClick(booking.id) }) {
                    Image(systemName: "bubble.left")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 14, height: 14)
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                        .frame(width: 30, height: 30)
                }
                .buttonStyle(.plain)
            }
            .padding(.top, 12)
        }
        .padding(14)
        .background(MiraTheme.surfaceVariant.opacity(0.5))
        .cornerRadius(MiraTheme.radiusDefault)
    }
    
    private func formatBookingTime(_ epochMillis: Int64) -> String {
        let date = Date(timeIntervalSince1970: Double(epochMillis) / 1000.0)
        let fmt = DateFormatter()
        fmt.dateFormat = "h:mm a"
        return fmt.string(from: date)
    }
}

struct StatusPill: View {
    let label: String
    let isPaid: Bool
    
    var body: some View {
        HStack(spacing: 5) {
            if isPaid {
                Circle()
                    .fill(MiraTheme.secondary)
                    .frame(width: 5, height: 5)
            }

            Text(label.uppercased())
                .font(MiraType.labelSmall)
                .fontWeight(.semibold)
                .foregroundColor(isPaid ? MiraTheme.onSecondaryContainer : MiraTheme.onSurfaceVariant)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 4)
        .background(
            isPaid ? MiraTheme.secondaryContainer.opacity(0.5) : MiraTheme.surfaceVariant
        )
        .cornerRadius(MiraTheme.radiusDefault)
    }
}

// MARK: - Summary & Book Bar
struct SummaryAndBookBar: View {
    let state: BookingState
    let onBook: () -> Void
    
    var body: some View {
        VStack(spacing: 0) {
            Divider()
                .background(MiraTheme.outlineVariant.opacity(0.5))
            
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 0) {
                    Text("Total")
                        .font(MiraType.titleSmall)
                        .foregroundColor(MiraTheme.onSurfaceVariant)
                    
                    Text("$\(String(format: "%.2f", state.totalAmount))")
                        .font(MiraType.headlineSmall)
                        .bold()
                        .foregroundColor(MiraTheme.onSurface)
                    
                    if let date = state.selectedDate {
                        Text(formatDateLabel(date))
                            .font(MiraType.labelSmall)
                            .foregroundColor(MiraTheme.onSurfaceVariant)
                    }
                    
                    if let slot = state.selectedSlot {
                        Text(slot.formattedTime)
                            .font(MiraType.labelMedium)
                            .fontWeight(.semibold)
                            .foregroundColor(MiraTheme.onSurface)
                    }
                }
                
                Spacer()
                
                Button(action: onBook) {
                    ZStack {
                        if state.isBooking {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        } else {
                            Text("Continue")
                                .font(MiraType.titleMedium)
                                .bold()
                        }
                    }
                    .frame(width: 140, height: 50)
                    .background(state.canBook ? MiraTheme.primary : MiraTheme.surfaceVariant)
                    .foregroundColor(state.canBook ? MiraTheme.onPrimary : MiraTheme.onSurfaceVariant)
                    .cornerRadius(8)
                }
                .disabled(!state.canBook || state.isBooking)
            }
            .padding(.top, 16)
            
            if let error = state.bookingError {
                Text(error)
                    .font(MiraType.bodySmall)
                    .foregroundColor(MiraTheme.error)
                    .padding(.top, 8)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
    
    private func formatDateLabel(_ date: Kotlinx_datetimeLocalDate) -> String {
        let fmt = DateFormatter()
        fmt.dateFormat = "EEE d, yyyy"
        let components = DateComponents(year: Int(date.year), month: Int(date.monthNumber), day: Int(date.dayOfMonth))
        if let d = Calendar.current.date(from: components) {
            return fmt.string(from: d).uppercased()
        }
        return ""
    }
}

// MARK: - Array Helper
extension Array {
    func chunked(into size: Int) -> [[Element]] {
        return stride(from: 0, to: count, by: size).map {
            Array(self[$0 ..< Swift.min($0 + size, count)])
        }
    }
}
