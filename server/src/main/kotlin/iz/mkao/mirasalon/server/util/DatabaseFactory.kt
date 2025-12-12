package iz.mkao.mirasalon.server.util

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import iz.mkao.mirasalon.server.data.tables.AppointmentsTable
import iz.mkao.mirasalon.server.data.tables.OrderItemsTable
import iz.mkao.mirasalon.server.data.tables.OrdersTable
import iz.mkao.mirasalon.server.data.tables.OutboxTable
import iz.mkao.mirasalon.server.data.tables.ProductCategoriesTable
import iz.mkao.mirasalon.server.data.tables.ProductsTable
import iz.mkao.mirasalon.server.data.tables.PromotionUsagesTable
import iz.mkao.mirasalon.server.data.tables.PromotionsTable
import iz.mkao.mirasalon.server.data.tables.RefreshTokensTable
import iz.mkao.mirasalon.server.data.tables.ReviewsTable
import iz.mkao.mirasalon.server.data.tables.SalonsTable
import iz.mkao.mirasalon.server.data.tables.ServiceCategoriesTable
import iz.mkao.mirasalon.server.data.tables.ServicesTable
import iz.mkao.mirasalon.server.data.tables.SpecialistAbsencesTable
import iz.mkao.mirasalon.server.data.tables.SpecialistClientNotesTable
import iz.mkao.mirasalon.server.data.tables.SpecialistServicesTable
import iz.mkao.mirasalon.server.data.tables.SpecialistShiftsTable
import iz.mkao.mirasalon.server.data.tables.SpecialistsTable
import iz.mkao.mirasalon.server.data.tables.UserAddressesTable
import iz.mkao.mirasalon.server.data.tables.UserNotificationPreferencesTable
import iz.mkao.mirasalon.server.data.tables.UsersTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object DatabaseFactory {

    private val log = LoggerFactory.getLogger(DatabaseFactory::class.java)
    private lateinit var dataSource: HikariDataSource

    fun init(url: String, driver: String, user: String, password: String) {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = url
            driverClassName = driver
            username = user
            this.password = password

            maximumPoolSize = 10
            minimumIdle = 2
            idleTimeout = 300_000       // 5 minutes
            connectionTimeout = 10_000  // 10 seconds
            maxLifetime = 1_800_000     // 30 minutes
            isAutoCommit = false

            leakDetectionThreshold = 30_000

            poolName = "MiraSalonHikariPool"
        }

        dataSource = HikariDataSource(hikariConfig)

        // runMigrations(url, user, password)

        Database.connect(dataSource)

        transaction {
            SchemaUtils.create(
                UsersTable, 
                RefreshTokensTable, 
                SalonsTable, 
                SpecialistsTable,
                ServicesTable, 
                ServiceCategoriesTable, 
                SpecialistServicesTable,
                SpecialistShiftsTable, 
                SpecialistAbsencesTable, 
                SpecialistClientNotesTable,
                AppointmentsTable, 
                ProductsTable, 
                ProductCategoriesTable,
                OrdersTable, 
                OrderItemsTable, 
                ReviewsTable,
                PromotionsTable, 
                PromotionUsagesTable, 
                OutboxTable,
                UserAddressesTable, 
                UserNotificationPreferencesTable
            )
        }
    }


    /*
    private fun runMigrations(url: String, user: String, password: String) {
        log.info("Running Flyway migrations...")
        try {
            val flyway = Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .validateOnMigrate(false)
                .cleanDisabled(false)
                .load()

            flyway.repair()
            flyway.migrate()
            log.info("Flyway migrations completed successfully")
        } catch (e: Exception) {
            log.warn("Flyway migrations failed: {}. Schema will be created by SchemaUtils.", e.message)
        }
    }
    */

    /**
     * Liveness probe: returns true only if a real DB connection can be obtained
     * and a simple query executes successfully.
     */
    fun isHealthy(): Boolean {
        return try {
            dataSource.connection.use { conn ->
                conn.prepareStatement("SELECT 1").use { stmt ->
                    stmt.executeQuery().next()
                }
            }
        } catch (e: Exception) {
            log.warn("Health check failed: {}", e.message)
            false
        }
    }
}
