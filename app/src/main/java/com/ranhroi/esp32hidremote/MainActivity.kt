package com.ranhroi.esp32hidremote

import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.ranhroi.esp32hidremote.ui.navigation.AppNavGraph
import com.ranhroi.esp32hidremote.ui.screens.onboarding.OnboardingViewModel
import com.ranhroi.esp32hidremote.ui.screens.server.ConnectionViewModel
import com.ranhroi.esp32hidremote.ui.screens.server.NetworkStatusViewModel
import com.ranhroi.esp32hidremote.ui.screens.server.ServersControlViewModel
import com.ranhroi.esp32hidremote.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val networkStatusViewModel: NetworkStatusViewModel by viewModels()
    private val connectionViewModel: ConnectionViewModel by viewModels()
    private val serversControlViewModel: ServersControlViewModel by viewModels()
    private val onboardingViewModel: OnboardingViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val startDestination by onboardingViewModel.startDestination
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        onboarding = onboardingViewModel,
                        networkStatus = networkStatusViewModel,
                        serversControl = serversControlViewModel,
                        connection = connectionViewModel,
                    )
                }
            }

        }
    }

    override fun onResume() {
        super.onResume()
        connectionViewModel.pingConnection()
    }

    override fun onDestroy() {
        super.onDestroy()
        connectionViewModel.disconnect()
    }
}
