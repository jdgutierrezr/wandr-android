package com.kotlin.wandr

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.kotlin.wandr.core.event.CacheInvalidator
import com.kotlin.wandr.data.repository.TelemetryRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WandrApplication : Application() {

    @Inject lateinit var cacheInvalidator: CacheInvalidator
    @Inject lateinit var telemetry: TelemetryRepository

    override fun onCreate() {
        super.onCreate()
        cacheInvalidator.start()
        telemetry.start()
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                telemetry.flushInBackground()
            }
        })
    }
}
