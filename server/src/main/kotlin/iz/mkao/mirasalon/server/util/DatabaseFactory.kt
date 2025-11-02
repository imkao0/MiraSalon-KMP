package iz.mkao.mirasalon.server.util

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
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

        runMigrations(url, user, password)

        Database.connect(dataSource)
    }


    private fun runMigrations(url: String, user: String, password: String) {
        log.info("Running Flyway migrations...")
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
    }

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
