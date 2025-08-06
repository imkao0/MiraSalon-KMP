package iz.mkao.mirasalon.feature.profile.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import iz.mkao.mirasalon.feature.profile.presentation.ui.PaymentMethodsScreenContent

@Composable
fun PaymentMethodsUi(state: PaymentMethodsState, modifier: Modifier = Modifier) {
    PaymentMethodsScreenContent(state, modifier)
}

