package com.sigmotoa.gitdash.ui.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object GradientUtils {

    fun verticalGradient(
        startColor: Color,
        endColor: Color
    ): Brush {
        return Brush.verticalGradient(
            colors = listOf(startColor, endColor)
        )
    }

    fun horizontalGradient(
        startColor: Color,
        endColor: Color
    ): Brush {
        return Brush.horizontalGradient(
            colors = listOf(startColor, endColor)
        )
    }

    fun diagonalGradient(
        startColor: Color,
        endColor: Color
    ): Brush {
        return Brush.linearGradient(
            colors = listOf(startColor, endColor),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    }

    fun tripleGradient(
        startColor: Color,
        middleColor: Color,
        endColor: Color
    ): Brush {
        return Brush.verticalGradient(
            colors = listOf(startColor, middleColor, endColor)
        )
    }

    fun cardGradient(isDark: Boolean = false): Brush {
        return if (isDark) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF2D1B4E),
                    Color(0xFF1A1B2E)
                ),
                start = Offset(0f, 0f),
                end = Offset(1000f, 1000f)
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFF8F9FF),
                    Color(0xFFEEF0FF),
                    Color(0xFFE8EAFF)
                ),
                start = Offset(0f, 0f),
                end = Offset(1000f, 1000f)
            )
        }
    }

    fun primaryGradient(): Brush {
        return Brush.linearGradient(
            colors = listOf(
                Color(0xFF6B4FFF),
                Color(0xFF8B5CF6),
                Color(0xFFA78BFA)
            ),
            start = Offset(0f, 0f),
            end = Offset(1000f, 500f)
        )
    }

    fun accentGradient(): Brush {
        return Brush.linearGradient(
            colors = listOf(
                Color(0xFF0969DA),
                Color(0xFF6E40C9)
            ),
            start = Offset(0f, 0f),
            end = Offset(500f, 500f)
        )
    }
}