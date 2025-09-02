package iz.mkao.mirasalon.feature.booking.di

import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import iz.mkao.mirasalon.core.database.MiraDatabase
import iz.mkao.mirasalon.feature.booking.data.network.api.BookingApi
import iz.mkao.mirasalon.feature.booking.data.network.api.KtorBookingApi
import iz.mkao.mirasalon.feature.booking.data.repository.BookingRepository
import iz.mkao.mirasalon.feature.booking.data.repository.BookingRepositoryImpl
import iz.mkao.mirasalon.feature.booking.domain.usecase.BookingUseCase
import iz.mkao.mirasalon.feature.booking.presentation.circuit.BookingManualPresenterFactory
import iz.mkao.mirasalon.feature.booking.presentation.ui.BookingManualUiFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module

val bookingModule = module {
    single<BookingApi> { KtorBookingApi(get()) }
    single<BookingRepository> { 
        val db = get<MiraDatabase>()
        BookingRepositoryImpl(
            api = get(), 
            bookingDao = db.bookingDao(),
            profileRepository = get(),
            realtimeGateway = get(),
            repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        ) 
    }
    single { BookingUseCase(get()) }


    single { BookingManualPresenterFactory(get(), get(), get()) } bind Presenter.Factory::class
    single { BookingManualUiFactory() } bind Ui.Factory::class
}
