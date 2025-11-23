package iz.mkao.mirasalon.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "specialist_services",
    primaryKeys = ["specialistId", "serviceId"],
    foreignKeys = [
        ForeignKey(
            entity = SpecialistEntity::class,
            parentColumns = ["id"],
            childColumns = ["specialistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ServiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["serviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("specialistId"),
        Index("serviceId")
    ]
)
data class SpecialistServiceEntity(
    val specialistId: String,
    val serviceId: String
)
