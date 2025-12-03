package iz.mkao.mirasalon.feature.favourites.salon.di

import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import iz.mkao.mirasalon.core.domain.repository.FavouritesRepository
import iz.mkao.mirasalon.core.domain.repository.ServiceFavouritesRepository
import iz.mkao.mirasalon.feature.favourites.domain.usecase.GetFavouritesUseCase
import iz.mkao.mirasalon.feature.favourites.salon.presentation.circuit.FavouritesManualPresenterFactory
import iz.mkao.mirasalon.feature.favourites.salon.presentation.circuit.FavouritesManualUiFactory
import org.koin.dsl.bind
import org.koin.dsl.module

val favouritesModule = module {
    // Repositories are provided by databaseModule
    single { GetFavouritesUseCase(get(), get()) }
    

    single { FavouritesManualPresenterFactory(get(), get(), get()) } bind Presenter.Factory::class
    single { FavouritesManualUiFactory() } bind Ui.Factory::class
}
