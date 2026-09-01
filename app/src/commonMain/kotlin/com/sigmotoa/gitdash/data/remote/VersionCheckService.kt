package com.sigmotoa.gitdash.data.remote

import com.sigmotoa.gitdash.data.model.AppVersion
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.appendPathSegments

/**
 * Descarga el `version.json` publicado para comprobar si hay una versión nueva.
 */
class VersionCheckService(private val client: HttpClient) {

    suspend fun checkVersion(): AppVersion =
        client.get(BASE_URL) {
            url { appendPathSegments("version.json") }
        }.body()

    companion object {
        const val BASE_URL = "https://raw.githubusercontent.com/sigmotoa/gitdash/main"
    }
}
