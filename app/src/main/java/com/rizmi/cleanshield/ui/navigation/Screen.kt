package com.rizmi.cleanshield.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Security)
    data object Activity : Screen("activity", "Activity", Icons.Default.ReceiptLong)
    data object Blocklists : Screen("blocklists", "Blocklists", Icons.Default.Block)
    data object Allowlist : Screen("allowlist", "Allowlist", Icons.Default.CheckCircle)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object About : Screen("about", "About", Icons.Default.Info)

    companion object {
        val navItems: List<Screen>
            get() = listOf(
                Dashboard,
                Activity,
                Blocklists,
                Allowlist,
                Settings,
                About
            )
    }
}
