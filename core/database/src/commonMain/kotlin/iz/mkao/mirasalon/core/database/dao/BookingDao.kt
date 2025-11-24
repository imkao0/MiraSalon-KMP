package iz.mkao.mirasalon.core.database.dao

import androidx.room.*
import iz.mkao.mirasalon.core.database.entity.BookingEntity
import iz.mkao.mirasalon.core.database.entity.BookingServiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookingDao {
    @Transaction
    @Query("SELECT * FROM bookings ORDER BY dateTime DESC")
    fun getAllBookingsWithServices(): Flow<List<BookingWithServices>>

    @Transaction
    @Query("SELECT * FROM bookings WHERE id = :id")
    suspend fun getBookingById(id: String): BookingWithServices?

    @Upsert
    suspend fun upsertBooking(booking: BookingEntity)

    @Upsert
    suspend fun upsertBookingServices(services: List<BookingServiceEntity>)

    @Transaction
    suspend fun saveBookingWithServices(booking: BookingEntity, services: List<BookingServiceEntity>) {
        upsertBooking(booking)
        upsertBookingServices(services)
    }

    @Query("DELETE FROM bookings")
    suspend fun deleteAllBookings()
}

data class BookingWithServices(
    @Embedded val booking: BookingEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "bookingId"
    )
    val services: List<BookingServiceEntity>
)
