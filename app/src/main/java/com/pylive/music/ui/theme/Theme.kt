package com.pylive.music.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.pylive.music.R

/**
 * Palette Pylive Music.
 *
 * Violet #B80DB8, blanc #FFFFFF, jaune #F5D328. Les nuances de violet et les gris
 * chauds servent uniquement à créer des surfaces et des états lisibles. Chaque
 * association de texte respecte un contraste d'au moins 4,5:1 (WCAG AA) ; les
 * combinaisons interdites (blanc sur jaune, jaune sur blanc, violet sur violet)
 * ne sont jamais employées.
 */
object PylivePalette {
    val Violet = Color(0xFFB80DB8)
    val VioletPressed = Color(0xFF930A93)
    val VioletDeep = Color(0xFF6E0A6E)
    val VioletNight = Color(0xFF140A16)
    val White = Color(0xFFFFFFFF)
    val SurfaceTint = Color(0xFFF8F2F9)
    val Outline = Color(0xFFE6DAE8)
    val OutlineSoft = Color(0xFFF1E9F3)
    val Yellow = Color(0xFFF5D328)
    val Ink = Color(0xFF1A1018)          // texte principal : 17,9:1 sur blanc
    val InkSoft = Color(0xFF5C4E5E)      // texte secondaire : 7,7:1 sur blanc
    val InkOnViolet = Color(0xFFFFFFFF)  // 5,5:1 sur violet
    val VioletSoft = Color(0xFFF7E3F7)   // pastille d'état actif
    val Danger = Color(0xFFB3261E)
    val DangerSoft = Color(0xFFFDECEA)
}

private val PyliveColorScheme = lightColorScheme(
    primary = PylivePalette.Violet,
    onPrimary = PylivePalette.InkOnViolet,
    primaryContainer = PylivePalette.VioletSoft,
    onPrimaryContainer = PylivePalette.VioletDeep,
    inversePrimary = Color(0xFFF2C9F2),

    secondary = PylivePalette.VioletDeep,
    onSecondary = PylivePalette.White,
    secondaryContainer = PylivePalette.Yellow,
    onSecondaryContainer = PylivePalette.Ink,

    tertiary = PylivePalette.VioletDeep,
    onTertiary = PylivePalette.White,
    tertiaryContainer = PylivePalette.SurfaceTint,
    onTertiaryContainer = PylivePalette.Ink,

    background = PylivePalette.White,
    onBackground = PylivePalette.Ink,
    surface = PylivePalette.White,
    onSurface = PylivePalette.Ink,
    surfaceVariant = PylivePalette.SurfaceTint,
    onSurfaceVariant = PylivePalette.InkSoft,
    surfaceTint = PylivePalette.Violet,

    surfaceContainerLowest = PylivePalette.White,
    surfaceContainerLow = Color(0xFFFDFAFD),
    surfaceContainer = PylivePalette.SurfaceTint,
    surfaceContainerHigh = Color(0xFFF4EBF5),
    surfaceContainerHighest = Color(0xFFF1E7F2),

    inverseSurface = PylivePalette.VioletNight,
    inverseOnSurface = PylivePalette.White,

    error = PylivePalette.Danger,
    onError = PylivePalette.White,
    errorContainer = PylivePalette.DangerSoft,
    onErrorContainer = Color(0xFF6E1B16),

    outline = PylivePalette.Outline,
    outlineVariant = PylivePalette.OutlineSoft,
    scrim = Color(0xFF000000)
)

/** Roboto est embarquée : le rendu ne dépend pas de la surcouche du fabricant. */
val PyliveFontFamily = FontFamily(
    Font(R.font.roboto_regular, FontWeight.Normal),
    Font(R.font.roboto_medium, FontWeight.Medium),
    Font(R.font.roboto_bold, FontWeight.Bold)
)

/**
 * Hiérarchie typographique : aucune taille de texte utile sous 13 sp, des
 * interlignes généreux et des graisses franches pour les titres.
 */
val PyliveTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = PyliveFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.2).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = PyliveFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 28.sp, lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PyliveFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 31.sp, letterSpacing = (-0.1).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = PyliveFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 21.sp, lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PyliveFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 20.sp, lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PyliveFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 17.sp, lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PyliveFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 21.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PyliveFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PyliveFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PyliveFontFamily, fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp, lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PyliveFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PyliveFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PyliveFontFamily, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.9.sp
    )
)

val PyliveShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PyliveTheme(content: @Composable () -> Unit) {
    // Thème clair unique : les zones de lecture restent blanches, comme exigé
    // par l'identité, quelle que soit la préférence système sombre.
    MaterialTheme(
        colorScheme = PyliveColorScheme,
        typography = PyliveTypography,
        shapes = PyliveShapes
    ) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as? Activity)?.window ?: return@SideEffect
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        }
        content()
    }
}
