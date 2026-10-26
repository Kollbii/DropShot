package pl.maciek.squashscoreboard.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val LightColorScheme = lightColorScheme(
    primary = Primary40,
    onPrimary = Primary100,
    primaryContainer = Primary90,
    onPrimaryContainer = Primary10,
    inversePrimary = Primary80,
    secondary = Secondary40,
    onSecondary = Secondary100,
    secondaryContainer = Secondary90,
    onSecondaryContainer = Secondary10,
    tertiary = Tertiary40,
    onTertiary = Tertiary100,
    tertiaryContainer = Tertiary90,
    onTertiaryContainer = Tertiary10,
    background = Neutral98,
    onBackground = Neutral10,
    surface = Neutral98,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    surfaceTint = Primary40,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral95,
    error = Error40,
    onError = Error100,
    errorContainer = Error90,
    onErrorContainer = Error10,
    outline = NeutralVariant50,
    outlineVariant = NeutralVariant80,
    scrim = Neutral0,
    surfaceBright = Neutral98,
    surfaceContainer = Neutral94,
    surfaceContainerHigh = Neutral92,
    surfaceContainerHighest = Neutral90,
    surfaceContainerLow = Neutral96,
    surfaceContainerLowest = Neutral100,
    surfaceDim = Neutral87,
)

internal val DarkColorScheme = darkColorScheme(
    primary = Primary80,
    onPrimary = Primary20,
    primaryContainer = Primary30,
    onPrimaryContainer = Primary90,
    inversePrimary = Primary40,
    secondary = Secondary80,
    onSecondary = Secondary20,
    secondaryContainer = Secondary30,
    onSecondaryContainer = Secondary90,
    tertiary = Tertiary80,
    onTertiary = Tertiary20,
    tertiaryContainer = Tertiary30,
    onTertiaryContainer = Tertiary90,
    background = Neutral6,
    onBackground = Neutral90,
    surface = Neutral6,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant80,
    surfaceTint = Primary80,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    error = Error80,
    onError = Error20,
    errorContainer = Error30,
    onErrorContainer = Error90,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant30,
    scrim = Neutral0,
    surfaceBright = Neutral24,
    surfaceContainer = Neutral12,
    surfaceContainerHigh = Neutral17,
    surfaceContainerHighest = Neutral22,
    surfaceContainerLow = Neutral10,
    surfaceContainerLowest = Neutral4,
    surfaceDim = Neutral6,
)

internal val LightHighContrastColorScheme = lightColorScheme(
    primary = Primary20,
    onPrimary = Primary100,
    primaryContainer = Primary30,
    onPrimaryContainer = Primary100,
    inversePrimary = Primary90,
    secondary = Secondary20,
    onSecondary = Secondary100,
    secondaryContainer = Secondary30,
    onSecondaryContainer = Secondary100,
    tertiary = Tertiary20,
    onTertiary = Tertiary100,
    tertiaryContainer = Tertiary30,
    onTertiaryContainer = Tertiary100,
    background = Neutral98,
    onBackground = Neutral0,
    surface = Neutral98,
    onSurface = Neutral0,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant10,
    surfaceTint = Primary20,
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral100,
    error = Error20,
    onError = Error100,
    errorContainer = Error30,
    onErrorContainer = Error100,
    outline = NeutralVariant20,
    outlineVariant = NeutralVariant30,
    scrim = Neutral0,
    surfaceBright = Neutral98,
    surfaceContainer = Neutral94,
    surfaceContainerHigh = Neutral92,
    surfaceContainerHighest = Neutral90,
    surfaceContainerLow = Neutral96,
    surfaceContainerLowest = Neutral100,
    surfaceDim = Neutral87,
)

internal val DarkHighContrastColorScheme = darkColorScheme(
    primary = Primary90,
    onPrimary = Primary0,
    primaryContainer = Primary80,
    onPrimaryContainer = Primary0,
    inversePrimary = Primary30,
    secondary = Secondary90,
    onSecondary = Secondary0,
    secondaryContainer = Secondary80,
    onSecondaryContainer = Secondary0,
    tertiary = Tertiary90,
    onTertiary = Tertiary0,
    tertiaryContainer = Tertiary80,
    onTertiaryContainer = Tertiary0,
    background = Neutral6,
    onBackground = Neutral100,
    surface = Neutral6,
    onSurface = Neutral100,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = NeutralVariant95,
    surfaceTint = Primary90,
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral0,
    error = Error90,
    onError = Error0,
    errorContainer = Error80,
    onErrorContainer = Error0,
    outline = NeutralVariant90,
    outlineVariant = NeutralVariant80,
    scrim = Neutral0,
    surfaceBright = Neutral24,
    surfaceContainer = Neutral12,
    surfaceContainerHigh = Neutral17,
    surfaceContainerHighest = Neutral22,
    surfaceContainerLow = Neutral10,
    surfaceContainerLowest = Neutral4,
    surfaceDim = Neutral6,
)

/**
 * Material Design 3 corner radius tokens for DropShot.
 */
val DropShotShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * 8 dp spatial grid and minimum touch target tokens.
 */
@Immutable
data class DropShotSpacing(
    val s0: Dp = 0.dp,
    val s1: Dp = 4.dp,
    val s2: Dp = 8.dp,
    val s3: Dp = 16.dp,
    val s4: Dp = 24.dp,
    val s5: Dp = 32.dp,
    val s6: Dp = 48.dp,
    val s7: Dp = 64.dp,
    val touchTargetMin: Dp = 48.dp,
    val screenMarginCompact: Dp = 16.dp,
    val screenMarginMedium: Dp = 24.dp,
    val screenMarginExpanded: Dp = 24.dp,
)

/**
 * Material Design 3 elevation tokens.
 */
@Immutable
data class DropShotElevation(
    val level0: Dp = 0.dp,
    val level1: Dp = 1.dp,
    val level2: Dp = 3.dp,
    val level3: Dp = 6.dp,
    val level4: Dp = 8.dp,
    val level5: Dp = 12.dp,
)

/**
 * Material Design 3 motion tokens.
 * When the system setting "Remove animations" is on (reduceMotion == true),
 * all durations resolve to 0 ms.
 */
@Immutable
data class DropShotMotion(
    val reduceMotion: Boolean = false,
    val short1Ms: Int = if (reduceMotion) 0 else 50,
    val short2Ms: Int = if (reduceMotion) 0 else 100,
    val short3Ms: Int = if (reduceMotion) 0 else 150,
    val short4Ms: Int = if (reduceMotion) 0 else 200,
    val medium1Ms: Int = if (reduceMotion) 0 else 250,
    val medium2Ms: Int = if (reduceMotion) 0 else 300,
    val medium4Ms: Int = if (reduceMotion) 0 else 400,
    val long2Ms: Int = if (reduceMotion) 0 else 500,
    val extraLong2Ms: Int = if (reduceMotion) 0 else 800,
    val easingEmphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
    val easingEmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f),
    val easingEmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f),
    val easingStandard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
    val easingStandardDecelerate: Easing = CubicBezierEasing(0.0f, 0.0f, 0.0f, 1.0f),
    val easingStandardAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 1.0f, 1.0f),
    val easingLinear: Easing = LinearEasing,
)

val LocalExtendedColors = staticCompositionLocalOf { DarkExtendedColors }
val LocalSpacing = staticCompositionLocalOf { DropShotSpacing() }
val LocalElevation = staticCompositionLocalOf { DropShotElevation() }
val LocalMotion = staticCompositionLocalOf { DropShotMotion() }

/**
 * Checks whether the Android system setting "Remove animations" is enabled.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale = try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            )
        } catch (_: Exception) {
            1f
        }
        scale == 0f
    }
}

/**
 * Top-level Material Design 3 theme for DropShot.
 *
 * @param darkTheme True for the dark court theme (default true in DropShot).
 * @param highContrast True when the user enables high-contrast mode in Settings.
 * @param maxFontScale Maximum supported system font scale (default 2.0f = 200%).
 */
@Composable
fun DropShotTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    maxFontScale: Float = 2.0f,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        darkTheme && highContrast -> DarkHighContrastColorScheme
        darkTheme -> DarkColorScheme
        highContrast -> LightHighContrastColorScheme
        else -> LightColorScheme
    }

    val extendedColors = when {
        darkTheme && highContrast -> DarkHighContrastExtendedColors
        darkTheme -> DarkExtendedColors
        highContrast -> LightHighContrastExtendedColors
        else -> LightExtendedColors
    }

    val reduceMotion = rememberReduceMotion()
    val motion = remember(reduceMotion) { DropShotMotion(reduceMotion = reduceMotion) }

    val baseDensity = LocalDensity.current
    val clampedFontScale = baseDensity.fontScale.coerceIn(0.85f, maxFontScale)
    val scaledDensity = remember(baseDensity.density, clampedFontScale) {
        Density(density = baseDensity.density, fontScale = clampedFontScale)
    }

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors,
        LocalScoreboardTypography provides ScoreboardTypography(),
        LocalSpacing provides DropShotSpacing(),
        LocalElevation provides DropShotElevation(),
        LocalMotion provides motion,
        LocalDensity provides scaledDensity,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DropShotTypography,
            shapes = DropShotShapes,
            content = content,
        )
    }
}

/**
 * Convenient accessor for all DropShot design tokens inside @Composable functions.
 */
object DropShotTheme {
    val colorScheme: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    val extendedColors: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val scoreboardTypography: ScoreboardTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalScoreboardTypography.current

    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes

    val spacing: DropShotSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current

    val elevation: DropShotElevation
        @Composable
        @ReadOnlyComposable
        get() = LocalElevation.current

    val motion: DropShotMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalMotion.current
}

