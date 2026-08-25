package iz.mkao.mirasalon.feature.chat.di

import iz.mkao.mirasalon.feature.chat.data.repository.ChatRepositoryImpl
import iz.mkao.mirasalon.feature.chat.domain.repository.ChatRepository
import iz.mkao.mirasalon.feature.chat.data.repository.UnreadMessagesSourceImpl
import iz.mkao.mirasalon.core.domain.repository.UnreadMessagesSource
import iz.mkao.mirasalon.feature.chat.presentation.circuit.*
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import org.koin.dsl.module
import org.koin.dsl.bind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

val chatModule = module {
    single<ChatRepository> { 
        ChatRepositoryImpl(
            realtimeGateway = get(),
            chatManager = get(),
            tokenProvider = get(),
            specialistRepository = get(),
            repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        )
    }
    single<UnreadMessagesSource> { UnreadMessagesSourceImpl(get()) }
    

    single { ChatManualPresenterFactory(get(), get(), get()) } bind Presenter.Factory::class
    single { ChatManualUiFactory() } bind Ui.Factory::class
}
