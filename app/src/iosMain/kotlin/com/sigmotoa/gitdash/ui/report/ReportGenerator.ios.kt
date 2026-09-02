package com.sigmotoa.gitdash.ui.report

import com.sigmotoa.gitdash.data.model.UnifiedRepo
import com.sigmotoa.gitdash.data.model.UnifiedUser
import com.sigmotoa.gitdash.data.repository.LastCommitInfo
import com.sigmotoa.gitdash.data.repository.RawEventRecord

// TODO(D2): portar el dibujado del PDF a CoreGraphics / UIGraphicsPDFRenderer.
actual object ReportGenerator {

    actual fun profileReport(
        user: UnifiedUser,
        repos: List<UnifiedRepo>,
        categoryCounts: Map<String, Int>,
        dateMap: Map<String, Int>,
        topReposByPushes: List<Pair<String, Int>>,
        lastCommitInfo: LastCommitInfo?,
    ): ByteArray? = null

    actual fun diffReport(
        user: UnifiedUser,
        repos: List<UnifiedRepo>,
        rawPushEvents: List<RawEventRecord>,
        startDateIso: String,
        endDateIso: String,
        linesAdded: Int,
    ): ByteArray? = null
}
