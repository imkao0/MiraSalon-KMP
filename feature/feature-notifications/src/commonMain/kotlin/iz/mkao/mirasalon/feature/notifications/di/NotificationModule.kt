package iz.mkao.mirasalon.feature.notifications.di

import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import iz.mkao.mirasalon.core.domain.repository.NotificationRepository
import iz.mkao.mirasalon.feature.notifications.data.network.api.NotificationApi
import iz.mkao.mirasalon.feature.notifications.data.repository.NotificationRepositoryImpl
import iz.mkao.mirasalon.feature.notifications.presentation.circuit.NotificationPresenterFactory
import iz.mkao.mirasalon.feature.notifications.presentation.screen.NotificationUiFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module

val notificationModule = module {
    single { NotificationApi(get()) }
    single<NotificationRepository> { 
        NotificationRepositoryImpl(
            api = get(),
            realtimeGateway = get(),
            repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        ) 
    }
    single { NotificationPresenterFactory(get()) } bind Presenter.Factory::class
    single { NotificationUiFactory() } bind Ui.Factory::class
}