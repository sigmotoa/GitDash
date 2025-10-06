package com.sigmotoa.gitdash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sigmotoa.gitdash.data.remote.RetrofitInstance
import com.sigmotoa.gitdash.data.repository.GitHubRepository
import com.sigmotoa.gitdash.ui.screen.ProfileScreen
import com.sigmotoa.gitdash.ui.screen.RepositoryListScreen
import com.sigmotoa.gitdash.ui.screen.StatsScreen
import com.sigmotoa.gitdash.ui.theme.GitDashTheme
import com.sigmotoa.gitdash.ui.viewmodel.GitHubViewModel

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Profile : Screen("profile", "Profile", Icons.Filled.AccountCircle)
    data object Repos : Screen("repos", "Repositories", Icons.AutoMirrored.Filled.List)
    data object Stats : Screen("stats", "Stats", Icons.Filled.BarChart)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = GitHubRepository(RetrofitInstance.api)
        val viewModel = GitHubViewModel(repository)

        setContent {
            GitDashTheme {
                GitDashApp(viewModel)
            }
        }
    }
}

@Composable
fun GitDashApp(viewModel: GitHubViewModel) {
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
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Profile.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Profile.route) {
                ProfileScreen(viewModel = viewModel)
            }
            composable(Screen.Repos.route) {
                RepositoryListScreen(viewModel = viewModel)
            }
            composable(Screen.Stats.route) {
                StatsScreen(viewModel = viewModel)
            }
        }
    }
}