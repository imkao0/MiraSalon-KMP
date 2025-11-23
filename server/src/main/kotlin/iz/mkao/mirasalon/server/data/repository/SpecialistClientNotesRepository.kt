package iz.mkao.mirasalon.server.data.repository

import iz.mkao.mirasalon.server.data.tables.SpecialistClientNotesTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class SpecialistClientNotesRepository {

    fun getNotes(specialistId: String, userId: String): List<SpecialistClientNoteRecord> = transaction {
        SpecialistClientNotesTable.selectAll()
            .where { (SpecialistClientNotesTable.specialistId eq specialistId) and (SpecialistClientNotesTable.userId eq userId) }
            .orderBy(SpecialistClientNotesTable.createdAt, SortOrder.DESC)
            .map { it.toRecord() }
    }

    fun getAllNotesForSpecialist(specialistId: String): List<SpecialistClientNoteRecord> = transaction {
        SpecialistClientNotesTable.selectAll()
            .where { SpecialistClientNotesTable.specialistId eq specialistId }
            .orderBy(SpecialistClientNotesTable.createdAt, SortOrder.DESC)
            .map { it.toRecord() }
    }

    fun addNote(specialistId: String, userId: String, note: String) = transaction {
        val now = System.currentTimeMillis()
        SpecialistClientNotesTable.insert {
            it[id] = UUID.randomUUID().toString()
            it[this.specialistId] = specialistId
            it[this.userId] = userId
            it[this.note] = note
            it[createdAt] = now
            it[updatedAt] = now
        }
    }

    fun updateNote(noteId: String, note: String) = transaction {
        SpecialistClientNotesTable.update({ SpecialistClientNotesTable.id eq noteId }) {
            it[this.note] = note
            it[updatedAt] = System.currentTimeMillis()
        }
    }

    fun deleteNote(noteId: String) = transaction {
        SpecialistClientNotesTable.deleteWhere { id eq noteId }
    }

    private fun ResultRow.toRecord() = SpecialistClientNoteRecord(
        id = this[SpecialistClientNotesTable.id],
        specialistId = this[SpecialistClientNotesTable.specialistId],
        userId = this[SpecialistClientNotesTable.userId],
        note = this[SpecialistClientNotesTable.note],
        createdAt = this[SpecialistClientNotesTable.createdAt],
        updatedAt = this[SpecialistClientNotesTable.updatedAt]
    )
}

data class SpecialistClientNoteRecord(
    val id: String,
    val specialistId: String,
    val userId: String,
    val note: String,
    val createdAt: Long,
    val updatedAt: Long
)
