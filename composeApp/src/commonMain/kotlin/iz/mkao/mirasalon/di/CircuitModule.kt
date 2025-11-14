package iz.mkao.mirasalon.di

import com.slack.circuit.foundation.Circuit
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import org.koin.dsl.module

val circuitModule = module {
    single {
        Circuit.Builder()
            .addPresenterFactories(getAll<Presenter.Factory>())
            .addUiFactories(getAll<Ui.Factory>())
            .build()
    }
}
