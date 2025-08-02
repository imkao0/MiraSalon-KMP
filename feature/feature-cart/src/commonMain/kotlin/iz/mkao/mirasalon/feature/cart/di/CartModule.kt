package iz.mkao.mirasalon.feature.cart.di

import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.runtime.ui.Ui
import iz.mkao.mirasalon.core.database.MiraDatabase
import iz.mkao.mirasalon.core.domain.repository.CartRepository
import iz.mkao.mirasalon.core.domain.repository.OrderRepository
import iz.mkao.mirasalon.feature.cart.data.network.api.KtorOrdersApi
import iz.mkao.mirasalon.feature.cart.data.network.api.OrdersApi
import iz.mkao.mirasalon.feature.cart.data.repository.CartRepositoryImpl
import iz.mkao.mirasalon.feature.cart.data.repository.OrderRepositoryImpl
import iz.mkao.mirasalon.feature.cart.presentation.circuit.CartManualPresenterFactory
import iz.mkao.mirasalon.feature.cart.presentation.circuit.CartManualUiFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.bind
import org.koin.dsl.module

val cartModule = module {
    single<OrdersApi> { KtorOrdersApi(get()) }
    single<CartRepository> { CartRepositoryImpl(get(), get(), get(), get()) }
    single<OrderRepository> { 
        val db = get<MiraDatabase>()
        OrderRepositoryImpl(
            api = get(),
            orderDao = db.orderDao(),
            profileRepository = get(),
            realtimeGateway = get(),
            repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        ) 
    }
    

    single { CartManualPresenterFactory(get(), get(), get(), get()) } bind Presenter.Factory::class
    single { CartManualUiFactory() } bind Ui.Factory::class
}
