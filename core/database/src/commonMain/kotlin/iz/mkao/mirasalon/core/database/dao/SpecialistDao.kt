package iz.mkao.mirasalon.core.database.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import iz.mkao.mirasalon.core.database.entity.ServiceEntity
import iz.mkao.mirasalon.core.database.entity.SpecialistEntity
import iz.mkao.mirasalon.core.database.entity.SpecialistServiceEntity
import kotlinx.coroutines.flow.Flow

data class SpecialistWithServices(
    @Embedded val specialist: SpecialistEntity,
    @Relation(
        entity = ServiceEntity::class,
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SpecialistServiceEntity::class,
            parentColumn = "specialistId",
            entityColumn = "serviceId"
        )
    )
    val services: List<ServiceEntity>
)

@Dao
interface SpecialistDao {
    @Transaction
    @Query("SELECT * FROM specialists")
    fun getAllSpecialistsWithServices(): Flow<List<SpecialistWithServices>>

    @Transaction
    @Query("SELECT * FROM specialists WHERE id = :id")
    fun observeSpecialistById(id: String): Flow<SpecialistWithServices?>

    @Query("SELECT * FROM specialists")
    fun getAllSpecialists(): Flow<List<SpecialistEntity>>

    @Query("SELECT * FROM specialists WHERE id = :id")
    suspend fun getSpecialistById(id: String): SpecialistEntity?

    @Upsert
    suspend fun upsertSpecialists(specialists: List<SpecialistEntity>)

    @Upsert
    suspend fun upsertSpecialistServices(relations: List<SpecialistServiceEntity>)

    @Query("DELETE FROM specialist_services WHERE specialistId = :specialistId")
    suspend fun deleteSpecialistServices(specialistId: String)

    @Query("DELETE FROM specialists")
    suspend fun deleteAllSpecialists()
}
