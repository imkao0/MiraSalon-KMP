package iz.mkao.mirasalon.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import iz.mkao.mirasalon.core.database.dao.BookingDao
import iz.mkao.mirasalon.core.database.dao.CartDao
import iz.mkao.mirasalon.core.database.dao.OrderDao
import iz.mkao.mirasalon.core.database.dao.ProductDao
import iz.mkao.mirasalon.core.database.dao.ProductFavoriteDao
import iz.mkao.mirasalon.core.database.dao.PromotionDao
import iz.mkao.mirasalon.core.database.dao.ServiceCategoryDao
import iz.mkao.mirasalon.core.database.dao.ServiceDao
import iz.mkao.mirasalon.core.database.dao.ServiceFavoriteDao
import iz.mkao.mirasalon.core.database.dao.SpecialistDao
import iz.mkao.mirasalon.core.database.entity.BookingEntity
import iz.mkao.mirasalon.core.database.entity.BookingServiceEntity
import iz.mkao.mirasalon.core.database.entity.CartEntity
import iz.mkao.mirasalon.core.database.entity.OrderEntity
import iz.mkao.mirasalon.core.database.entity.OrderItemEntity
import iz.mkao.mirasalon.core.database.entity.ProductEntity
import iz.mkao.mirasalon.core.database.entity.ProductFavoriteEntity
import iz.mkao.mirasalon.core.database.entity.PromotionEntity
import iz.mkao.mirasalon.core.database.entity.ServiceCategoryEntity
import iz.mkao.mirasalon.core.database.entity.ServiceEntity
import iz.mkao.mirasalon.core.database.entity.ServiceFavoriteEntity
import iz.mkao.mirasalon.core.database.entity.SpecialistEntity
import iz.mkao.mirasalon.core.database.entity.SpecialistServiceEntity

@Database(
    entities = [
        ProductEntity::class,
        SpecialistEntity::class,
        BookingEntity::class,
        BookingServiceEntity::class,
        CartEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        PromotionEntity::class,
        ProductFavoriteEntity::class,
        ServiceCategoryEntity::class,
        ServiceEntity::class,
        ServiceFavoriteEntity::class,
        SpecialistServiceEntity::class
    ],
    version = 14,
    exportSchema = true
)
@ConstructedBy(MiraDatabaseConstructor::class)
abstract class MiraDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun specialistDao(): SpecialistDao
    abstract fun bookingDao(): BookingDao
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao
    abstract fun serviceDao(): ServiceDao
    abstract fun promotionDao(): PromotionDao
    abstract fun productFavoriteDao(): ProductFavoriteDao
    abstract fun serviceCategoryDao(): ServiceCategoryDao
    abstract fun serviceFavoriteDao(): ServiceFavoriteDao
}

expect fun getDatabaseBuilder(ctx: Any?): RoomDatabase.Builder<MiraDatabase>
expect fun getMiraDatabase(builder: RoomDatabase.Builder<MiraDatabase>): MiraDatabase
