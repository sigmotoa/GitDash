package com.sigmotoa.gitdash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.android.gms.ads.MobileAds
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sigmotoa.gitdash.ads.InterstitialAdManager
import com.sigmotoa.gitdash.data.remote.RetrofitInstance
import com.sigmotoa.gitdash.data.repository.GitHubRepository
import com.sigmotoa.gitdash.data.repository.UnifiedRepository
import com.sigmotoa.gitdash.ui.components.UpdateDialog
import com.sigmotoa.gitdash.ui.screen.ProfileScreen
import com.sigmotoa.gitdash.version.VersionCheckManager
import com.sigmotoa.gitdash.version.VersionUpdateInfo
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.sigmotoa.gitdash.ui.screen.RepositoryDetailScreen
import com.sigmotoa.gitdash.ui.screen.RepositoryListScreen
import com.sigmotoa.gitdash.ui.screen.StatsScreen
import com.sigmotoa.gitdash.ui.theme.GitDashTheme
import com.sigmotoa.gitdash.ui.viewmodel.GitHubViewModel

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Profile : Screen("profile", "Profile", Icons.Filled.AccountCircle)
    data object Repos : Screen("repos", "Repositories", Icons.AutoMirrored.Filled.List)
    data object Stats : Screen("stats", "Stats", Icons.Filled.BarChart)
    data object RepoDetail : Screen("repo_detail/{repoId}", "Repository Detail", Icons.AutoMirrored.Filled.List) {
        fun createRoute(repoId: Int) = "repo_detail/$repoId"
    }
}

class MainActivity : ComponentActivity() {

    private lateinit var interstitialAdManager: InterstitialAdManager
    private lateinit var versionCheckManager: VersionCheckManager
    private val updateInfo = mutableStateOf<VersionUpdateInfo?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize AdMob
        MobileAds.initialize(this) {}

        // Initialize Interstitial Ad Manager
        interstitialAdManager = InterstitialAdManager(this)

        // Initialize Version Check Manager
        versionCheckManager = VersionCheckManager(this, RetrofitInstance.versionCheckApi)

        val repository = GitHubRepository(RetrofitInstance.api)
        val unifiedRepository = UnifiedRepository(RetrofitInstance.api, RetrofitInstance.gitlabApi)
        val viewModel = GitHubViewModel(repository, unifiedRepository)

        // Check for updates
        checkForAppUpdates()

        setContent {
            GitDashTheme {
                GitDashApp(
                    viewModel = viewModel,
                    onUserInteraction = { interstitialAdManager.registerClick() }
                )

                // Show update dialog if available
                updateInfo.value?.let { info ->
                    if (info.isUpdateAvailable) {
                        UpdateDialog(
                            updateInfo = info,
                            onDismiss = {
                                updateInfo.value = null
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkForAppUpdates() {
        lifecycleScope.launch {
            val info = versionCheckManager.checkForUpdate()
            if (info != null && info.isUpdateAvailable) {
                updateInfo.value = info
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        interstitialAdManager.destroy()
    }
}

@Composable
fun GitDashApp(
    viewModel: GitHubViewModel,
    onUserInteraction: () -> Unit = {}
) {
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
                            // Register click for ad tracking
                            onUserInteraction()

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
                ProfileScreen(
                    viewModel = viewModel,
                    onUserInteraction = onUserInteraction
                )
            }
            composable(Screen.Repos.route) {
                RepositoryListScreen(
                    viewModel = viewModel,
                    onRepositoryClick = { repoId ->
                        // Register click for ad tracking
                        onUserInteraction()
                        navController.navigate(Screen.RepoDetail.createRoute(repoId))
                    },
                    onUserInteraction = onUserInteraction
                )
            }
            composable(Screen.Stats.route) {
                StatsScreen(viewModel = viewModel)
            }
            composable(
                route = Screen.RepoDetail.route,
                arguments = listOf(navArgument("repoId") { type = NavType.IntType })
            ) { backStackEntry ->
                val repoId = backStackEntry.arguments?.getInt("repoId") ?: return@composable
                RepositoryDetailScreen(
                    repoId = repoId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.navigateUp() },
                    onUserInteraction = onUserInteraction
                )
            }
        }
    }
}