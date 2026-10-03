package com.kotlin.wandr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kotlin.wandr.ui.components.map.WandrMap
import com.kotlin.wandr.ui.navigation.WandrNavHost
import com.kotlin.wandr.ui.theme.WandrTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** The only Activity. Compose draws every screen inside it; navigation swaps them. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** The map implementation chosen by Hilt (Google Maps). Screens only see the interface. */
    @Inject lateinit var wandrMap: WandrMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WandrTheme {
                WandrNavHost(map = wandrMap)
            }
        }
    }
}
