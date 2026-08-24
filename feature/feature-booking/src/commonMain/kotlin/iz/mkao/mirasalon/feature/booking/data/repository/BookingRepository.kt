package iz.mkao.mirasalon.feature.booking.data.repository

import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.feature.booking.domain.model.BookingSpecialist
import iz.mkao.mirasalon.feature.booking.domain.model.BookingTimeSlot
import iz.mkao.mirasalon.feature.booking.domain.model.ConfirmedBooking
import kotlinx.coroutines.flow.StateFlow

/**
 * Shared booking repository.
 * Exposes confirmed bookings as a [StateFlow] so a confirmed booking can be
 * shown on the e-receipt and listed on the bookings screen.
 * Falls back to a simulated server response when the network fails.
 */
interface BookingRepository {

    /** All confirmed bookings, most-recent first. */
    val confirmedBookings: StateFlow<List<ConfirmedBooking>>

    /**
     * Fetches the user's bookings from the server.
     * Replaces the local [confirmedBookings] state.
     * Called when the bookings screen is shown so the Upcoming / Completed /
     * Cancelled tabs always reflect server data.
     */
    suspend fun refreshBookings()

    /** The most recently confirmed booking (used by the e-receipt). */
    val latestBooking: StateFlow<ConfirmedBooking?>

    /** Per-booking "Remind me" toggle states keyed by booking id. */
    val remindersEnabled: StateFlow<Map<String, Boolean>>

    suspend fun getServices(serviceIds: List<String>): List<Service>

    suspend fun getSpecialistsForService(serviceId: String): List<BookingSpecialist>

    /**
     * Fetch available time slots for [specialistId] on [date] (YYYY-MM-DD).
     * Uses the project's networking layer.
     * If the request fails it simulates a plausible server response so the
     * booking flow remains usable.
     */
    suspend fun getTimeSlots(
        specialistId: String,
        date: String,
        duration: Int? = null
    ): List<BookingTimeSlot>

    /** Optimistically lock a slot for a short duration. */
    suspend fun lockSlot(slotId: String): Result<Unit>

    /** Resolve the default salon id used when creating an appointment. */
    suspend fun getDefaultSalonId(): String

    /**
     * Create a booking.
     * On network failure a local booking is simulated so the flow can still
     * navigate to the e-receipt.
     * Returns the confirmed booking.
     */
    suspend fun createBooking(
        specialistId: String,
        salonId: String,
        serviceIds: List<String>,
        dateTime: Long,
        reminderEnabled: Boolean = false
    ): Result<ConfirmedBooking>

    /**
     * Cancel a booking.
     * Tries the server first, but always updates the local [confirmedBookings]
     * state (status -> Cancelled) so the bookings screen moves the card to the
     * Cancelled tab immediately.
     */
    suspend fun cancelBooking(id: String): Result<Unit>

    /** Persist the per-booking "Remind me" toggle state. */
    fun setReminder(
        bookingId: String,
        enabled: Boolean
    )

    suspend fun getBookingById(id: String): ConfirmedBooking?

    suspend fun submitReview(bookingId: String, rating: Int, comment: String): Result<Unit>

    suspend fun updateReminderEnabled(bookingId: String, enabled: Boolean): Result<Unit>
}
