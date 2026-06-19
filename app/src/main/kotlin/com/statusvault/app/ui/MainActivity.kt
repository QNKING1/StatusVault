package com.statusvault.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.statusvault.app.ui.navigation.BottomNavItem
import com.statusvault.app.ui.screens.ImageViewerScreen
import com.statusvault.app.ui.screens.MessagesScreen
import com.statusvault.app.ui.screens.SettingsScreen
import com.statusvault.app.ui.screens.StatusScreen
import com.statusvault.app.ui.screens.VideoPlayerScreen
import com.statusvault.app.ui.theme.StatusVaultTheme
import com.statusvault.app.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Request necessary permissions on startup
        requestInitialPermissions()

        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
            val isSystemTheme by settingsViewModel.isSystemTheme.collectAsState()

            StatusVaultTheme(
                darkTheme = isDarkMode,
                useSystemTheme = isSystemTheme
            ) {
                StatusVaultApp()
            }
        }
    }

    private fun requestInitialPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
}

@Composable
fun StatusVaultApp(
    navController: NavHostController = rememberNavController()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Check if current route is a bottom nav destination
    val bottomNavRoutes = BottomNavItem.items.map { it.route }
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    currentRoute = currentRoute ?: BottomNavItem.Status.route,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        AppNavigation(
            navController = navController,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        BottomNavItem.items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = stringResource(item.labelResId)
                    )
                },
                label = { Text(stringResource(item.labelResId)) },
                selected = selected,
                onClick = { onNavigate(item.route) }
            )
        }
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Status.route,
        modifier = modifier
    ) {
        composable(BottomNavItem.Status.route) {
            StatusScreen(
                onImageClick = { imagePath ->
                    // URL-encode the path to prevent '/' from breaking navigation routes
                    val encodedPath = Uri.encode(imagePath)
                    navController.navigate("image_viewer/$encodedPath")
                },
                onVideoClick = { videoPath ->
                    // URL-encode the path to prevent '/' from breaking navigation routes
                    val encodedPath = Uri.encode(videoPath)
                    navController.navigate("video_player/$encodedPath")
                },
                snackbarHostState = snackbarHostState
            )
        }

        composable(BottomNavItem.Messages.route) {
            MessagesScreen(
                snackbarHostState = snackbarHostState
            )
        }

        composable(BottomNavItem.Settings.route) {
            SettingsScreen(
                snackbarHostState = snackbarHostState
            )
        }

        composable(
            route = "image_viewer/{imagePath}",
            arguments = listOf(
                navArgument("imagePath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // Decode the URL-encoded path back to the actual file path
            val imagePath = Uri.decode(backStackEntry.arguments?.getString("imagePath") ?: "")
            ImageViewerScreen(
                imagePath = imagePath,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = "video_player/{videoPath}",
            arguments = listOf(
                navArgument("videoPath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // Decode the URL-encoded path back to the actual file path
            val videoPath = Uri.decode(backStackEntry.arguments?.getString("videoPath") ?: "")
            VideoPlayerScreen(
                videoPath = videoPath,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
