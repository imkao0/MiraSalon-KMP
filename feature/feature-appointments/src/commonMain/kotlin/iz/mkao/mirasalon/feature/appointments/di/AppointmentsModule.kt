package iz.mkao.mirasalon.feature.appointments.di

import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import iz.mkao.mirasalon.feature.appointments.data.repository.AppointmentRepositoryImpl
import iz.mkao.mirasalon.feature.appointments.domain.repository.AppointmentRepository
import iz.mkao.mirasalon.feature.appointments.presentation.circuit.AppointmentDetailManualFactory
import org.koin.dsl.module

val appointmentsModule = module {
    single<AppointmentRepository> { AppointmentRepositoryImpl() }
    
    single { AppointmentDetailManualFactory(get()) }
    factory<Presenter.Factory> { get<AppointmentDetailManualFactory>() }
    factory<Ui.Factory> { get<AppointmentDetailManualFactory>() }
}
