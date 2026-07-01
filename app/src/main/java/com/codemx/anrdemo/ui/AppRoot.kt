package com.codemx.anrdemo.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.codemx.anrdemo.anr.diagnostics.DiagnosticsRepository
import com.codemx.anrdemo.anr.dispatch.AnrScenarioDispatcher
import com.codemx.anrdemo.perf.load.PerfLoadController
import com.codemx.anrdemo.ui.navigation.AppDestination
import com.codemx.anrdemo.ui.perf.PerfScreen

/**
 * 顶层容器：Scaffold + 底部导航（微信式 tab）+ NavHost。
 * 替代原先直接渲染 AnrDemoScreen 的入口。
 */
@Composable
fun AppRoot(
    dispatcher: AnrScenarioDispatcher,
    diagnosticsRepository: DiagnosticsRepository,
    perfLoadController: PerfLoadController,
) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { AppBottomBar(navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Anr.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppDestination.Anr.route) {
                AnrDemoScreen(dispatcher = dispatcher, diagnosticsRepository = diagnosticsRepository)
            }
            composable(AppDestination.Perf.route) {
                PerfScreen(perfLoadController = perfLoadController)
            }
        }
    }
}

@Composable
private fun AppBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    NavigationBar {
        AppDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    if (currentRoute != destination.route) {
                        navController.navigate(destination.route) {
                            // 标准底部导航：保留/恢复各 Tab 状态，避免重复入栈
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
            )
        }
    }
}
