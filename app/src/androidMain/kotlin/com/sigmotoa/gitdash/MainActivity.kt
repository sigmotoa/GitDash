package com.sigmotoa.gitdash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import com.sigmotoa.gitdash.ads.InterstitialAdManager
import com.sigmotoa.gitdash.data.remote.ApiClient
import com.sigmotoa.gitdash.data.repository.GitHubRepository
import com.sigmotoa.gitdash.data.repository.UnifiedRepository
import com.sigmotoa.gitdash.platform.PlatformInfo
import com.sigmotoa.gitdash.ui.GitDashApp
import com.sigmotoa.gitdash.ui.viewmodel.GitHubViewModel
import com.sigmotoa.gitdash.version.VersionCheckManager
import com.sigmotoa.gitdash.version.VersionUpdateInfo
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var interstitialAdManager: InterstitialAdManager
    private lateinit var versionCheckManager: VersionCheckManager
    private val updateInfo = mutableStateOf<VersionUpdateInfo?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        MobileAds.initialize(this) {}
        interstitialAdManager = InterstitialAdManager(this)
        versionCheckManager = VersionCheckManager(PlatformInfo(this), ApiClient.versionCheck)

        val viewModel = GitHubViewModel(
            GitHubRepository(ApiClient.gitHub),
            UnifiedRepository(ApiClient.gitHub, ApiClient.gitLab),
        )

        checkForAppUpdates()

        setContent {
            GitDashApp(
                viewModel = viewModel,
                updateInfo = updateInfo.value,
                onDismissUpdate = { updateInfo.value = null },
                onUserInteraction = { interstitialAdManager.registerClick() },
            )
        }
    }

    private fun checkForAppUpdates() {
        lifecycleScope.launch {
            versionCheckManager.checkForUpdate()
                ?.takeIf { it.isUpdateAvailable }
                ?.let { updateInfo.value = it }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        interstitialAdManager.destroy()
    }
}
