package iz.mkao.mirasalon.core.realtime.di

import iz.mkao.mirasalon.core.domain.repository.StreamChatManager
import iz.mkao.mirasalon.core.realtime.RealtimeGateway
import iz.mkao.mirasalon.core.realtime.createRealtimeGateway
import iz.mkao.mirasalon.core.realtime.createStreamChatManager
import org.koin.dsl.module

val realtimeModule = module {
    single<RealtimeGateway> { createRealtimeGateway(get(), get(), get()) }
    single<StreamChatManager> { createStreamChatManager(get()) }
}
