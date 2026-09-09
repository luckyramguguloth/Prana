package dev.paarudev.prana.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PranaLightColorScheme = lightColorScheme(
    primary = SagePrimary,
    onPrimary = OnSagePrimary,
    primaryContainer = SageContainer,
    secondary = SageSecondary,
    background = WarmOffWhite,
    surface = SurfaceCard,
    surfaceVariant = SurfaceSubtle,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = MutedBrickAccent,
    errorContainer = AccentContainer,
    onError = OnAccent,
    outline = SurfaceBorder
)

@Composable
fun PranaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PranaLightColorScheme,
        typography = PranaTypography,
        shapes = PranaShapes,
        content = content
    )
}
