import org.gradle.kotlin.dsl.implementation
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.sigmotoa.gitdash"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sigmotoa.gitdash"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Load local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(FileInputStream(localPropertiesFile))
        }

        // AdMob IDs with fallback to test IDs
        val adMobAppId = localProperties.getProperty("adMobAppId") ?: "ca-app-pub-3940256099942544~3347511713"
        val adUnitId = localProperties.getProperty("adUnitId") ?: "ca-app-pub-3940256099942544/6300978111"
        val adUnitIntersti = localProperties.getProperty("adUnitIntersti") ?: "ca-app-pub-3940256099942544/1033173712"

        // Add to BuildConfig
        buildConfigField("String", "ADMOB_APP_ID", "\"$adMobAppId\"")
        buildConfigField("String", "AD_UNIT_ID", "\"$adUnitId\"")
        buildConfigField("String", "AD_UNIT_INTERSTITIAL", "\"$adUnitIntersti\"")

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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation ("com.google.android.gms:play-services-ads:24.7.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.logging)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}