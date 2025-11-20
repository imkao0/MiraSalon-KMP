package iz.mkao.mirasalon.server.data.repository

import iz.mkao.mirasalon.server.data.tables.RefreshTokensTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

data class RefreshToken(
    val token: String,
    val userId: String,
    val expiresAt: Long,
    val revoked: Boolean,
    val createdAt: Long
)

class RefreshTokenRepository {

    fun insert(refreshToken: RefreshToken) = transaction {
        RefreshTokensTable.insert {
            it[token] = refreshToken.token
            it[userId] = refreshToken.userId
            it[expiresAt] = refreshToken.expiresAt
            it[revoked] = refreshToken.revoked
            it[createdAt] = refreshToken.createdAt
        }
    }

    fun findByToken(token: String): RefreshToken? = transaction {
        RefreshTokensTable.selectAll().where { RefreshTokensTable.token eq token }
            .map { it.toRefreshToken() }
            .singleOrNull()
    }

    fun revoke(token: String) = transaction {
        RefreshTokensTable.update({ RefreshTokensTable.token eq token }) {
            it[revoked] = true
        }
    }

    fun revokeAllForUser(userId: String) = transaction {
        RefreshTokensTable.update({ RefreshTokensTable.userId eq userId }) {
            it[revoked] = true
        }
    }

    fun deleteExpired() = transaction {
        val now = System.currentTimeMillis()
        RefreshTokensTable.deleteWhere { RefreshTokensTable.expiresAt less now }
    }

    private fun ResultRow.toRefreshToken() = RefreshToken(
        token = this[RefreshTokensTable.token],
        userId = this[RefreshTokensTable.userId],
        expiresAt = this[RefreshTokensTable.expiresAt],
        revoked = this[RefreshTokensTable.revoked],
        createdAt = this[RefreshTokensTable.createdAt]
    )
}
