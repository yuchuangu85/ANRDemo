package com.codemx.anrdemo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

/** 底部导航 Tab。route 用于 NavHost，label/icon 用于 NavigationBarItem。 */
enum class AppDestination(val route: String, val label: String, val icon: ImageVector) {
    Anr("anr", "ANR", Icons.Filled.Warning),
    Perf("perf", "性能", Icons.Filled.Speed),
}
