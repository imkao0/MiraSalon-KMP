package iz.mkao.mirasalon.feature.profile.presentation.circuit

import androidx.compose.runtime.*
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import iz.mkao.mirasalon.core.domain.repository.PaymentMethodRepository
import iz.mkao.mirasalon.feature.profile.presentation.circuit.PaymentMethodsEvent
import iz.mkao.mirasalon.feature.profile.presentation.circuit.PaymentMethodsState
import kotlinx.coroutines.launch

class PaymentMethodsPresenter(
    private val repository: PaymentMethodRepository,
    private val navigator: Navigator
) : Presenter<PaymentMethodsState> {

    @Composable
    override fun present(): PaymentMethodsState {
        val methods by repository.observePaymentMethods().collectAsState(emptyList())
        val scope = rememberCoroutineScope()

        return PaymentMethodsState(
            methods = methods,
            eventSink = { event ->
                when (event) {
                    is PaymentMethodsEvent.RemoveMethod -> scope.launch { repository.removePaymentMethod(event.id) }
                    is PaymentMethodsEvent.AddMethod -> {} // navigator.goTo(...)
                    is PaymentMethodsEvent.SetDefault -> scope.launch { repository.setDefault(event.id) }
                    PaymentMethodsEvent.Back -> navigator.pop()
                }
            }
        )
    }
}
