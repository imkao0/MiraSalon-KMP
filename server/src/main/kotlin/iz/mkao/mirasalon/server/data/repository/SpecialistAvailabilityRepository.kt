package iz.mkao.mirasalon.server.data.repository

import iz.mkao.mirasalon.core.network.model.dto.SpecialistAvailabilityDto
import iz.mkao.mirasalon.core.network.model.dto.SpecialistShiftDto
import iz.mkao.mirasalon.core.network.model.dto.TimeSlotDto
import iz.mkao.mirasalon.server.data.tables.AppointmentsTable
import iz.mkao.mirasalon.server.data.tables.SalonsTable
import iz.mkao.mirasalon.server.data.tables.SpecialistAbsencesTable
import iz.mkao.mirasalon.server.data.tables.SpecialistShiftsTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class SpecialistAvailabilityRepository {

    fun getAvailableSlots(specialistId: String, dateMillis: Long, duration: Int): SpecialistAvailabilityDto = transaction {
        val utcZone = TimeZone.getTimeZone("UTC")
        val calendar = Calendar.getInstance(utcZone).apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK).let {
            if (it == Calendar.SUNDAY) 7 else it - 1
        }
        
        val startOfDay = calendar.timeInMillis

        val shift = SpecialistShiftsTable.selectAll().where { 
            (SpecialistShiftsTable.specialistId eq specialistId) and 
            (SpecialistShiftsTable.dayOfWeek eq dayOfWeek) and
            (SpecialistShiftsTable.isActive eq true)
        }.singleOrNull()

        if (shift == null) {
            return@transaction SpecialistAvailabilityDto(
                specialistId = specialistId,
                date = dateMillis.toString(),
                availableSlots = emptyList()
            )
        }

        //Return a few slots
        val slots = mutableListOf<TimeSlotDto>()
        
        val salon = SalonsTable.selectAll().limit(1).singleOrNull()
        val salonOpen = salon?.get(SalonsTable.openTime)
        val salonClose = salon?.get(SalonsTable.closeTime)

        val startHour = shift[SpecialistShiftsTable.startTime].split(":")[0].toIntOrNull() 
            ?: salonOpen?.split(":")?.get(0)?.toIntOrNull() ?: 9
        val startMinute = shift[SpecialistShiftsTable.startTime].split(":").getOrNull(1)?.toIntOrNull() ?: 0

        val endHour = shift[SpecialistShiftsTable.endTime].split(":")[0].toIntOrNull()
            ?: salonClose?.split(":")?.get(0)?.toIntOrNull() ?: 18
        val endMinute = shift[SpecialistShiftsTable.endTime].split(":").getOrNull(1)?.toIntOrNull() ?: 0
        
        calendar.set(Calendar.HOUR_OF_DAY, startHour)
        calendar.set(Calendar.MINUTE, startMinute)
        
        val endCal = Calendar.getInstance(utcZone).apply { 
            timeInMillis = calendar.timeInMillis
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, endMinute)
        }

        // Fetch existing appointments for this day to check overlaps
        // Only exclude CANCELLED appointments; CONFIRMED and COMPLETED should block slots
        val dayEnd = startOfDay + (24 * 60 * 60 * 1000)
        val dayAppointments = AppointmentsTable.selectAll().where {
            (AppointmentsTable.specialistId eq specialistId) and
            (AppointmentsTable.status neq AppointmentStatus.CANCELLED.name) and
            (AppointmentsTable.dateTime greaterEq startOfDay) and
            (AppointmentsTable.dateTime less dayEnd)
        }.map {
            it[AppointmentsTable.dateTime] to it[AppointmentsTable.durationMinutes]
        }
        
        val now = System.currentTimeMillis()
        while (calendar.before(endCal)) {
            val slotStart = calendar.timeInMillis
            val slotEnd = slotStart + (duration * 60 * 1000)
            
            // Skip slots that are in the past
            if (slotStart < now) {
                calendar.add(Calendar.MINUTE, duration)
                continue
            }

            // Check for absences
            val isAbsent = SpecialistAbsencesTable.selectAll().where {
                (SpecialistAbsencesTable.specialistId eq specialistId) and
                (SpecialistAbsencesTable.startTime less slotEnd) and
                (SpecialistAbsencesTable.endTime greater slotStart)
            }.any()

            // Check for confirmed appointments
            val isBooked = dayAppointments.any { (appStart, appDuration) ->
                val appEnd = appStart + (appDuration * 60 * 1000)
                slotStart < appEnd && slotEnd > appStart
            }
            
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val amPm = if (hour < 12) "AM" else "PM"
            val hour12 = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            val formatted = String.format(Locale.US, "%d:%02d %s", hour12, minute, amPm)

            slots.add(TimeSlotDto(
                startTime = slotStart,
                endTime = slotEnd,
                isAvailable = !isAbsent && !isBooked,
                formattedTime = formatted
            ))
            
            calendar.add(Calendar.MINUTE, duration)
        }

        SpecialistAvailabilityDto(
            specialistId = specialistId,
            date = dateMillis.toString(),
            availableSlots = slots
        )
    }

    fun getShifts(specialistId: String): List<SpecialistShiftDto> = transaction {
        SpecialistShiftsTable.selectAll().where { SpecialistShiftsTable.specialistId eq specialistId }
            .map {
                SpecialistShiftDto(
                    id = it[SpecialistShiftsTable.id],
                    specialistId = it[SpecialistShiftsTable.specialistId],
                    dayOfWeek = it[SpecialistShiftsTable.dayOfWeek],
                    startTime = it[SpecialistShiftsTable.startTime],
                    endTime = it[SpecialistShiftsTable.endTime],
                    isWorkingDay = it[SpecialistShiftsTable.isActive]
                )
            }
    }

    fun updateShifts(specialistId: String, shifts: List<SpecialistShiftDto>) = transaction {
        SpecialistShiftsTable.deleteWhere { SpecialistShiftsTable.specialistId eq specialistId }
        shifts.forEach { shift ->
            SpecialistShiftsTable.insert {
                it[id] = UUID.randomUUID().toString()
                it[this.specialistId] = specialistId
                it[dayOfWeek] = shift.dayOfWeek
                it[startTime] = shift.startTime
                it[endTime] = shift.endTime
                it[isActive] = shift.isWorkingDay
            }
        }
    }

    fun getAbsences(specialistId: String): List<SpecialistAbsenceRecord> = transaction {
        SpecialistAbsencesTable.selectAll().where { SpecialistAbsencesTable.specialistId eq specialistId }
            .map {
                SpecialistAbsenceRecord(
                    id = it[SpecialistAbsencesTable.id],
                    specialistId = it[SpecialistAbsencesTable.specialistId],
                    startTime = it[SpecialistAbsencesTable.startTime],
                    endTime = it[SpecialistAbsencesTable.endTime],
                    reason = it[SpecialistAbsencesTable.reason],
                    createdAt = it[SpecialistAbsencesTable.createdAt]
                )
            }
    }

    fun addAbsence(specialistId: String, startTime: Long, endTime: Long, reason: String?) = transaction {
        SpecialistAbsencesTable.insert {
            it[id] = UUID.randomUUID().toString()
            it[this.specialistId] = specialistId
            it[this.startTime] = startTime
            it[this.endTime] = endTime
            it[this.reason] = reason
            it[createdAt] = System.currentTimeMillis()
        }
    }

    fun deleteAbsence(absenceId: String) = transaction {
        SpecialistAbsencesTable.deleteWhere { id eq absenceId }
    }
}

data class SpecialistAbsenceRecord(
    val id: String,
    val specialistId: String,
    val startTime: Long,
    val endTime: Long,
    val reason: String?,
    val createdAt: Long
)
