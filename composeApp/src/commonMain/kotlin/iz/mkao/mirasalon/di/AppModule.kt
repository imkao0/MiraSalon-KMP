package iz.mkao.mirasalon.di

import iz.mkao.mirasalon.feature.auth.di.authModule
import iz.mkao.mirasalon.feature.appointments.di.appointmentsModule
import iz.mkao.mirasalon.feature.booking.di.bookingModule
import iz.mkao.mirasalon.feature.chat.di.chatModule
import iz.mkao.mirasalon.feature.cart.di.cartModule
import iz.mkao.mirasalon.core.common.di.commonModule
import iz.mkao.mirasalon.core.network.di.networkModule
import iz.mkao.mirasalon.feature.products.di.productsModule
import iz.mkao.mirasalon.feature.favourites.salon.di.favouritesModule
import iz.mkao.mirasalon.feature.salon.salon.di.salonModule
import iz.mkao.mirasalon.feature.specialists.di.specialistsModule
import iz.mkao.mirasalon.feature.profile.di.profileModule
import iz.mkao.mirasalon.feature.notifications.di.notificationModule
import iz.mkao.mirasalon.core.realtime.di.realtimeModule
import iz.mkao.mirasalon.core.database.di.databaseModule
import org.koin.core.module.Module
import org.koin.dsl.module

expect val platformModule: Module

val appModule = module {
    includes(
        platformModule,
        networkModule,
        commonModule,
        databaseModule,
        realtimeModule,
        productsModule,
        specialistsModule,
        salonModule,
        favouritesModule,
        cartModule,
        chatModule,
        appointmentsModule,
        bookingModule,
        profileModule,
        notificationModule,
        authModule,
        circuitModule
    )
}
