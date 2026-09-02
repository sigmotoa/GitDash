package com.sigmotoa.gitdash.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.coil3.Coil3ImageTransformerImpl
import com.mikepenz.markdown.m3.Markdown

/**
 * Renderiza un string Markdown como texto enriquecido, multiplataforma.
 *
 * Usa `multiplatform-markdown-renderer` (variante Material 3), que hereda los
 * colores y la tipografía de `MaterialTheme`. Las imágenes se cargan con Coil 3.
 *
 * Sustituye a la implementación anterior con Markwon + `TextView`, que era
 * exclusiva de Android.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    Markdown(
        content = markdown,
        modifier = modifier,
        imageTransformer = Coil3ImageTransformerImpl,
    )
}
