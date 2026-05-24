package com.luna.budgetapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GruvboxOrange,
    onPrimary = GruvboxBg0,
    primaryContainer = GruvboxOrange,
    onPrimaryContainer = GruvboxBg0,
    secondary = GruvboxAqua,
    onSecondary = GruvboxBg0,
    tertiary = GruvboxYellow,
    onTertiary = GruvboxBg0,
    background = GruvboxBg0,
    onBackground = GruvboxFg1,
    surface = GruvboxBg0,
    onSurface = GruvboxFg1,
    error = GruvboxRed,
    onError = GruvboxBg0
)

private val LightColorScheme = lightColorScheme(
    primary = GruvboxLightOrange,
    onPrimary = GruvboxLightBg0,
    secondary = GruvboxLightAqua,
    onSecondary = GruvboxLightBg0,
    tertiary = GruvboxLightYellow,
    onTertiary = GruvboxLightBg0,
    background = GruvboxLightBg0,
    onBackground = GruvboxLightFg0,
    surface = GruvboxLightBg0,
    onSurface = GruvboxLightFg0,
    error = GruvboxLightRed,
    onError = GruvboxLightBg0
)

@Composable
fun LazyWalletTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
