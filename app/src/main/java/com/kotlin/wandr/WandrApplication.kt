package com.kotlin.wandr

import android.app.Application
import com.kotlin.wandr.core.event.CacheInvalidator
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WandrApplication : Application() {

    @Inject lateinit var cacheInvalidator: CacheInvalidator

    override fun onCreate() {
        super.onCreate()
        // Event bus subscribers that live as long as the app
        cacheInvalidator.start()
    }
}
