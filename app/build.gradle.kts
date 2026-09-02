import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    // Silencia el aviso "beta" de las clases expect/actual (estable en la práctica).
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // ---- Target: Android ----
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // ---- Targets: iOS (device + simulators) ----
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "GitDashKit"
            isStatic = true
        }
    }

    sourceSets {
        // ---- commonMain: shared multiplatform code ----
        commonMain.dependencies {
            // Compose Multiplatform (plugin org.jetbrains.compose). Todavía sin UI
            // compartida; se prepara el classpath para mover ui/** en C2.
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.animation)
            // Coroutines — el artefacto -core es multiplataforma
            implementation(libs.kotlinx.coroutines.core)
            // DateTime — reemplazo multiplataforma de java.time
            implementation(libs.kotlinx.datetime)
            // Serialization — ya es KMP
            implementation(libs.kotlinx.serialization.json)
            // Ktor Client — reemplazo multiplataforma de Retrofit
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            // AndroidX Lifecycle — multiplataforma desde 2.8
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            // Markdown — renderer multiplataforma (antes Markwon, Android/TextView)
            implementation(libs.markdown.renderer.m3)
            implementation(libs.markdown.renderer.coil3)
            // Coil 3 — carga de imágenes multiplataforma
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            // Navigation Compose multiplataforma
            implementation(libs.androidx.navigation.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }

        // ---- androidMain: Android-only code and dependencies ----
        androidMain {
            dependencies {
                implementation("com.google.android.gms:play-services-ads:24.7.0")
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.androidx.activity.compose)
                implementation(project.dependencies.platform(libs.androidx.compose.bom))
                implementation(libs.androidx.ui)
                implementation(libs.androidx.ui.graphics)
                implementation(libs.androidx.ui.tooling.preview)
                implementation(libs.androidx.material3)

                // Ktor engine para Android
                implementation(libs.ktor.client.okhttp)
            }
        }

        // ---- iosMain: iOS-only code and dependencies ----
        iosMain.dependencies {
            // Ktor engine para iOS (NSURLSession)
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "com.sigmotoa.gitdash"
    compileSdk = 36

    sourceSets["main"].manifest.srcFile("src/main/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/main/res")

    defaultConfig {
        applicationId = "com.sigmotoa.gitdash"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Load local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }

        // AdMob IDs with fallback to test IDs
        val adMobAppId     = localProperties.getProperty("adMobAppId")     ?: "ca-app-pub-3940256099942544~3347511713"
        val adUnitId       = localProperties.getProperty("adUnitId")       ?: "ca-app-pub-3940256099942544/6300978111"
        val adUnitIntersti = localProperties.getProperty("adUnitIntersti") ?: "ca-app-pub-3940256099942544/1033173712"
        val adMobRegard    = localProperties.getProperty("adMobRegard")    ?: "ca-app-pub-3940256099942544/5224354917"

        // Add to BuildConfig
        buildConfigField("String", "ADMOB_APP_ID",         "\"$adMobAppId\"")
        buildConfigField("String", "AD_UNIT_ID",           "\"$adUnitId\"")
        buildConfigField("String", "AD_UNIT_INTERSTITIAL", "\"$adUnitIntersti\"")
        buildConfigField("String", "AD_UNIT_REWARDED",     "\"$adMobRegard\"")

        // Add to resources for AndroidManifest
        resValue("string", "admob_app_id", adMobAppId)
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
