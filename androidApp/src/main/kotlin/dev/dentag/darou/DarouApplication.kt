package dev.dentag.darou

import android.app.Application
import dev.dentag.darou.di.initKoin
import dev.dentag.darou.ui.di.initUiKoin

class DarouApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(BuildConfig.SERVER_ORIGIN)
        initUiKoin()
    }
}
