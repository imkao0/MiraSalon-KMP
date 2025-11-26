package iz.mkao.mirasalon.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import iz.mkao.mirasalon.core.database.entity.OrderEntity
import iz.mkao.mirasalon.core.database.entity.OrderItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Transaction
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrdersWithItems(): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getOrderById(id: String): OrderWithItems?

    @Upsert
    suspend fun upsertOrder(order: OrderEntity)

    @Upsert
    suspend fun upsertOrderItems(items: List<OrderItemEntity>)

    @Transaction
    suspend fun saveOrderWithItems(order: OrderEntity, items: List<OrderItemEntity>) {
        upsertOrder(order)
        upsertOrderItems(items)
    }

    @Query("DELETE FROM orders WHERE id = :id")
    suspend fun deleteOrderById(id: String)

    @Query("DELETE FROM orders")
    suspend fun deleteAllOrders()
}

data class OrderWithItems(
    @Embedded val order: OrderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val items: List<OrderItemEntity>
)
