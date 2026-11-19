package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = OnEmerald,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldBright,
    secondary = ElectricBlue,
    onSecondary = OnBlue,
    secondaryContainer = BlueContainer,
    onSecondaryContainer = ElectricBlueBright,
    tertiary = CyanAccent,
    onTertiary = SlateMidnight,
    tertiaryContainer = CyanContainer,
    onTertiaryContainer = CyanAccent,
    background = SlateMidnight,
    onBackground = TextPrimary,
    surface = SlateDeep,
    onSurface = TextPrimary,
    surfaceVariant = SlateElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = SlateDeep,
    surfaceContainer = SlateCard,
    surfaceContainerHigh = SlateElevated,
    outline = SlateOutline,
    outlineVariant = SlateOutlineSoft,
    error = CoralError,
    onError = OnCoral,
    errorContainer = CoralContainer,
    onErrorContainer = OnCoral
)

private val StudioLightColorScheme = lightColorScheme(
    primary = LightEmerald,
    onPrimary = OnBlue,
    secondary = LightBlue,
    onSecondary = OnBlue,
    background = LightBg,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant
)

val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun JibonVoiceTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = StudioShapes,
        content = content
    )
}
