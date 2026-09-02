package com.sigmotoa.gitdash.ui.report

import com.sigmotoa.gitdash.data.model.UnifiedRepo
import com.sigmotoa.gitdash.data.model.UnifiedUser
import com.sigmotoa.gitdash.data.repository.LastCommitInfo
import com.sigmotoa.gitdash.data.repository.RawEventRecord
import com.sigmotoa.gitdash.ui.util.DiffReportGenerator
import com.sigmotoa.gitdash.ui.util.ProfileReportGenerator
import java.time.LocalDate

actual object ReportGenerator {

    actual fun profileReport(
        user: UnifiedUser,
        repos: List<UnifiedRepo>,
        categoryCounts: Map<String, Int>,
        dateMap: Map<String, Int>,
        topReposByPushes: List<Pair<String, Int>>,
        lastCommitInfo: LastCommitInfo?,
    ): ByteArray? =ProfileReportGenerator.generate(
        user = user,
        repos = repos,
        categoryCounts = categoryCounts,
        dateMap = dateMap,
        topReposByPushes = topReposByPushes,
        lastCommitInfo = lastCommitInfo,
    )

    actual fun diffReport(
        user: UnifiedUser,
        repos: List<UnifiedRepo>,
        rawPushEvents: List<RawEventRecord>,
        startDateIso: String,
        endDateIso: String,
        linesAdded: Int,
    ): ByteArray? =DiffReportGenerator.generate(
        user = user,
        repos = repos,
        rawPushEvents = rawPushEvents,
        startDate = LocalDate.parse(startDateIso),
        endDate = LocalDate.parse(endDateIso),
        linesAdded = linesAdded,
    )
}
