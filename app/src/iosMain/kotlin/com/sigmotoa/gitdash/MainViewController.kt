package com.sigmotoa.gitdash

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.sigmotoa.gitdash.data.remote.ApiClient
import com.sigmotoa.gitdash.data.repository.GitHubRepository
import com.sigmotoa.gitdash.data.repository.UnifiedRepository
import com.sigmotoa.gitdash.ui.GitDashApp
import com.sigmotoa.gitdash.ui.viewmodel.GitHubViewModel
import platform.UIKit.UIViewController

/**
 * Punto de entrada de la UI para iOS. Swift lo llama desde `ContentView.swift`
 * (`ComposeView`), igual que `MainActivity` llama a `GitDashApp` en Android.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    val viewModel = remember {
        GitHubViewModel(
            GitHubRepository(ApiClient.gitHub),
            UnifiedRepository(ApiClient.gitHub, ApiClient.gitLab),
        )
    }
    GitDashApp(viewModel = viewModel)
}
