package com.kotlin.wandr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kotlin.wandr.ui.navigation.WandrNavHost
import com.kotlin.wandr.ui.theme.WandrTheme
import dagger.hilt.android.AndroidEntryPoint

/** The only Activity. Compose draws every screen inside it; navigation swaps them. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WandrTheme {
                WandrNavHost()
            }
        }
    }
}
