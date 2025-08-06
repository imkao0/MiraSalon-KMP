package iz.mkao.mirasalon.feature.profile.di

import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import iz.mkao.mirasalon.core.domain.repository.PaymentMethodRepository
import iz.mkao.mirasalon.core.network.client.SalonTokenProvider
import iz.mkao.mirasalon.feature.profile.data.remote.ProfileApi
import iz.mkao.mirasalon.feature.profile.data.repository.AddressRepositoryImpl
import iz.mkao.mirasalon.feature.profile.data.repository.AppSettingsRepositoryImpl
import iz.mkao.mirasalon.feature.profile.data.repository.NotificationPreferencesRepositoryImpl
import iz.mkao.mirasalon.feature.profile.data.repository.PaymentMethodRepositoryImpl
import iz.mkao.mirasalon.feature.profile.data.repository.ProfileRepositoryImpl
import iz.mkao.mirasalon.feature.profile.domain.repository.AddressRepository
import iz.mkao.mirasalon.feature.profile.domain.repository.AppSettingsRepository
import iz.mkao.mirasalon.feature.profile.domain.repository.NotificationPreferencesRepository
import iz.mkao.mirasalon.feature.profile.domain.repository.ProfileRepository
import iz.mkao.mirasalon.feature.profile.domain.repository.SessionController
import iz.mkao.mirasalon.feature.profile.domain.repository.UnreadMessagesSource
import iz.mkao.mirasalon.feature.profile.domain.repository.UpcomingAppointmentsSource
import iz.mkao.mirasalon.feature.profile.presentation.circuit.ProfileManualPresenterFactory
import iz.mkao.mirasalon.feature.profile.presentation.circuit.ProfileManualUiFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.dsl.bind
import org.koin.dsl.module

val profileModule = module {
    single<UnreadMessagesSource> {
        object : UnreadMessagesSource {
            override fun observeUnreadMessagesCount(): Flow<Int> = flowOf(0)
        }
    }
    single<UpcomingAppointmentsSource> {
        object : UpcomingAppointmentsSource {
            override fun observeUpcomingAppointmentsCount(): Flow<Int> = flowOf(0)
        }
    }

    single { ProfileApi(get()) }

    single<ProfileRepository> { ProfileRepositoryImpl(get(), get(), get(), get()) }
    single<AddressRepository> { AddressRepositoryImpl(get(), get()) }
    single<PaymentMethodRepository> { PaymentMethodRepositoryImpl(get()) }
    single<NotificationPreferencesRepository> { NotificationPreferencesRepositoryImpl(get()) }
    single<AppSettingsRepository> { AppSettingsRepositoryImpl(get()) }
    

    single { ProfileManualPresenterFactory(
        get(),
        get(),
        get(),
        get(),
        get(), 
        get(),
        get(),
        get())
    } bind Presenter.Factory::class
    single { ProfileManualUiFactory() } bind Ui.Factory::class

    single<SessionController> {
        val tokenProvider = get<SalonTokenProvider>()
        object : SessionController {
            override suspend fun logout() {
                tokenProvider.onAuthenticationExpired()
            }
        }
    }
}
