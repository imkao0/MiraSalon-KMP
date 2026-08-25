package iz.mkao.mirasalon.feature.products.di

import iz.mkao.mirasalon.feature.products.data.network.api.KtorProductsApi
import iz.mkao.mirasalon.feature.products.data.network.api.ProductsApi
import iz.mkao.mirasalon.feature.products.data.repository.ProductRepositoryImpl
import iz.mkao.mirasalon.core.domain.repository.ProductRepository
import iz.mkao.mirasalon.core.domain.repository.CartRepository
import iz.mkao.mirasalon.core.domain.repository.FavouritesRepository
import iz.mkao.mirasalon.core.domain.repository.PromoRepository
import iz.mkao.mirasalon.core.database.datasource.ProductLocalDataSource
import iz.mkao.mirasalon.feature.products.presentation.circuit.*
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import org.koin.dsl.module
import org.koin.dsl.bind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

val productsModule = module {
    single<ProductsApi> { KtorProductsApi(get()) }
    single { ProductLocalDataSource(get()) }
    single<ProductRepository> {
        ProductRepositoryImpl(
            api = get(),
            localDataSource = get(),
            realtimeGateway = get(),
            repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        )
    }
    

    single { ProductsManualPresenterFactory(get(), get(), get(), get()) } bind Presenter.Factory::class
    single { ProductsManualUiFactory() } bind Ui.Factory::class
}
