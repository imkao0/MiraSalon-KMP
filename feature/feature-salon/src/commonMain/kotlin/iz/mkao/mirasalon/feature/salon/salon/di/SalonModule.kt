package iz.mkao.mirasalon.feature.salon.salon.di

import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import iz.mkao.mirasalon.core.database.MiraDatabase
import iz.mkao.mirasalon.core.database.datasource.PromotionLocalDataSource
import iz.mkao.mirasalon.core.domain.repository.PromoRepository
import iz.mkao.mirasalon.core.domain.repository.SalonRepository
import iz.mkao.mirasalon.core.domain.repository.ServiceRepository
import iz.mkao.mirasalon.feature.salon.salon.data.network.api.KtorSalonApi
import iz.mkao.mirasalon.feature.salon.salon.data.network.api.SalonApi
import iz.mkao.mirasalon.feature.salon.salon.data.repository.SalonRepositoryImpl
import iz.mkao.mirasalon.feature.salon.salon.presentation.circuit.SalonManualPresenterFactory
import iz.mkao.mirasalon.feature.salon.salon.presentation.screen.SalonManualUiFactory
import iz.mkao.mirasalon.feature.salon.services.data.network.api.KtorServicesApi
import iz.mkao.mirasalon.feature.salon.services.data.network.api.ServicesApi
import iz.mkao.mirasalon.feature.salon.services.data.repository.PromotionRepositoryImpl
import iz.mkao.mirasalon.feature.salon.services.data.repository.ServiceRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module

val salonModule = module {
    single<SalonApi> { KtorSalonApi(get()) }
    single<SalonRepository> { 
        SalonRepositoryImpl(
            api = get(),
            realtimeGateway = get(),
            repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        ) 
    }

    single<ServicesApi> { KtorServicesApi(get()) }
    single<ServiceRepository> {
        val db: MiraDatabase = get()
        ServiceRepositoryImpl(get(), db.serviceDao(), db.serviceCategoryDao(), get(), get()) 
    }

    single<PromoRepository> {
        val db: MiraDatabase = get()
        PromotionRepositoryImpl(
            promoApi = get(),
            localDataSource = PromotionLocalDataSource(db),
            realtimeGateway = get(),
            repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        )
    }


    single {
        SalonManualPresenterFactory(
            salonRepository = get(),
            tokenProvider = get(),
            profileRepository = get(),
            addressRepository = get(),
            serviceRepository = get(),
            serviceFavouritesRepository = get(),
            specialistRepository = get(),
            promoRepository = get(),
            notificationRepository = get(),
            notificationPreferencesRepository = get(),
            unreadMessagesSource = get(),
            upcomingAppointmentsSource = get()
        )
    } bind Presenter.Factory::class
    single { SalonManualUiFactory() } bind Ui.Factory::class
}
