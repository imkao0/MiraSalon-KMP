package iz.mkao.mirasalon.server.util

import at.favre.lib.crypto.bcrypt.BCrypt
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.domain.model.UserRole
import iz.mkao.mirasalon.server.data.repository.OrderRepository
import iz.mkao.mirasalon.server.data.repository.SpecialistRepository
import iz.mkao.mirasalon.server.data.repository.UserRepository
import iz.mkao.mirasalon.server.data.tables.UsersTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class StartupTasks(
    private val appConfig: AppConfig,
    private val orderRepository: OrderRepository,
    private val specialistRepository: SpecialistRepository,
    private val userRepository: UserRepository
) {
    private companion object {
        const val ADMIN_AVATAR_URL = "/uploads/images/avatars/066c3f4a-1460-4009-8f1c-0ee720cccc1d.jpg"
    }

    fun run(scope: CoroutineScope) {
        Napier.i("Starting server startup tasks...")
        scope.launch(Dispatchers.IO) {
            try {
                ensureAdminUser()
                Napier.i("All startup tasks completed successfully.")
            } catch (e: Exception) {
                Napier.e("StartupTasks critical failure", e)
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
                    Napier.e("Order cleanup background task failed", e)
                }
                delay((60 * 60 * 1000).milliseconds)
            }
        }
    }

    private suspend fun ensureAdminUser() {
        newSuspendedTransaction(Dispatchers.IO) {
            val adminEmail = ServerEnvironment.orDefault("ADMIN_EMAIL", "admin@mirasalon")
            val adminPassword = ServerEnvironment.orDefault("ADMIN_PASSWORD", "password")

            val existing = UsersTable
                .selectAll().where { UsersTable.email eq adminEmail }
                .firstOrNull()

            val passwordHash = BCrypt.withDefaults()
                .hashToString(12, adminPassword.toCharArray())

            if (existing == null) {
                val adminId = UUID.randomUUID().toString()
                Napier.i("Creating default admin user: $adminEmail")
                UsersTable.insert {
                    it[id] = adminId
                    it[name] = "Sarah Johnson"
                    it[email] = adminEmail
                    it[this.passwordHash] = passwordHash
                    it[role] = UserRole.ADMIN.name
                    it[isActive] = true
                    it[avatarUrl] = ADMIN_AVATAR_URL
                    it[createdAt] = System.currentTimeMillis()
                }
            } else {
                UsersTable.update({ UsersTable.email eq adminEmail }) {
                    it[this.passwordHash] = passwordHash
                    it[name] = "Linet Johnson"
                    it[role] = UserRole.ADMIN.name
                    it[avatarUrl] = ADMIN_AVATAR_URL
                }
            }
        }
    }
}
