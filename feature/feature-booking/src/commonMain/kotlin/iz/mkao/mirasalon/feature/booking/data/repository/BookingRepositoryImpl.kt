package iz.mkao.mirasalon.feature.booking.data.repository

import iz.mkao.mirasalon.core.database.dao.BookingDao
import iz.mkao.mirasalon.core.domain.model.Service
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.dto.CreateAppointmentRequest
import iz.mkao.mirasalon.core.network.model.dto.SubmitReviewRequest
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import iz.mkao.mirasalon.feature.booking.data.mappers.toBookingSpecialist
import iz.mkao.mirasalon.feature.booking.data.mappers.toConfirmedBooking
import iz.mkao.mirasalon.feature.booking.data.mappers.toDomain
import iz.mkao.mirasalon.feature.booking.data.mappers.toEntity
import iz.mkao.mirasalon.feature.booking.data.network.api.BookingApi
import iz.mkao.mirasalon.feature.booking.domain.model.BookingSpecialist
import iz.mkao.mirasalon.feature.booking.domain.model.BookingTimeSlot
import iz.mkao.mirasalon.feature.booking.domain.model.ConfirmedBooking
import iz.mkao.mirasalon.feature.profile.domain.model.UserProfile
import iz.mkao.mirasalon.feature.profile.domain.repository.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookingRepositoryImpl(
    private val api: BookingApi,
    private val bookingDao: BookingDao,
    private val profileRepository: ProfileRepository,
    private val realtimeGateway: RealtimeGateway,
    private val repositoryScope: CoroutineScope
) : BookingRepository {

    override val confirmedBookings: StateFlow<List<ConfirmedBooking>> =
        bookingDao.getAllBookingsWithServices()
            .map { list ->
                val bookings = list.map { it.toDomain() }

                _remindersEnabled.value = bookings.associate { it.id to it.reminderEnabled }
                bookings
            }
            .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    init {
        repositoryScope.launch {
            realtimeGateway.events.collectLatest { event ->
                when (event) {
                    is DomainEvent.BookingCreated,
                    is DomainEvent.BookingUpdated -> {
                        refreshBookings()
                    }
                    is DomainEvent.ReviewSubmitted -> {
                        if (event.targetType == "APPOINTMENT") {
                            refreshBookings()
                        }
                    }
                    else -> {}
                }
            }
        }
        repositoryScope.launch { refreshBookings() }
    }

    private val _latestBooking = MutableStateFlow<ConfirmedBooking?>(null)
    override val latestBooking: StateFlow<ConfirmedBooking?> = _latestBooking.asStateFlow()

    private val _remindersEnabled = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val remindersEnabled: StateFlow<Map<String, Boolean>> = _remindersEnabled.asStateFlow()

    override suspend fun refreshBookings() {
        when (val result = api.fetchAppointments()) {
            is Outcome.Success -> {
                val profile = (profileRepository.getProfile() as? Outcome.Success<UserProfile>)?.data
                result.data.items.forEach { dto ->
                    println("DEBUG: Appointment ${dto.id} status=${dto.status} isReviewed=${dto.isReviewed}")
                    val entity = dto.toEntity().let {
                        it.copy(
                            userName = it.userName ?: profile?.fullName,
                            userEmail = it.userEmail ?: profile?.email
                        )
                    }
                    bookingDao.saveBookingWithServices(
                        entity,
                        dto.services.distinctBy { it.id }.map { it.toEntity(dto.id) }
                    )
                }
            }
            is Outcome.Error -> println("DEBUG: Failed to refresh bookings: ${result.failure}")
            is Outcome.Loading -> {}
        }
    }

    override suspend fun getServices(serviceIds: List<String>): List<Service> {
        val result = api.fetchServices(null, null)
        if (result !is Outcome.Success) return emptyList()
        return result.data.filter { it.id in serviceIds }.map { it.toDomain() }
    }

    override suspend fun getSpecialistsForService(serviceId: String): List<BookingSpecialist> {
        val result = api.fetchSpecialists()
        if (result !is Outcome.Success) return emptyList()
        return result.data.map { it.toBookingSpecialist() }
    }

    override suspend fun getTimeSlots(specialistId: String, date: String): List<BookingTimeSlot> {
        return when (val result = api.fetchAvailability(specialistId, date)) {
            is Outcome.Success -> result.data.availableSlots.map { it.toDomain() }
            else -> emptyList()
        }
    }

    override suspend fun lockSlot(slotId: String): Result<Unit> = Result.success(Unit)

    override suspend fun getDefaultSalonId(): String = "main-salon"

    override suspend fun createBooking(
        specialistId: String,
        salonId: String,
        serviceIds: List<String>,
        dateTime: Long,
        reminderEnabled: Boolean
    ): Result<ConfirmedBooking> {
        val request = CreateAppointmentRequest(salonId, specialistId, dateTime, serviceIds)
        return when (val result = api.createAppointment(request)) {
            is Outcome.Success -> {
                val dto = result.data
                val profile = (profileRepository.getProfile() as? Outcome.Success<UserProfile>)?.data
                
                val booking = dto.toConfirmedBooking().copy(
                    reminderEnabled = reminderEnabled,
                    customerName = dto.userName ?: profile?.fullName.orEmpty(),
                    customerEmail = dto.userEmail ?: profile?.email.orEmpty()
                )
                
                val entity = dto.toEntity().copy(
                    reminderEnabled = reminderEnabled,
                    userName = dto.userName ?: profile?.fullName,
                    userEmail = dto.userEmail ?: profile?.email
                )
                
                bookingDao.saveBookingWithServices(
                    entity,
                    dto.services.distinctBy { it.id }.map { it.toEntity(dto.id) }
                )
                
                Result.success(booking)
            }
            is Outcome.Error -> {
                val message = when (val failure = result.failure) {
                    is Failure.ServerError -> failure.message
                    is Failure.ClientError -> failure.message
                    is Failure.NetworkConnection -> failure.message
                    else -> failure.toString()
                }
                Result.failure(Exception(message))
            }
            is Outcome.Loading -> Result.failure(Exception("Loading"))
        }
    }

    override suspend fun cancelBooking(id: String): Result<Unit> {
        return when (val result = api.cancelAppointment(id)) {
            is Outcome.Success -> {

                Result.success(Unit)
            }
            is Outcome.Error -> {
                val message = when (val failure = result.failure) {
                    is Failure.ServerError -> failure.message
                    is Failure.ClientError -> failure.message
                    is Failure.NetworkConnection -> failure.message
                    else -> failure.toString()
                }
                Result.failure(Exception(message))
            }
            is Outcome.Loading -> Result.failure(Exception("Loading"))
        }
    }

    override fun setReminder(bookingId: String, enabled: Boolean) {
        repositoryScope.launch {
            val booking = bookingDao.getBookingById(bookingId)
            if (booking != null) {
                bookingDao.upsertBooking(booking.booking.copy(reminderEnabled = enabled))
            }
        }
    }

    override fun getBookingById(id: String): ConfirmedBooking? = 
        confirmedBookings.value.find { it.id == id }

    override suspend fun submitReview(bookingId: String, rating: Int, comment: String): Result<Unit> {
        return when (val result = api.submitReview(
            bookingId,
            SubmitReviewRequest(
                rating = rating,
                comment = comment,
                targetId = bookingId,
                targetType = "APPOINTMENT"
            )
        )) {
            is Outcome.Success -> {
                refreshBookings()
                Result.success(Unit)
            }
            is Outcome.Error -> {
                val message = when (val failure = result.failure) {
                    is Failure.ServerError -> failure.message
                    is Failure.ClientError -> failure.message
                    is Failure.NetworkConnection -> failure.message
                    else -> failure.toString()
                }
                Result.failure(Exception(message))
            }
            is Outcome.Loading -> Result.failure(Exception("Loading"))
        }
    }

    override suspend fun updateReminderEnabled(bookingId: String, enabled: Boolean): Result<Unit> {
        return when (val result = api.updateReminderEnabled(bookingId, enabled)) {
            is Outcome.Success -> {
                val booking = bookingDao.getBookingById(bookingId)
                if (booking != null) {
                    bookingDao.upsertBooking(booking.booking.copy(reminderEnabled = enabled))
                }
                Result.success(Unit)
            }
            is Outcome.Error -> {
                val message = when (val failure = result.failure) {
                    is Failure.ServerError -> failure.message
                    is Failure.ClientError -> failure.message
                    is Failure.NetworkConnection -> failure.message
                    else -> failure.toString()
                }
                Result.failure(Exception(message))
            }
            is Outcome.Loading -> Result.failure(Exception("Loading"))
        }
    }
}
