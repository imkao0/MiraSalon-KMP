package iz.mkao.mirasalon.core.database.datasource

import iz.mkao.mirasalon.core.database.MiraDatabase
import iz.mkao.mirasalon.core.database.entity.PromotionEntity
import iz.mkao.mirasalon.core.domain.model.DiscountType
import iz.mkao.mirasalon.core.domain.model.PromoStatus
import iz.mkao.mirasalon.core.domain.model.PromoType
import iz.mkao.mirasalon.core.domain.model.Promotion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PromotionLocalDataSource(private val database: MiraDatabase) {
    private val promoDao = database.promotionDao()

    fun observeActivePromotions(): Flow<List<Promotion>> {
        return promoDao.getActivePromotions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun savePromotions(promotions: List<Promotion>) {
        promoDao.upsertPromotions(promotions.map { it.toEntity() })
    }

    suspend fun clearAll() {
        promoDao.deleteAllPromotions()
    }
}

private fun PromotionEntity.toDomain() = Promotion(
    id = id,
    code = code,
    description = description,
    discountType = try { DiscountType.valueOf(discountType) } catch(e: Exception) { DiscountType.PERCENTAGE },
    discountValue = discountValue,
    imageUrl = imageUrl,
    status = try { PromoStatus.valueOf(status) } catch(e: Exception) { PromoStatus.ACTIVE },
    validFrom = validFrom?.let { kotlin.time.Instant.fromEpochMilliseconds(it) },
    validUntil = validUntil?.let { kotlin.time.Instant.fromEpochMilliseconds(it) },
    title = title,
    ctaText = ctaText,
    type = try { PromoType.valueOf(promoType) } catch(e: Exception) { PromoType.EXPERTS }
)

private fun Promotion.toEntity() = PromotionEntity(
    id = id ?: "",
    code = code ?: "",
    description = description,
    discountType = discountType.name,
    discountValue = discountValue,
    imageUrl = imageUrl,
    status = status.name,
    validFrom = validFrom?.toEpochMilliseconds(),
    validUntil = validUntil?.toEpochMilliseconds(),
    title = title,
    ctaText = ctaText,
    promoType = type.name
)
