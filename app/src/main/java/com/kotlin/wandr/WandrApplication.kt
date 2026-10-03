package com.kotlin.wandr

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.kotlin.wandr.core.analytics.QuestFunnelTracker
import com.kotlin.wandr.core.event.CacheInvalidator
import com.kotlin.wandr.core.telemetry.TelemetryCollector
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WandrApplication : Application() {

    @Inject lateinit var telemetryCollector: TelemetryCollector
    @Inject lateinit var cacheInvalidator: CacheInvalidator
    @Inject lateinit var questFunnelTracker: QuestFunnelTracker

    override fun onCreate() {
        super.onCreate()
        // Event bus subscribers that live as long as the app
        telemetryCollector.start()
        cacheInvalidator.start()
        questFunnelTracker.start()

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                telemetryCollector.onAppBackground()
            }
        })
    }
}
