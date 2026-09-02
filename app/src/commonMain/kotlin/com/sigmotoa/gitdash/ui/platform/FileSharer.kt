package com.sigmotoa.gitdash.ui.platform

import androidx.compose.runtime.Composable

/**
 * Escribe unos bytes en un archivo temporal y abre el diálogo de compartir del
 * sistema. Implementación real en Android (FileProvider + `ACTION_SEND`);
 * en iOS (`UIActivityViewController`) queda pendiente del paso de integración.
 */
fun interface FileSharer {
    fun share(bytes: ByteArray, fileName: String, mimeType: String)
}

fun FileSharer.sharePdf(bytes: ByteArray, fileName: String) =
    share(bytes, fileName, "application/pdf")

@Composable
expect fun rememberFileSharer(): FileSharer
