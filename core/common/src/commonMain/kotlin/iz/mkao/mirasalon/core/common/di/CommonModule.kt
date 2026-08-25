package iz.mkao.mirasalon.core.common.di

import iz.mkao.mirasalon.core.common.analytics.AnalyticsLogger
import iz.mkao.mirasalon.core.common.analytics.NapierAnalyticsLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

val commonModule = module {
    single { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    single<AnalyticsLogger> { NapierAnalyticsLogger() }
}
