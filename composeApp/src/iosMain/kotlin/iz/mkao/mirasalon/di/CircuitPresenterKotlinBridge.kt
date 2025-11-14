package iz.mkao.mirasalon.di

import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.screen.Screen
import com.slack.circuit.foundation.Circuit
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import com.slack.circuit.runtime.CircuitUiState
import androidx.compose.runtime.Composable
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import iz.mkao.mirasalon.core.common.util.FlowWrapper
import iz.mkao.mirasalon.core.common.util.wrap

/**
 * A bridge class that allows SwiftUI to consume a Circuit Presenter.
 * It uses Molecule to transform the @Composable present() function into a
 * Flow of States.
 */
class CircuitPresenterKotlinBridge<UiState : CircuitUiState>(
    private val presenter: Presenter<UiState>
) {
    private val scope = MainScope()

    val state: FlowWrapper<UiState> = scope.launchMolecule(RecompositionMode.Immediate) {
        presenter.present()
    }.wrap()

    fun clear() {
        scope.cancel()
    }
}

class CircuitBridge(private val circuit: Circuit) {
    fun createPresenter(
        screen: Screen,
        navigator: Navigator
    ): CircuitPresenterKotlinBridge<CircuitUiState>? {
        val presenter = circuit.presenter(screen, navigator) as? Presenter<CircuitUiState>
        return presenter?.let { CircuitPresenterKotlinBridge(it) }
    }
}
