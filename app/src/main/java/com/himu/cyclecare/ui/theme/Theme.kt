package com.himu.cyclecare.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CycleColors = lightColorScheme(
    primary = Color(0xFF8A3554),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E3),
    secondary = Color(0xFF75565F),
    tertiary = Color(0xFF765A2F),
    background = Color(0xFFFFF8F8),
    surface = Color(0xFFFFF8F8),
    surfaceVariant = Color(0xFFF5DDE3),
    error = Color(0xFFBA1A1A),
)

@Composable
fun CycleCareTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CycleColors, content = content)
}
