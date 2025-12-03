package iz.mkao.mirasalon.core.database.di

import iz.mkao.mirasalon.core.database.MiraDatabase
import iz.mkao.mirasalon.core.database.datasource.CartLocalDataSource
import iz.mkao.mirasalon.core.database.datasource.FavoritesLocalDataSource
import iz.mkao.mirasalon.core.database.datasource.ProductLocalDataSource
import iz.mkao.mirasalon.core.database.getDatabaseBuilder
import iz.mkao.mirasalon.core.database.getMiraDatabase
import iz.mkao.mirasalon.core.database.repository.FavouritesRepositoryImpl
import iz.mkao.mirasalon.core.database.repository.ServiceFavouritesRepositoryImpl
import iz.mkao.mirasalon.core.domain.repository.FavouritesRepository
import iz.mkao.mirasalon.core.domain.repository.ServiceFavouritesRepository
import org.koin.core.module.Module
import org.koin.dsl.module

actual val databaseModule: Module = module {
    single { getMiraDatabase(getDatabaseBuilder(null)) }
    single { get<MiraDatabase>().productFavoriteDao() }
    single { get<MiraDatabase>().serviceFavoriteDao() }
    single { ProductLocalDataSource(get()) }
    single { CartLocalDataSource(get()) }
    single { FavoritesLocalDataSource(get(), get()) }
    single<FavouritesRepository> { FavouritesRepositoryImpl(get()) }
    single<ServiceFavouritesRepository> { ServiceFavouritesRepositoryImpl(get()) }
}
