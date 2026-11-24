package com.fitforge.treinos.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val LightBackground = Color(0xFFF8FAFC)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFF1F5F9)
private val LightOnSurface = Color(0xFF0F172A)
private val LightOnSurfaceVariant = Color(0xFF475569)
private val LightOutline = Color(0xFFE2E8F0)

private val DarkBackground = Color(0xFF090D16)
private val DarkSurface = Color(0xFF111827)
private val DarkSurfaceVariant = Color(0xFF1E293B)
private val DarkOnSurface = Color(0xFFF1F5F9)
private val DarkOnSurfaceVariant = Color(0xFF94A3B8)
private val DarkOutline = Color(0xFF1F2937)

fun paletteLightScheme(palette: AppPalette): ColorScheme = when (palette) {
    AppPalette.EMERALD -> lightColorScheme(
        primary = Color(0xFF059669),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD1FAE5),
        onPrimaryContainer = Color(0xFF022C22),
        secondary = Color(0xFF0D9488),
        onSecondary = Color.White,
        background = LightBackground,
        onBackground = LightOnSurface,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        outlineVariant = LightOutline,
        error = Color(0xFFDC2626),
        onError = Color.White
    )
    AppPalette.COBALT -> lightColorScheme(
        primary = Color(0xFF2563EB),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDBEAFE),
        onPrimaryContainer = Color(0xFF0C1E3E),
        secondary = Color(0xFF0284C7),
        onSecondary = Color.White,
        background = LightBackground,
        onBackground = LightOnSurface,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        outlineVariant = LightOutline,
        error = Color(0xFFDC2626),
        onError = Color.White
    )
    AppPalette.AMBER -> lightColorScheme(
        primary = Color(0xFFD97706),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFEF3C7),
        onPrimaryContainer = Color(0xFF271702),
        secondary = Color(0xFFEA580C),
        onSecondary = Color.White,
        background = LightBackground,
        onBackground = LightOnSurface,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        outlineVariant = LightOutline,
        error = Color(0xFFDC2626),
        onError = Color.White
    )
    AppPalette.CRIMSON -> lightColorScheme(
        primary = Color(0xFFE11D48),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFE4E6),
        onPrimaryContainer = Color(0xFF310612),
        secondary = Color(0xFFDB2777),
        onSecondary = Color.White,
        background = LightBackground,
        onBackground = LightOnSurface,
        surface = LightSurface,
        onSurface = LightOnSurface,
        surfaceVariant = LightSurfaceVariant,
        onSurfaceVariant = LightOnSurfaceVariant,
        outlineVariant = LightOutline,
        error = Color(0xFFDC2626),
        onError = Color.White
    )
}

fun paletteDarkScheme(palette: AppPalette): ColorScheme = when (palette) {
    AppPalette.EMERALD -> darkColorScheme(
        primary = Color(0xFF34D399),
        onPrimary = Color(0xFF022C22),
        primaryContainer = Color(0xFF064E3B),
        onPrimaryContainer = Color(0xFFD1FAE5),
        secondary = Color(0xFF2DD4BF),
        onSecondary = Color(0xFF042F2E),
        background = DarkBackground,
        onBackground = DarkOnSurface,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        outlineVariant = DarkOutline,
        error = Color(0xFFF87171),
        onError = Color(0xFF450A0A)
    )
    AppPalette.COBALT -> darkColorScheme(
        primary = Color(0xFF60A5FA),
        onPrimary = Color(0xFF0C1E3E),
        primaryContainer = Color(0xFF1E3A8A),
        onPrimaryContainer = Color(0xFFDBEAFE),
        secondary = Color(0xFF38BDF8),
        onSecondary = Color(0xFF082F49),
        background = DarkBackground,
        onBackground = DarkOnSurface,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        outlineVariant = DarkOutline,
        error = Color(0xFFF87171),
        onError = Color(0xFF450A0A)
    )
    AppPalette.AMBER -> darkColorScheme(
        primary = Color(0xFFFBBF24),
        onPrimary = Color(0xFF271702),
        primaryContainer = Color(0xFF78350F),
        onPrimaryContainer = Color(0xFFFEF3C7),
        secondary = Color(0xFFFB923C),
        onSecondary = Color(0xFF431407),
        background = DarkBackground,
        onBackground = DarkOnSurface,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        outlineVariant = DarkOutline,
        error = Color(0xFFF87171),
        onError = Color(0xFF450A0A)
    )
    AppPalette.CRIMSON -> darkColorScheme(
        primary = Color(0xFFFB7185),
        onPrimary = Color(0xFF310612),
        primaryContainer = Color(0xFF881337),
        onPrimaryContainer = Color(0xFFFFE4E6),
        secondary = Color(0xFFF472B6),
        onSecondary = Color(0xFF500724),
        background = DarkBackground,
        onBackground = DarkOnSurface,
        surface = DarkSurface,
        onSurface = DarkOnSurface,
        surfaceVariant = DarkSurfaceVariant,
        onSurfaceVariant = DarkOnSurfaceVariant,
        outlineVariant = DarkOutline,
        error = Color(0xFFF87171),
        onError = Color(0xFF450A0A)
    )
}
