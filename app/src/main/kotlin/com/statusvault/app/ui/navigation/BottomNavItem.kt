package com.statusvault.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.statusvault.app.R

sealed class BottomNavItem(
    val route: String,
    @StringRes val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Status : BottomNavItem(
        route = "status",
        labelResId = R.string.nav_status,
        selectedIcon = Icons.Filled.Collections,
        unselectedIcon = Icons.Outlined.Collections
    )

    data object Messages : BottomNavItem(
        route = "messages",
        labelResId = R.string.nav_messages,
        selectedIcon = Icons.Filled.Message,
        unselectedIcon = Icons.Outlined.Message
    )

    data object Settings : BottomNavItem(
        route = "settings",
        labelResId = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    companion object {
        val items = listOf(Status, Messages, Settings)
    }
}
