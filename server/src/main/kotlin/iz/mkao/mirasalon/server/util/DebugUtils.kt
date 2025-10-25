package iz.mkao.mirasalon.server.util

import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.server.data.tables.UsersTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object DebugUtils {
    fun logUsers() {
        transaction {
            val users = UsersTable.selectAll().toList()
            Napier.i("Current users in DB: ${users.size}")
            users.forEach {
                Napier.i("User: id=${it[UsersTable.id]}, email=${it[UsersTable.email]}, role=${it[UsersTable.role]}, active=${it[UsersTable.isActive]}")
            }
        }
    }
}
