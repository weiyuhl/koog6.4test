package com.lhzkml.codestudio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lhzkml.codestudio.Colors

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    color: Color = Colors.Surface,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(shadowElevation, RoundedCornerShape(0.dp))
            .background(color)
    ) {
        content()
    }
}

