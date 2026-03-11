package com.example.myapplication.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.myapplication.AppColors

@Composable
fun RadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedColor: Color = AppColors.Primary,
    unselectedColor: Color = AppColors.Outline
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clickable(
                enabled = enabled,
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val strokeWidth = 2.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            
            // Outer circle
            drawCircle(
                color = if (selected) selectedColor else unselectedColor,
                radius = radius,
                style = Stroke(width = strokeWidth)
            )
            
            // Inner filled circle when selected
            if (selected) {
                drawCircle(
                    color = selectedColor,
                    radius = radius * 0.5f,
                    style = Fill
                )
            }
        }
    }
}
