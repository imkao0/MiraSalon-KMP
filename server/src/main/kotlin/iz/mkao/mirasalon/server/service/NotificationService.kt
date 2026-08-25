package iz.mkao.mirasalon.server.service

import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.network.model.event.DomainEventCodec
import iz.mkao.mirasalon.core.domain.model.event.DomainEvent
import iz.mkao.mirasalon.server.data.repository.AppointmentRepository
import iz.mkao.mirasalon.server.data.repository.AppointmentStatus
import iz.mkao.mirasalon.server.data.tables.AppointmentsTable
import iz.mkao.mirasalon.server.data.tables.OutboxTable
import iz.mkao.mirasalon.server.data.tables.SpecialistsTable
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class NotificationService(
    private val appointmentRepository: AppointmentRepository,
    private val clock: Clock = Clock.systemUTC()
) {
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    fun startReminderTask() {
        serviceScope.launch {
            while (isActive) {
                try {
                    checkAndSendReminders()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Napier.e("Reminder check failed; retrying on next tick", e)
                }
                delay(60_000)
            }
        }
    }

    private suspend fun checkAndSendReminders() = newSuspendedTransaction(Dispatchers.IO) {
        val now = clock.millis()
        val thirtyMinutesFromNow = now + (30 * 60 * 1000)
        val thirtyFiveMinutesFromNow = now + (35 * 60 * 1000)

        val upcomingAppointments = (AppointmentsTable innerJoin SpecialistsTable)
            .select(AppointmentsTable.columns + SpecialistsTable.name + SpecialistsTable.imageUrl)
            .where {
                (AppointmentsTable.dateTime greaterEq thirtyMinutesFromNow) and
                (AppointmentsTable.dateTime lessEq thirtyFiveMinutesFromNow) and
                (AppointmentsTable.status eq AppointmentStatus.CONFIRMED.name) and
                (AppointmentsTable.reminderSent eq false)
            }.toList()

        for (row in upcomingAppointments) {
            val userId = row[AppointmentsTable.userId]
            val appointmentId = row[AppointmentsTable.id]
            val appointmentTime = row[AppointmentsTable.dateTime]
            val specialistName = row[SpecialistsTable.name]
            val specialistAvatarUrl = row[SpecialistsTable.imageUrl]

            val event = DomainEvent.AppointmentReminder(
                eventId = UUID.randomUUID().toString(),
                timestamp = now,
                message = "Appointment in 30 minutes",
                appointmentId = appointmentId,
                appointmentTime = appointmentTime,
                reminderType = "30_MINUTES",
                specialistName = specialistName,
                specialistAvatarUrl = specialistAvatarUrl
            )

            OutboxTable.insert {
                it[OutboxTable.eventId] = event.eventId
                it[OutboxTable.userId] = userId
                it[OutboxTable.payload] = DomainEventCodec.encode(event)
                it[OutboxTable.createdAt] = event.timestamp
                it[OutboxTable.dispatched] = false
            }

            AppointmentsTable.update({ AppointmentsTable.id eq appointmentId }) {
                it[reminderSent] = true
            }
        }
    }

    suspend fun sendChatNotification(
        userId: String,
        senderName: String,
        senderAvatarUrl: String? = null,
        messageText: String? = null,
        conversationId: String? = null,
        messageId: String? = null,
        isInternal: Boolean = false,
        audience: String = "CLIENT"
    ) = newSuspendedTransaction(Dispatchers.IO) {
        if (isInternal) return@newSuspendedTransaction
        val displayMessage = messageText ?: "Sent you a message"
        val event = DomainEvent.NotificationReceived(
            eventId = UUID.randomUUID().toString(),
            timestamp = clock.millis(),
            message = displayMessage,
            notificationType = "MESSAGE", // Changed from CHAT_MESSAGE to match NotificationType enum
            referenceId = conversationId,
            senderName = senderName,
            senderAvatarUrl = senderAvatarUrl,
            messageId = messageId
        )
        OutboxTable.insert {
            it[eventId] = event.eventId
            it[OutboxTable.userId] = userId
            it[payload] = DomainEventCodec.encode(event)
            it[createdAt] = event.timestamp
            it[dispatched] = false
            it[OutboxTable.audience] = audience
        }
    }

    fun stop() {
        serviceJob.cancel()
    }
}
