package com.ranhroi.esp32hidremote.ui.navigation


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ranhroi.esp32hidremote.ui.screens.main.MainScreen
import com.ranhroi.esp32hidremote.ui.screens.onboarding.OnboardingScreen
import com.ranhroi.esp32hidremote.ui.screens.onboarding.OnboardingViewModel
import com.ranhroi.esp32hidremote.ui.screens.quickkey.QuickKeysSetupScreen
import com.ranhroi.esp32hidremote.ui.screens.server.ConnectionViewModel
import com.ranhroi.esp32hidremote.ui.screens.server.NetworkStatusViewModel
import com.ranhroi.esp32hidremote.ui.screens.server.ServerScreen
import com.ranhroi.esp32hidremote.ui.screens.server.ServersControlViewModel
import com.ranhroi.esp32hidremote.ui.screens.setting.SettingsScreen
import com.ranhroi.esp32hidremote.ui.screens.shortkey.ShortKeySetupScreen
import com.ranhroi.esp32hidremote.ui.screens.splash.SplashScreen


@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    onboarding: OnboardingViewModel,
    networkStatus: NetworkStatusViewModel,
    serversControl: ServersControlViewModel,
    connection: ConnectionViewModel,
) {
    NavHost(
        navController, startDestination,
        route = "root_graph"
    ) {
        composable(NavRoute.Loading.route) {
            SplashScreen()
        }

        composable(NavRoute.Onboarding.route) {
            OnboardingScreen(navController, onboarding)
        }

        composable(NavRoute.Main.route) {
            MainScreen(navController, connection, networkStatus)
        }

        composable(NavRoute.SetServer.route) {
            ServerScreen(navController, connection, serversControl, networkStatus)
        }

        composable(
            route = NavRoute.SetQuickKeys.route,
            arguments = listOf(
                navArgument("quickKeyId") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val quickKeyId = backStackEntry.arguments?.getInt("quickKeyId") ?: 0
            QuickKeysSetupScreen(
                navController = navController,
                quickKeyId = quickKeyId
            )
        }

        composable(NavRoute.SetShortKey.route) {
            ShortKeySetupScreen(navController)
        }

        composable(NavRoute.Setting.route) {
            SettingsScreen(navController)
        }

    }
}


