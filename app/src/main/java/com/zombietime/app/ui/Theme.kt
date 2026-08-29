package com.zombietime.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** 진한 파스텔 팔레트 */
object Pastel {
    val Bg = Color(0xFFFFF4EC)
    val BgAlt = Color(0xFFFBEFF6)
    val Card = Color(0xFFFFFFFF)
    val Primary = Color(0xFF8B7BE8)
    val PrimarySoft = Color(0xFFEDE7FF)
    val Pink = Color(0xFFFF8FA9)
    val PinkSoft = Color(0xFFFFE1E9)
    val Mint = Color(0xFF4CC7A8)
    val MintSoft = Color(0xFFD7F5EC)
    val Lemon = Color(0xFFFFC96B)
    val Zombie = Color(0xFF6FBF7E)
    val Ink = Color(0xFF443C5E)
    val InkSoft = Color(0xFF8B82A6)
    val InkFaint = Color(0xFFC3BCD6)
    val Line = Color(0xFFF1ECF8)
}

private val scheme = lightColorScheme(
    primary = Pastel.Primary,
    onPrimary = Color.White,
    secondary = Pastel.Pink,
    onSecondary = Color.White,
    background = Pastel.Bg,
    onBackground = Pastel.Ink,
    surface = Pastel.Card,
    onSurface = Pastel.Ink,
    surfaceVariant = Pastel.PrimarySoft,
    onSurfaceVariant = Pastel.InkSoft
)

@Composable
fun ZombieTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}

/** 진행도에 따른 대표 색 (칩, 막대 등) */
fun stageColor(progress: Float): Color = when {
    progress < 0.30f -> Color(0xFF57C2A4)
    progress < 0.50f -> Color(0xFFE8B65C)
    progress < 0.72f -> Color(0xFFE8916B)
    progress < 0.92f -> Color(0xFF8FBF6B)
    else -> Color(0xFF5E9E62)
}
