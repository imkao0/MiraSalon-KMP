package iz.mkao.mirasalon.feature.auth.di

import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import iz.mkao.mirasalon.feature.auth.data.network.api.AuthApi
import iz.mkao.mirasalon.feature.auth.data.network.api.KtorAuthApi
import iz.mkao.mirasalon.feature.auth.data.repository.AuthRepository
import iz.mkao.mirasalon.feature.auth.data.repository.DefaultAuthRepository
import iz.mkao.mirasalon.feature.auth.presentation.circuit.*
import org.koin.dsl.module
import org.koin.dsl.bind

val authModule = module {
    single<AuthApi> { KtorAuthApi(get()) }
    single<AuthRepository> { DefaultAuthRepository(get(), get()) }
    

    single { AuthManualPresenterFactory(get()) } bind Presenter.Factory::class
    single { AuthManualUiFactory() } bind Ui.Factory::class
}
