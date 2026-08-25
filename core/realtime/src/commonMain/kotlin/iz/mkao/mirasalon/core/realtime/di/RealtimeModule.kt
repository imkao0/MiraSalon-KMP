package iz.mkao.mirasalon.core.realtime.di

import iz.mkao.mirasalon.core.domain.repository.ChatManager
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import iz.mkao.mirasalon.core.realtime.createRealtimeGateway
import iz.mkao.mirasalon.core.realtime.createChatManager
import org.koin.dsl.module

val realtimeModule = module {
    single<RealtimeGateway> { createRealtimeGateway(get(), get(), get()) }
    single<ChatManager> { createChatManager(get(), get()) }
}
