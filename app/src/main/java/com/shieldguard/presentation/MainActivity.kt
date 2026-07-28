package com.shieldguard.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shieldguard.presentation.dashboard.DashboardScreen
import com.shieldguard.presentation.scanner.AppScannerScreen
import com.shieldguard.presentation.permissions.PermissionsScreen
import com.shieldguard.presentation.alerts.NotificationAlertsScreen
import com.shieldguard.presentation.urlcheck.UrlCheckScreen
import com.shieldguard.ui.theme.ShieldGuardTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShieldGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ShieldGuardNavHost()
                }
            }
        }
    }
}

// =============================================
// NAVIGATION ROUTES
// =============================================
object Routes {
    const val DASHBOARD = "dashboard"
    const val APP_SCANNER = "app_scanner"
    const val PERMISSIONS = "permissions"
    const val NOTIFICATION_ALERTS = "notification_alerts"
    const val URL_CHECK = "url_check"
}

@Composable
fun ShieldGuardNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigateToApps = { navController.navigate(Routes.APP_SCANNER) },
                onNavigateToPermissions = { navController.navigate(Routes.PERMISSIONS) },
                onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATION_ALERTS) },
                onNavigateToUrlCheck = { navController.navigate(Routes.URL_CHECK) }
            )
        }
        composable(Routes.APP_SCANNER) {
            AppScannerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.PERMISSIONS) {
            PermissionsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.NOTIFICATION_ALERTS) {
            NotificationAlertsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.URL_CHECK) {
            UrlCheckScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
