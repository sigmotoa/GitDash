package com.sigmotoa.gitdash.ui.report

import com.sigmotoa.gitdash.data.model.Platform
import com.sigmotoa.gitdash.data.model.UnifiedRepo
import com.sigmotoa.gitdash.data.model.UnifiedUser
import com.sigmotoa.gitdash.data.repository.LastCommitInfo
import com.sigmotoa.gitdash.data.repository.RawEventRecord
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifica que el layout compartido + el renderer de cada plataforma producen un
 * PDF real. Corre en JVM (`PdfDocument`) y en Kotlin/Native (`UIGraphicsPDFRenderer`).
 * `avatarUrl` vacío -> sin descarga de red.
 */
class ReportGeneratorTest {

    private val user = UnifiedUser(
        id = 1, username = "octocat", name = "The Octocat", avatarUrl = "",
        bio = "hi", location = "SF", company = "GitHub", blog = null,
        publicRepos = 8, followers = 100, following = 10, platform = Platform.GITHUB,
    )

    private fun repo(name: String, stars: Int, lang: String?) = UnifiedRepo(
        id = name.hashCode(), name = name, fullName = "octocat/$name", description = null,
        htmlUrl = "https://x/$name", starCount = stars, forksCount = 0, language = lang,
        createdAt = null, updatedAt = null,
        owner = UnifiedRepo.Owner("octocat", "a"), platform = Platform.GITHUB,
    )

    private fun assertIsPdf(bytes: ByteArray) {
        assertTrue(bytes.size > 400, "PDF too small: ${bytes.size} bytes")
        assertEquals("%PDF", bytes.decodeToString(0, 4))
    }

    @Test
    fun profileReport_produces_a_pdf() = runTest {
        val bytes = ReportGenerator.profileReport(
            user = user,
            repos = listOf(repo("kotlin", 500, "Kotlin"), repo("swift", 200, "Swift"), repo("go", 10, null)),
            categoryCounts = mapOf("Commits" to 42, "PRs" to 3, "Issues" to 1),
            dateMap = mapOf("2026-08-01" to 5, "2026-08-15" to 3),
            topReposByPushes = listOf("kotlin" to 20, "swift" to 10),
            lastCommitInfo = LastCommitInfo("kotlin", "octocat/kotlin", "2026-08-30"),
        )
        assertIsPdf(bytes)
    }

    @Test
    fun profileReport_handles_empty_data() = runTest {
        val bytes = ReportGenerator.profileReport(
            user = user.copy(name = null, location = null, company = null),
            repos = emptyList(),
            categoryCounts = emptyMap(),
            dateMap = emptyMap(),
            topReposByPushes = emptyList(),
            lastCommitInfo = null,
        )
        assertIsPdf(bytes)
    }

    @Test
    fun diffReport_produces_a_pdf() = runTest {
        val bytes = ReportGenerator.diffReport(
            user = user,
            repos = listOf(repo("kotlin", 500, "Kotlin")),
            rawPushEvents = listOf(
                RawEventRecord("2026-08-10", "kotlin", "octocat/kotlin", 4),
                RawEventRecord("2026-08-12", "kotlin", "octocat/kotlin", 2),
            ),
            startDateIso = "2026-08-01",
            endDateIso = "2026-08-31",
            linesAdded = 137,
        )
        assertIsPdf(bytes)
    }
}
