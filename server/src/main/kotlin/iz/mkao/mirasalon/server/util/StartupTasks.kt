package iz.mkao.mirasalon.server.util

import at.favre.lib.crypto.bcrypt.BCrypt
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.domain.model.UserRole
import iz.mkao.mirasalon.server.data.repository.OrderRepository
import iz.mkao.mirasalon.server.data.repository.PromotionRepository
import iz.mkao.mirasalon.server.data.repository.UserRepository
import iz.mkao.mirasalon.server.data.tables.SpecialistsTable
import iz.mkao.mirasalon.server.data.tables.UsersTable
import iz.mkao.mirasalon.server.service.StreamSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.Calendar
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class StartupTasks(
    private val userRepository: UserRepository,
    private val streamSyncService: StreamSyncService,
    private val orderRepository: OrderRepository,
    private val promotionRepository: PromotionRepository
) {
    private companion object {
        const val ADMIN_AVATAR_URL = "/uploads/specialists/sarah.jpeg"
    }

    fun run(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                ensureAdminUser()
                syncUsersToStream()
                syncSpecialistsToStream()
            } catch (e: Exception) {
                Napier.e("StartupTasks failed", e)
            }
        }

        scope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val cancelledCount = orderRepository.cleanupExpiredOrders()
                    if (cancelledCount > 0) {
                        Napier.i("Cancelled $cancelledCount expired orders")
                    }
                } catch (e: Exception) {
                    Napier.e("Order cleanup failed", e)
                }
                delay((60 * 60 * 1000).milliseconds)
            }
        }
    }

    private suspend fun ensureAdminUser() {
        newSuspendedTransaction(Dispatchers.IO) {
            val adminEmail = ServerEnvironment.orDefault("ADMIN_EMAIL", "admin@mirasalon.com")
            val adminPassword = ServerEnvironment.secret("ADMIN_PASSWORD")

            val existing = UsersTable
                .selectAll().where { UsersTable.email eq adminEmail }
                .firstOrNull()

            val passwordHash = BCrypt.withDefaults()
                .hashToString(12, adminPassword.toCharArray())

            if (existing == null) {
                val adminId = UUID.randomUUID().toString()
                UsersTable.insert {
                    it[UsersTable.id] = adminId
                    it[UsersTable.name] = "Sarah Johnson"
                    it[UsersTable.email] = adminEmail
                    it[UsersTable.passwordHash] = passwordHash
                    it[UsersTable.role] = UserRole.ADMIN.name
                    it[UsersTable.isActive] = true
                    it[UsersTable.avatarUrl] = ADMIN_AVATAR_URL
                    it[UsersTable.createdAt] = System.currentTimeMillis()
                }
            } else {
                UsersTable.update({ UsersTable.email eq adminEmail }) {
                    it[UsersTable.passwordHash] = passwordHash
                    it[UsersTable.name] = "Sarah Johnson"
                    it[UsersTable.role] = UserRole.ADMIN.name
                    if (existing[UsersTable.avatarUrl].isNullOrBlank()) {
                        it[UsersTable.avatarUrl] = ADMIN_AVATAR_URL
                    }
                }
            }
        }
    }

    private suspend fun syncUsersToStream() {
        val usersToSync = newSuspendedTransaction(Dispatchers.IO) {
            UsersTable.selectAll().where { UsersTable.isActive eq true }
                .map {
                    Triple(it[UsersTable.id], it[UsersTable.name], it[UsersTable.role]) to it[UsersTable.avatarUrl]
                }
        }

        for ((data, avatarUrl) in usersToSync) {
            val (userId, name, roleStr) = data
            streamSyncService.syncUser(
                userId = userId,
                name = name,
                role = UserRole.fromString(roleStr),
                avatarUrl = avatarUrl
            )
        }
    }

    private suspend fun syncSpecialistsToStream() {
        val specialistsToSync = newSuspendedTransaction(Dispatchers.IO) {
            SpecialistsTable.selectAll().where { SpecialistsTable.isDeleted eq false }
                .map {
                    Triple(it[SpecialistsTable.id], it[SpecialistsTable.name], it[SpecialistsTable.imageUrl])
                }
        }

        for ((id, name, imageUrl) in specialistsToSync) {
            try {
                streamSyncService.syncUser(
                    userId = id,
                    name = name,
                    role = UserRole.SPECIALIST,
                    avatarUrl = imageUrl
                )
            } catch (e: Exception) {
                Napier.e("Failed to sync specialist $id to Stream", e)
            }
        }
    }
}
