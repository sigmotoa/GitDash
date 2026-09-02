package com.sigmotoa.gitdash.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sigmotoa.gitdash.ui.components.UpdateDialog
import com.sigmotoa.gitdash.ui.screen.ProfileScreen
import com.sigmotoa.gitdash.ui.screen.RepositoryDetailScreen
import com.sigmotoa.gitdash.ui.screen.RepositoryListScreen
import com.sigmotoa.gitdash.ui.screen.StatsScreen
import com.sigmotoa.gitdash.ui.theme.GitDashTheme
import com.sigmotoa.gitdash.ui.viewmodel.GitHubViewModel
import com.sigmotoa.gitdash.version.VersionUpdateInfo

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Profile : Screen("profile", "Profile", Icons.Filled.AccountCircle)
    data object Repos : Screen("repos", "Repositories", Icons.AutoMirrored.Filled.List)
    data object Stats : Screen("stats", "Stats", Icons.Filled.BarChart)
    data object RepoDetail :
        Screen("repo_detail/{repoId}", "Repository Detail", Icons.AutoMirrored.Filled.List) {
        fun createRoute(repoId: Int) = "repo_detail/$repoId"
    }
}

/**
 * Raíz de la UI, compartida por Android e iOS. Envuelve el tema, la barra de
 * navegación inferior, el `NavHost` y el diálogo de actualización.
 */
@Composable
fun GitDashApp(
    viewModel: GitHubViewModel,
    updateInfo: VersionUpdateInfo? = null,
    onDismissUpdate: () -> Unit = {},
    onUserInteraction: () -> Unit = {},
) {
    GitDashTheme {
        val navController = rememberNavController()
        val items = listOf(Screen.Profile, Screen.Repos, Screen.Stats)

        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy
                                ?.any { it.route == screen.route } == true,
                            onClick = {
                                onUserInteraction()
                                navController.navigate(screen.route) {
                                    popUpTo(
                                        navController.graph.startDestinationRoute
                                            ?: Screen.Profile.route,
                                    ) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Profile.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        viewModel = viewModel,
                        onUserInteraction = onUserInteraction,
                    )
                }
                composable(Screen.Repos.route) {
                    RepositoryListScreen(
                        viewModel = viewModel,
                        onRepositoryClick = { repoId ->
                            onUserInteraction()
                            navController.navigate(Screen.RepoDetail.createRoute(repoId))
                        },
                        onUserInteraction = onUserInteraction,
                    )
                }
                composable(Screen.Stats.route) {
                    StatsScreen(viewModel = viewModel)
                }
                composable(
                    route = Screen.RepoDetail.route,
                    arguments = listOf(navArgument("repoId") { type = NavType.IntType }),
                ) { backStackEntry ->
                    val repoId = backStackEntry.arguments?.getInt("repoId") ?: return@composable
                    RepositoryDetailScreen(
                        repoId = repoId,
                        viewModel = viewModel,
                        onNavigateBack = { navController.navigateUp() },
                        onUserInteraction = onUserInteraction,
                    )
                }
            }
        }

        updateInfo?.let { info ->
            if (info.isUpdateAvailable) {
                UpdateDialog(updateInfo = info, onDismiss = onDismissUpdate)
            }
        }
    }
}
