package com.lhzkml.codestudio.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lhzkml.codestudio.Colors
import kotlinx.coroutines.launch

@Composable
fun SideItem(
    label: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) Colors.Primary.copy(alpha = 0.1f)
                else Color.Transparent
            )
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        label()
    }
}

@Composable
fun SideContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(Color.White)
    ) {
        // 状态栏区域 - 白色背景
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
        )
        // 侧边栏内容 - 灰色背景
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Colors.Surface)
        ) {
            content()
        }
    }
}

@Composable
fun Side(
    sideContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    sideState: SideState,
    content: @Composable () -> Unit
) {
    val sideWidth = 280.dp
    val offsetX by animateDpAsState(
        targetValue = if (sideState.isOpen) sideWidth else 0.dp,
        label = "side_offset"
    )
    val scope = rememberCoroutineScope()
    
    Box(modifier = modifier.fillMaxSize()) {
        // 侧边栏 - 固定在左边
        Box(
            modifier = Modifier
                .width(sideWidth)
                .fillMaxHeight()
        ) {
            sideContent()
        }
        
        // 主界面 - 向右滑动
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.roundToPx(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {},
                        onHorizontalDrag = { _, dragAmount ->
                            if (dragAmount > 20 && !sideState.isOpen) {
                                scope.launch { sideState.open() }
                            } else if (dragAmount < -20 && sideState.isOpen) {
                                scope.launch { sideState.close() }
                            }
                        }
                    )
                }
                .then(
                    if (sideState.isOpen) {
                        Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            scope.launch { sideState.close() }
                        }
                    } else {
                        Modifier
                    }
                )
        ) {
            content()
        }
    }
}

class SideState(initialValue: Boolean = false) {
    private var _isOpen = mutableStateOf(initialValue)
    val isOpen: Boolean get() = _isOpen.value
    
    fun open() {
        _isOpen.value = true
    }
    
    fun close() {
        _isOpen.value = false
    }
    
    suspend fun animateTo(targetValue: Boolean) {
        _isOpen.value = targetValue
    }
}

@Composable
fun rememberSideState(initialValue: Boolean = false): SideState {
    return remember { SideState(initialValue) }
}
