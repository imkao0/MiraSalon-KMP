package iz.mkao.mirasalon.core.domain.repository

import iz.mkao.mirasalon.core.domain.model.PaymentMethod
import iz.mkao.mirasalon.core.domain.model.PaymentMethodType
import kotlinx.coroutines.flow.Flow

interface PaymentMethodRepository {
    fun observePaymentMethods(): Flow<List<PaymentMethod>>
    suspend fun addPaymentMethod(type: PaymentMethodType, label: String, last4Digits: String?, expiryDate: String? = null)
    suspend fun removePaymentMethod(id: String)
    suspend fun setDefault(id: String)
}
