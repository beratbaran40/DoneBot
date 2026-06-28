package com.todoapp.mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicBoolean

val LocalNavController =
    staticCompositionLocalOf<NavHostController> {
        error("No NavController provided")
    }

/**
 * Current window size class, provided from [MainActivity.onCreate]. Defaults to a typical phone
 * (Compact width) so previews and tests that don't supply it still resolve to the phone layout.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
val LocalWindowSizeClass =
    staticCompositionLocalOf {
        WindowSizeClass.calculateFromSize(DpSize(width = 411.dp, height = 891.dp))
    }

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mainViewModel.onPushIntent(intent)
        setContent {
            CompositionLocalProvider(
                LocalWindowSizeClass provides calculateWindowSizeClass(this),
            ) {
                MainContent()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mainViewModel.onPushIntent(intent)
    }

    companion object {
        val suppressNextTransition = AtomicBoolean(false)
    }
}
