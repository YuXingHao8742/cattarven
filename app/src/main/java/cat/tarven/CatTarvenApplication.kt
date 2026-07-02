package cat.tarven

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 自定义 Application 类 — Hilt 依赖注入入口
 */
@HiltAndroidApp
class CatTarvenApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            timber.log.Timber.plant(timber.log.Timber.DebugTree())
        }
    }
}
