package com.sigmotoa.gitdash.data

import com.sigmotoa.gitdash.data.model.Platform
import com.sigmotoa.gitdash.data.model.UnifiedUser
import com.sigmotoa.gitdash.data.remote.CommitResponse
import com.sigmotoa.gitdash.data.remote.GitHubApiService
import com.sigmotoa.gitdash.data.repository.GitHubRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifica la capa de red compartida (Ktor + kotlinx.serialization) con un
 * `MockEngine`. Corre igual en `testDebugUnitTest` (Android/JVM) y en
 * `iosSimulatorArm64Test` (Kotlin/Native).
 */
class GitHubApiServiceTest {

    private fun service(
        status: HttpStatusCode = HttpStatusCode.OK,
        headers: io.ktor.http.Headers = headersOf(HttpHeaders.ContentType, "application/json"),
        body: String,
    ): GitHubApiService {
        val client = HttpClient(MockEngine { respond(ByteReadChannel(body), status, headers) }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; coerceInputValues = true }) }
        }
        return GitHubApiService(client)
    }

    @Test
    fun getUser_deserializes_json() = runTest {
        val api = service(
            body = """
                { "login": "octocat", "id": 583231, "avatar_url": "https://x/a.png",
                  "name": "The Octocat", "public_repos": 8, "followers": 100 }
            """.trimIndent(),
        )

        val user = api.getUser("octocat")

        assertEquals("octocat", user.login)
        assertEquals(583231, user.id)
        assertEquals("The Octocat", user.name)
        assertEquals(8, user.publicRepos)
        assertEquals(100, user.followers)
    }

    @Test
    fun getUser_maps_to_UnifiedUser() = runTest {
        val api = service(
            body = """{ "login": "octocat", "id": 1, "avatar_url": "a", "bio": "hi" }""",
        )

        val unified = UnifiedUser.fromGitHub(api.getUser("octocat"))

        assertEquals("octocat", unified.username)
        assertEquals("hi", unified.bio)
        assertEquals(Platform.GITHUB, unified.platform)
    }

    @Test
    fun getRepoCommits_exposes_link_header() = runTest {
        val api = service(
            headers = headersOf(
                HttpHeaders.ContentType to listOf("application/json"),
                "Link" to listOf("""<https://api.github.com/repositories/1/commits?per_page=1&page=42>; rel="last""""),
            ),
            body = """[{ "sha": "abc123" }]""",
        )

        val response = api.getRepoCommits("octocat", "hello", perPage = 1)

        assertTrue(response.headers["Link"]!!.contains("""rel="last""""))
        assertEquals("abc123", response.body<List<CommitResponse>>().single().sha)
    }

    @Test
    fun repository_getCommitCount_reads_last_page_from_link_header() = runTest {
        val api = service(
            headers = headersOf(
                HttpHeaders.ContentType to listOf("application/json"),
                "Link" to listOf("""<https://api.github.com/repositories/1/commits?page=137>; rel="last""""),
            ),
            body = """[{ "sha": "abc" }]""",
        )

        val count = GitHubRepository(api).getCommitCount("octocat", "hello")

        assertEquals(137, count.getOrNull())
    }
}
