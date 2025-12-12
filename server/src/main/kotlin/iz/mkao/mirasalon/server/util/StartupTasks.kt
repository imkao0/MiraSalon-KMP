package iz.mkao.mirasalon.server.util

import at.favre.lib.crypto.bcrypt.BCrypt
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.core.domain.model.UserRole
import iz.mkao.mirasalon.server.data.repository.OrderRepository
import iz.mkao.mirasalon.server.data.tables.ProductsTable
import iz.mkao.mirasalon.server.data.tables.SalonsTable
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
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

class StartupTasks(
    private val appConfig: AppConfig,
    private val streamSyncService: StreamSyncService,
    private val orderRepository: OrderRepository
) {
    private companion object {
        const val ADMIN_AVATAR_URL = "/uploads/specialists/sarah.jpeg"
    }

    fun run(scope: CoroutineScope) {
        Napier.i("Starting server startup tasks...")
        scope.launch(Dispatchers.IO) {
            try {
                ensureAdminUser()
                seedInitialData()
                syncUsersToStream()
                syncSpecialistsToStream()
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
            val adminEmail = ServerEnvironment.orDefault("ADMIN_EMAIL", "admin@mirasalon.com")
            val adminPassword = ServerEnvironment.secret("ADMIN_PASSWORD")

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
                    it[name] = "Sarah Johnson"
                    it[role] = UserRole.ADMIN.name
                    it[avatarUrl] = ADMIN_AVATAR_URL // Force update existing admin avatar
                }
            }
        }
    }

    private suspend fun seedInitialData() {
        newSuspendedTransaction(Dispatchers.IO) {
            // Seed Salon if empty
            if (SalonsTable.selectAll().empty()) {
                Napier.i("Seeding initial salon data...")
                val salonId = "main-salon"
                SalonsTable.insert {
                    it[id] = salonId
                    it[name] = "Mira Salon Main"
                    it[address] = "123 Beauty Lane, Styledale"
                    it[imageUrl] = "/uploads/salons/main.jpeg"
                    it[phone] = "+1 555-1234"
                    it[rating] = 4.8
                    it[openTime] = "09:00"
                    it[closeTime] = "20:00"
                }
            } else {
                // Ensure existing salon has an image
                SalonsTable.update({ SalonsTable.id eq "main-salon" }) {
                    it[imageUrl] = "/uploads/salons/main.jpeg"
                }
            }

            // Force update or seed Sarah specialist
            val sarahSpecId = "spec-sarah-johnson"
            val sarahExists = SpecialistsTable.selectAll().where { SpecialistsTable.id eq sarahSpecId }.firstOrNull() != null
            if (!sarahExists) {
                Napier.i("Seeding Sarah Johnson specialist...")
                SpecialistsTable.insert {
                    it[id] = sarahSpecId
                    it[this.salonId] = "main-salon"
                    it[name] = "Sarah Johnson"
                    it[role] = "Senior Hair Specialist"
                    it[imageUrl] = "/uploads/specialists/sarah.jpeg"
                    it[bio] = "Expert in color and precision cuts with 10 years of experience."
                    it[status] = "ONLINE"
                    it[isActive] = true
                }
            } else {
                SpecialistsTable.update({ SpecialistsTable.id eq sarahSpecId }) {
                    it[imageUrl] = "/uploads/specialists/sarah.jpeg"
                }
            }

                // Seed Products
                if (ProductsTable.selectAll().empty()) {
                    Napier.i("Seeding initial product: Volume Boost Shampoo")
                    ProductsTable.insert {
                        it[id] = "prod-shampoo-1"
                        it[name] = "Volume Boost Shampoo"
                        it[category] = "Hair Care"
                        it[description] = "Professional grade shampoo for thinning hair."
                        it[price] = 24.99
                        it[stockQuantity] = 50
                        it[imageUrl] = "/uploads/products/shampoo.jpeg"
                        it[createdAt] = System.currentTimeMillis()
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

        Napier.d("Syncing ${usersToSync.size} users to Stream...")
        for ((data, avatarUrl) in usersToSync) {
            val (userId, name, roleStr) = data
            try {
                streamSyncService.syncUser(
                    userId = userId,
                    name = name,
                    role = UserRole.fromString(roleStr),
                    avatarUrl = avatarUrl
                )
            } catch (e: Exception) {
                Napier.w("Failed to sync user $userId to Stream", e)
            }
        }
    }

    private suspend fun syncSpecialistsToStream() {
        val specialistsToSync = newSuspendedTransaction(Dispatchers.IO) {
            SpecialistsTable.selectAll().where { SpecialistsTable.isDeleted eq false }
                .map {
                    Triple(it[SpecialistsTable.id], it[SpecialistsTable.name], it[SpecialistsTable.imageUrl])
                }
        }

        Napier.d("Syncing ${specialistsToSync.size} specialists to Stream...")
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
