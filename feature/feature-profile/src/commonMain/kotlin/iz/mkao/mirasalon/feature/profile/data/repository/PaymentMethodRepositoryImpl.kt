package iz.mkao.mirasalon.feature.profile.data.repository

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import com.russhwolf.settings.get
import iz.mkao.mirasalon.core.domain.model.PaymentMethod
import iz.mkao.mirasalon.core.domain.model.PaymentMethodType
import iz.mkao.mirasalon.core.domain.repository.PaymentMethodRepository
import iz.mkao.mirasalon.feature.profile.data.dto.PaymentMethodDto
import iz.mkao.mirasalon.feature.profile.data.mapper.toDomain
import iz.mkao.mirasalon.feature.profile.data.mapper.toDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

class PaymentMethodRepositoryImpl(
    private val settings: Settings,
) : PaymentMethodRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val _methods = MutableStateFlow(loadMethods())
    private val methods: StateFlow<List<PaymentMethod>> = _methods.asStateFlow()

    override fun observePaymentMethods(): StateFlow<List<PaymentMethod>> = methods

    override suspend fun addPaymentMethod(type: PaymentMethodType, label: String, last4Digits: String?, expiryDate: String?) {
        val newMethod = PaymentMethod(
            id = Random.nextLong().toString(),
            type = type,
            label = label,
            last4Digits = last4Digits,
            expiryDate = expiryDate,
            isDefault = _methods.value.isEmpty(),
        )
        persist(_methods.value + newMethod)
    }

    override suspend fun removePaymentMethod(id: String) {
        persist(_methods.value.filterNot { it.id == id })
    }

    override suspend fun setDefault(id: String) {
        persist(_methods.value.map { it.copy(isDefault = it.id == id) })
    }

    private fun persist(methods: List<PaymentMethod>) {
        settings[STORAGE_KEY] = json.encodeToString(methods.map { it.toDto() })
        _methods.value = methods
    }

    private fun loadMethods(): List<PaymentMethod> {
        val raw = (settings.get<String>(STORAGE_KEY) as String?) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<PaymentMethodDto>>(raw).map { it.toDomain() }
        }.getOrDefault(emptyList())
    }

    private companion object {
        const val STORAGE_KEY = "profile_payment_methods"
    }
}
