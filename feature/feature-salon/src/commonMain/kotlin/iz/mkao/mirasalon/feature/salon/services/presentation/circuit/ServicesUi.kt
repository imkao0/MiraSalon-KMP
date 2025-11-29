package iz.mkao.mirasalon.feature.salon.services.presentation.circuit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.slack.circuit.codegen.annotations.CircuitInject
import iz.mkao.mirasalon.core.common.di.AppScope
import iz.mkao.mirasalon.core.navigation.ServiceRoute
import iz.mkao.mirasalon.feature.salon.services.presentation.ui.ServicesScreenContent

@CircuitInject(ServiceRoute.Services::class, AppScope::class)
@Composable
fun ServicesUi(state: ServicesState, modifier: Modifier = Modifier) {
    ServicesScreenContent(state, modifier)
}
