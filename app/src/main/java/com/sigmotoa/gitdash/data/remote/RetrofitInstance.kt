package com.sigmotoa.gitdash.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object RetrofitInstance {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val githubRetrofit = Retrofit.Builder()
        .baseUrl(GitHubApiService.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val gitlabRetrofit = Retrofit.Builder()
        .baseUrl(GitLabApiService.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val versionCheckRetrofit = Retrofit.Builder()
        .baseUrl(VersionCheckService.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val api: GitHubApiService = githubRetrofit.create(GitHubApiService::class.java)
    val gitlabApi: GitLabApiService = gitlabRetrofit.create(GitLabApiService::class.java)
    val versionCheckApi: VersionCheckService = versionCheckRetrofit.create(VersionCheckService::class.java)
}