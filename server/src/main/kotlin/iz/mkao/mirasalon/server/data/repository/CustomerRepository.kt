package iz.mkao.mirasalon.server.data.repository

import iz.mkao.mirasalon.core.domain.model.CustomerDetail
import iz.mkao.mirasalon.core.domain.model.CustomerSummary
import iz.mkao.mirasalon.core.domain.outcome.Failure
import iz.mkao.mirasalon.core.domain.outcome.Outcome
import iz.mkao.mirasalon.core.network.model.PagedResponse
import iz.mkao.mirasalon.core.network.model.dto.*
import iz.mkao.mirasalon.server.data.tables.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import iz.mkao.mirasalon.core.domain.repository.CustomerRepository as CoreCustomerRepository

class CustomerRepository(private val userRepository: UserRepository) : CoreCustomerRepository {

    override suspend fun getAll(query: String?): Outcome<List<CustomerSummary>> = Outcome.Success(transaction {
        val baseQuery = UsersTable.selectAll().where { (UsersTable.role eq "USER") and (UsersTable.isDeleted eq false) }
        
        if (query != null) {
            baseQuery.andWhere { 
                (UsersTable.name.lowerCase() like "%${query.lowercase()}%") or 
                (UsersTable.email.lowerCase() like "%${query.lowercase()}%") 
            }
        }
        
        baseQuery.map { it.toCustomerSummary() }
    })

    override suspend fun getDetail(id: String): Outcome<CustomerDetail> = transaction {
        val user = UsersTable.selectAll().where { (UsersTable.id eq id) and (UsersTable.isDeleted eq false) }
            .singleOrNull() ?: return@transaction Outcome.Error(Failure.ServerError(404, "Customer not found"))
        
        Outcome.Success(user.toCustomerDetail())
    }

    override suspend fun create(name: String, email: String): Outcome<Unit> = transaction {
        try {
            val id = UUID.randomUUID().toString()
            UsersTable.insert {
                it[UsersTable.id] = id
                it[UsersTable.name] = name
                it[UsersTable.email] = email
                it[UsersTable.passwordHash] = ""
                it[UsersTable.role] = "USER"
                it[UsersTable.createdAt] = System.currentTimeMillis()
            }
            Outcome.Success(Unit)
        } catch (e: Exception) {
            Outcome.Error(Failure.ServerError(500, e.message ?: "Failed to create customer"))
        }
    }

    override suspend fun update(id: String, name: String, email: String, avatarUrl: String?): Outcome<Unit> = transaction {
        try {
            val updatedRows = UsersTable.update({ UsersTable.id eq id }) {
                it[UsersTable.name] = name
                it[UsersTable.email] = email
                avatarUrl?.let { url -> it[UsersTable.avatarUrl] = url }
            }
            if (updatedRows > 0) Outcome.Success(Unit)
            else Outcome.Error(Failure.ServerError(404, "Customer not found"))
        } catch (e: Exception) {
            Outcome.Error(Failure.ServerError(500, e.message ?: "Failed to update customer"))
        }
    }

    override suspend fun delete(id: String): Outcome<Unit> = transaction {
        try {
            val updatedRows = UsersTable.update({ UsersTable.id eq id }) {
                it[UsersTable.isDeleted] = true
                it[UsersTable.isActive] = false
            }
            if (updatedRows > 0) Outcome.Success(Unit)
            else Outcome.Error(Failure.ServerError(404, "Customer not found"))
        } catch (e: Exception) {
            Outcome.Error(Failure.ServerError(500, e.message ?: "Failed to delete customer"))
        }
    }

    private fun ResultRow.toCustomerSummary(): CustomerSummary {
        val userId = this[UsersTable.id]
        val appointmentCount = AppointmentsTable.selectAll().where { AppointmentsTable.userId eq userId }.count()
        val totalSpent = OrdersTable.selectAll().where { OrdersTable.userId eq userId }.sumOf { it[OrdersTable.totalAmount] }
        val lastVisit = AppointmentsTable.select(AppointmentsTable.dateTime)
            .where { (AppointmentsTable.userId eq userId) and (AppointmentsTable.status eq "COMPLETED") }
            .orderBy(AppointmentsTable.dateTime, SortOrder.DESC)
            .limit(1)
            .map { it[AppointmentsTable.dateTime] }
            .firstOrNull()

        return CustomerSummary(
            id = userId,
            name = this[UsersTable.name],
            email = this[UsersTable.email],
            phone = this[UsersTable.phone] ?: "",
            imageUrl = this[UsersTable.avatarUrl],
            totalBookings = appointmentCount.toInt(),
            totalSpent = totalSpent,
            lastVisit = lastVisit
        )
    }

    private fun ResultRow.toCustomerDetail(): CustomerDetail {
        val userId = this[UsersTable.id]
        val appointmentCount = AppointmentsTable.selectAll().where { AppointmentsTable.userId eq userId }.count()
        val totalSpent = OrdersTable.selectAll().where { OrdersTable.userId eq userId }.sumOf { it[OrdersTable.totalAmount] }

        return CustomerDetail(
            id = userId,
            name = this[UsersTable.name],
            email = this[UsersTable.email],
            phone = this[UsersTable.phone] ?: "",
            imageUrl = this[UsersTable.avatarUrl],
            createdAt = this[UsersTable.createdAt],
            totalBookings = appointmentCount.toInt(),
            totalSpent = totalSpent
        )
    }

    // --- Legacy methods for Ktor Routes ---

    fun findAllSummaries(page: Int, pageSize: Int, query: String? = null): PagedResponse<CustomerSummaryDto> = transaction {
        val baseQuery = UsersTable.selectAll().where { (UsersTable.role eq "USER") and (UsersTable.isDeleted eq false) }
        
        if (query != null) {
            baseQuery.andWhere { 
                (UsersTable.name.lowerCase() like "%${query.lowercase()}%") or 
                (UsersTable.email.lowerCase() like "%${query.lowercase()}%") 
            }
        }
        
        val total = baseQuery.count()
        val items = baseQuery.limit(pageSize).offset(((page - 1) * pageSize).toLong())
            .map { it.toSummaryDto() }
        
        val totalPages = if (pageSize > 0) ((total + pageSize - 1) / pageSize).toInt() else 0
        PagedResponse(items, total, page, pageSize, totalPages)
    }

    fun getCustomerDetailLegacy(id: String): CustomerDetailDto? = transaction {
        val user = UsersTable.selectAll().where { (UsersTable.id eq id) and (UsersTable.isDeleted eq false) }
            .singleOrNull() ?: return@transaction null
        
        val recentAppointments = AppointmentsTable.selectAll()
            .where { AppointmentsTable.userId eq id }
            .orderBy(AppointmentsTable.dateTime, SortOrder.DESC)
            .limit(5)
            .map {
                SimpleAppointmentDto(
                    id = it[AppointmentsTable.id],
                    status = it[AppointmentsTable.status],
                    dateTime = it[AppointmentsTable.dateTime],
                    amount = it[AppointmentsTable.totalAmount]
                )
            }

        val recentOrders = OrdersTable.selectAll()
            .where { OrdersTable.userId eq id }
            .orderBy(OrdersTable.createdAt, SortOrder.DESC)
            .limit(5)
            .map {
                SimpleOrderDto(
                    id = it[OrdersTable.id],
                    status = it[OrdersTable.status],
                    amount = it[OrdersTable.totalAmount],
                    date = it[OrdersTable.createdAt]
                )
            }

        CustomerDetailDto(
            id = user[UsersTable.id],
            name = user[UsersTable.name],
            email = user[UsersTable.email],
            phone = user[UsersTable.phone],
            avatarUrl = user[UsersTable.avatarUrl],
            bio = null,
            dateOfBirth = user[UsersTable.dateOfBirth],
            joinedAt = user[UsersTable.createdAt],
            referralCode = user[UsersTable.referralCode],
            recentAppointments = recentAppointments,
            recentOrders = recentOrders,
            reviews = emptyList()
        )
    }

    fun createCustomer(name: String, email: String): String = transaction {
        val id = UUID.randomUUID().toString()
        UsersTable.insert {
            it[UsersTable.id] = id
            it[UsersTable.name] = name
            it[UsersTable.email] = email
            it[UsersTable.passwordHash] = ""
            it[UsersTable.role] = "USER"
            it[UsersTable.createdAt] = System.currentTimeMillis()
        }
        id
    }

    fun updateCustomer(id: String, request: UpdateCustomerRequestDto): Boolean = transaction {
        val updatedRows = UsersTable.update({ UsersTable.id eq id }) {
            request.name?.let { name -> it[UsersTable.name] = name }
            request.email?.let { email -> it[UsersTable.email] = email }
            request.phone?.let { phone -> it[UsersTable.phone] = phone }
            request.avatarUrl?.let { avatarUrl -> it[UsersTable.avatarUrl] = avatarUrl }
            request.dateOfBirth?.let { dateOfBirth -> it[UsersTable.dateOfBirth] = dateOfBirth }
        }
        updatedRows > 0
    }

    fun softDelete(id: String): Boolean = transaction {
        val updatedRows = UsersTable.update({ UsersTable.id eq id }) {
            it[UsersTable.isDeleted] = true
            it[UsersTable.isActive] = false
        }
        updatedRows > 0
    }

    private fun ResultRow.toSummaryDto(): CustomerSummaryDto {
        val userId = this[UsersTable.id]
        val appointmentCount = AppointmentsTable.selectAll().where { AppointmentsTable.userId eq userId }.count()
        val totalSpend = OrdersTable.selectAll().where { OrdersTable.userId eq userId }.sumOf { it[OrdersTable.totalAmount] }
        
        return CustomerSummaryDto(
            id = userId,
            name = this[UsersTable.name],
            email = this[UsersTable.email],
            phone = this[UsersTable.phone],
            avatarUrl = this[UsersTable.avatarUrl],
            totalAppointments = appointmentCount.toInt(),
            totalSpend = totalSpend,
            referralCode = this[UsersTable.referralCode],
            createdAt = this[UsersTable.createdAt]
        )
    }
}
