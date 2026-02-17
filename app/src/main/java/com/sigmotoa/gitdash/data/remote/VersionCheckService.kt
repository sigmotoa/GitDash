package com.sigmotoa.gitdash.data.remote

import com.sigmotoa.gitdash.data.model.AppVersion
import retrofit2.http.GET

interface VersionCheckService {

    // This should point to a JSON file hosted on GitHub, Firebase, or your own server
    // Example: https://raw.githubusercontent.com/username/repo/main/version.json
    @GET("version.json")
    suspend fun checkVersion(): AppVersion

    companion object {
        // Update this URL to point to your hosted version.json file
        const val BASE_URL = "https://raw.githubusercontent.com/sigmotoa/gitdash/main/"
    }
}
