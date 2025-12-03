package iz.mkao.mirasalon

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.LocalPlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

@OptIn(ExperimentalCoilApi::class, ExperimentalNativeApi::class)
fun MainViewController() = ComposeUIViewController {
    if (Platform.isDebugBinary) {
        Napier.base(DebugAntilog())
    }
    
    val context = LocalPlatformContext.current
    
    val imageLoader = remember {
        val imageHttpClient = KoinPlatform.getKoin().get<HttpClient>(named("imageLoader"))
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(imageHttpClient))
            }
            .build()
    }
    
    setSingletonImageLoaderFactory {
        imageLoader
    }
    
    App()
}
