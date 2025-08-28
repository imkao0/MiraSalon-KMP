package iz.mkao.mirasalon.feature.specialists.di

import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import iz.mkao.mirasalon.core.database.MiraDatabase
import iz.mkao.mirasalon.core.domain.repository.SpecialistRepository
import iz.mkao.mirasalon.feature.specialists.data.network.api.KtorSpecialistsApi
import iz.mkao.mirasalon.feature.specialists.data.network.api.SpecialistsApi
import iz.mkao.mirasalon.feature.specialists.data.repository.SpecialistRepositoryImpl
import iz.mkao.mirasalon.feature.specialists.presentation.circuit.SpecialistsManualPresenterFactory
import iz.mkao.mirasalon.feature.specialists.presentation.screen.SpecialistsManualUiFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module

val specialistsModule = module {
    single<SpecialistsApi> { KtorSpecialistsApi(get()) }
    single<SpecialistRepository> {
        val db = get<MiraDatabase>()
        SpecialistRepositoryImpl(
            api = get(),
            specialistDao = db.specialistDao(),
            serviceDao = db.serviceDao(),
            realtimeGateway = get(),
            repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        )
    }
    

    single { SpecialistsManualPresenterFactory(get(), get()) } bind Presenter.Factory::class
    single { SpecialistsManualUiFactory() } bind Ui.Factory::class
}
