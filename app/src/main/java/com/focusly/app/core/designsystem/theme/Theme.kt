package com.focusly.app.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FocuslyDarkColorScheme = darkColorScheme(
    primary = FocuslyColors.PrimaryAccent,
    onPrimary = FocuslyColors.PrimaryText,
    secondary = FocuslyColors.SecondaryAccent,
    onSecondary = FocuslyColors.PrimaryBackground,
    background = FocuslyColors.PrimaryBackground,
    onBackground = FocuslyColors.PrimaryText,
    surface = FocuslyColors.SecondaryBackground,
    onSurface = FocuslyColors.PrimaryText,
    surfaceVariant = FocuslyColors.CardBackground,
    onSurfaceVariant = FocuslyColors.SecondaryText,
    outline = FocuslyColors.CardBorder,
    error = FocuslyColors.Error,
    onError = FocuslyColors.PrimaryText
)

@Composable
fun FocuslyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FocuslyDarkColorScheme,
        typography = FocuslyTypography,
        shapes = FocuslyShapes,
        content = content
    )
}
