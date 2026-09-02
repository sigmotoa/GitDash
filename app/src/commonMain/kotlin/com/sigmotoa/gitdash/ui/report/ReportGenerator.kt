package com.sigmotoa.gitdash.ui.report

import com.sigmotoa.gitdash.data.model.UnifiedRepo
import com.sigmotoa.gitdash.data.model.UnifiedUser
import com.sigmotoa.gitdash.data.repository.LastCommitInfo
import com.sigmotoa.gitdash.data.repository.RawEventRecord

/**
 * Genera los PDF de informes.
 *
 * Android usa `android.graphics.pdf.PdfDocument`. En iOS todavía **no** está
 * implementado y devuelve `null` (paso D2 del plan de migración: falta portar el
 * dibujado a CoreGraphics / `UIGraphicsPDFRenderer`). La UI debe ocultar o
 * desactivar la exportación a PDF cuando el resultado es `null`.
 */
expect object ReportGenerator {

    fun profileReport(
        user: UnifiedUser,
        repos: List<UnifiedRepo>,
        categoryCounts: Map<String, Int>,
        dateMap: Map<String, Int>,
        topReposByPushes: List<Pair<String, Int>>,
        lastCommitInfo: LastCommitInfo?,
    ): ByteArray?

    fun diffReport(
        user: UnifiedUser,
        repos: List<UnifiedRepo>,
        rawPushEvents: List<RawEventRecord>,
        startDateIso: String,
        endDateIso: String,
        linesAdded: Int,
    ): ByteArray?
}
