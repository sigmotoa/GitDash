package com.sigmotoa.gitdash.ui.utils

import androidx.compose.ui.graphics.Color

object LanguageColors {
    private val colorMap = mapOf(
        "Kotlin" to Color(0xFFA97BFF),
        "Java" to Color(0xFFB07219),
        "JavaScript" to Color(0xFFF1E05A),
        "TypeScript" to Color(0xFF2B7489),
        "Python" to Color(0xFF3572A5),
        "Swift" to Color(0xFFFFAC45),
        "Objective-C" to Color(0xFF438EFF),
        "C" to Color(0xFF555555),
        "C++" to Color(0xFFF34B7D),
        "C#" to Color(0xFF178600),
        "Go" to Color(0xFF00ADD8),
        "Rust" to Color(0xFFDEA584),
        "Ruby" to Color(0xFF701516),
        "PHP" to Color(0xFF4F5D95),
        "HTML" to Color(0xFFE34C26),
        "CSS" to Color(0xFF563D7C),
        "Shell" to Color(0xFF89E051),
        "Dart" to Color(0xFF00B4AB),
        "Scala" to Color(0xFFC22D40),
        "Perl" to Color(0xFF0298C3),
        "R" to Color(0xFF198CE7),
        "Vim script" to Color(0xFF199F4B),
        "Elixir" to Color(0xFF6E4A7E),
        "Haskell" to Color(0xFF5E5086),
        "Lua" to Color(0xFF000080),
        "Clojure" to Color(0xFFDB5855),
        "Groovy" to Color(0xFFE69F56)
    )

    fun getColor(language: String?): Color {
        return language?.let { colorMap[it] } ?: Color(0xFF858585)
    }
}