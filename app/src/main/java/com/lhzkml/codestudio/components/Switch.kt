package com.lhzkml.codestudio.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 自定义滑动开关组件
 * 
 * @param checked 开关状态
 * @param onCheckedChange 状态变化回调
 * @param enabled 是否启用
 * @param modifier 修饰符
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val trackColor = when {
        !enabled -> Color(0xFFE0E0E0)
        checked -> Color(0xFF4CAF50)
        else -> Color(0xFFBDBDBD)
    }
    
    val thumbColor = when {
        !enabled -> Color(0xFFBDBDBD)
        else -> Color.White
    }
    
    // 滑块位置动画
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "thumbOffset"
    )
    
    val interactionSource = remember { MutableInteractionSource() }
    
    Box(
        modifier = modifier
            .width(48.dp)
            .height(28.dp)
            .background(trackColor, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .offset(x = (20.dp * thumbOffset))
                .shadow(2.dp, CircleShape)
                .background(thumbColor, CircleShape)
        )
    }
}
