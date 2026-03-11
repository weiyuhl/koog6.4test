package com.example.myapplication

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 自定义简洁配色方案
private val AppColors = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    secondary = Color(0xFF34C759),
    onSecondary = Color.White,
    tertiary = Color(0xFFFF9500),
    onTertiary = Color.White,
    error = Color(0xFFFF3B30),
    onError = Color.White,
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF333333),
    surface = Color.White,
    onSurface = Color(0xFF333333),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF999999),
    outline = Color(0xFFCCCCCC),
)

@Composable
fun KoogMaterialTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        content = content,
    )
}
