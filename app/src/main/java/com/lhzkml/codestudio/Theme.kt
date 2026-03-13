package com.lhzkml.codestudio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// 自定义颜色方�?
object Colors {
    val Primary = Color(0xFF007AFF)
    val OnPrimary = Color.White
    val Secondary = Color(0xFF34C759)
    val OnSecondary = Color.White
    val Tertiary = Color(0xFFFF9500)
    val OnTertiary = Color.White
    val Error = Color(0xFFFF3B30)
    val OnError = Color.White
    val Background = Color(0xFFF5F5F5)
    val OnBackground = Color(0xFF333333)
    val Surface = Color.White
    val OnSurface = Color(0xFF333333)
    val SurfaceVariant = Color(0xFFF0F0F0)
    val OnSurfaceVariant = Color(0xFF999999)
    val Outline = Color(0xFFCCCCCC)
}

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    content()
}

