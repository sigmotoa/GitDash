package com.sigmotoa.gitdash.data.remote

/**
 * Punto de acceso único a las APIs remotas. Comparte un solo [io.ktor.client.HttpClient]
 * (y por tanto un pool de conexiones) entre GitHub, GitLab y la comprobación de versión.
 *
 * Sustituye al antiguo `RetrofitInstance`.
 */
object ApiClient {

    private val client = createHttpClient()

    val gitHub: GitHubApiService = GitHubApiService(client)
    val gitLab: GitLabApiService = GitLabApiService(client)
    val versionCheck: VersionCheckService = VersionCheckService(client)
}
