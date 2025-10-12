package iz.mkao.mirasalon.di

import iz.mkao.mirasalon.feature.profile.di.profileModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        allowOverride(true)
        appDeclaration()
        modules(
            iz.mkao.mirasalon.core.network.di.networkModule,
            iz.mkao.mirasalon.core.common.di.commonModule,
            iz.mkao.mirasalon.core.database.di.databaseModule,
            iz.mkao.mirasalon.core.realtime.di.realtimeModule,
            iz.mkao.mirasalon.feature.products.di.productsModule,
            iz.mkao.mirasalon.feature.specialists.di.specialistsModule,
            iz.mkao.mirasalon.feature.salon.salon.di.salonModule,
            iz.mkao.mirasalon.feature.chat.di.chatModule,
            iz.mkao.mirasalon.feature.appointments.di.appointmentsModule,
            profileModule,
            iz.mkao.mirasalon.feature.auth.di.authModule,
            desktopModule
        )
    }
