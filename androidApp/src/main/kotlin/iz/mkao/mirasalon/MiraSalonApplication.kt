package iz.mkao.mirasalon

import android.app.Application
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import iz.mkao.mirasalon.di.initKoin
import multiplatform.network.cmptoast.AppContext
import org.koin.android.ext.koin.androidContext

class MiraSalonApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Napier.base(DebugAntilog())
        }
        AppContext.set(this)
        initKoin {
            androidContext(this@MiraSalonApplication)
        }
    }
}
