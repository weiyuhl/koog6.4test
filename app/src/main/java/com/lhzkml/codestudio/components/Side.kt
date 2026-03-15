package com.lhzkml.codestudio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lhzkml.codestudio.Colors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SideItem(
    label: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
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
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.weight(1f))
            trailingIcon()
        }
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .statusBarsPadding()
        )
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

enum class SideValue {
    Closed,
    Open
}

@Composable
fun Side(
    sideContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    sideState: SideState = rememberSideState(SideValue.Closed),
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val sideWidth = 280.dp
    val sideWidthPx = with(density) { sideWidth.toPx() }
    val scope = rememberCoroutineScope()
    
    var offset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    
    if (!isDragging) {
        offset = if (sideState.isOpen) sideWidthPx else 0f
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // 侧边栏 - 固定280dp宽度
        Box(
            modifier = Modifier
                .width(sideWidth)
                .fillMaxHeight()
                .offset { IntOffset((-sideWidthPx + offset).roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            scope.launch {
                                // 从关闭到打开：需要超过50%
                                // 从打开到关闭：只需向左滑动1/3（还剩2/3时就关闭）
                                if (sideState.isOpen) {
                                    if (offset > sideWidthPx * 2 / 3) {
                                        sideState.open()
                                    } else {
                                        sideState.close()
                                    }
                                } else {
                                    if (offset > sideWidthPx / 2) {
                                        sideState.open()
                                    } else {
                                        sideState.close()
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            scope.launch {
                                // 从关闭到打开：需要超过50%
                                // 从打开到关闭：只需向左滑动1/3（还剩2/3时就关闭）
                                if (sideState.isOpen) {
                                    if (offset > sideWidthPx * 2 / 3) {
                                        sideState.open()
                                    } else {
                                        sideState.close()
                                    }
                                } else {
                                    if (offset > sideWidthPx / 2) {
                                        sideState.open()
                                    } else {
                                        sideState.close()
                                    }
                                }
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offset = (offset + dragAmount).coerceIn(0f, sideWidthPx)
                        }
                    )
                }
        ) {
            Column(Modifier.fillMaxSize(), content = sideContent)
        }
        
        // 主界面 - 被侧边栏推着向右移动
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                        },
                        onDragEnd = {
                            isDragging = false
                            scope.launch {
                                // 从关闭到打开：需要超过50%
                                // 从打开到关闭：只需向左滑动1/3（还剩2/3时就关闭）
                                if (sideState.isOpen) {
                                    if (offset > sideWidthPx * 2 / 3) {
                                        sideState.open()
                                    } else {
                                        sideState.close()
                                    }
                                } else {
                                    if (offset > sideWidthPx / 2) {
                                        sideState.open()
                                    } else {
                                        sideState.close()
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            scope.launch {
                                // 从关闭到打开：需要超过50%
                                // 从打开到关闭：只需向左滑动1/3（还剩2/3时就关闭）
                                if (sideState.isOpen) {
                                    if (offset > sideWidthPx * 2 / 3) {
                                        sideState.open()
                                    } else {
                                        sideState.close()
                                    }
                                } else {
                                    if (offset > sideWidthPx / 2) {
                                        sideState.open()
                                    } else {
                                        sideState.close()
                                    }
                                }
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            offset = (offset + dragAmount).coerceIn(0f, sideWidthPx)
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

class SideState(
    initialValue: SideValue
) {
    private var _currentValue = mutableStateOf(initialValue)
    val currentValue: SideValue
        get() = _currentValue.value
    
    val isOpen: Boolean
        get() = currentValue == SideValue.Open
    
    val isClosed: Boolean
        get() = currentValue == SideValue.Closed
    
    fun open() {
        _currentValue.value = SideValue.Open
    }
    
    fun close() {
        _currentValue.value = SideValue.Closed
    }
}

@Composable
fun rememberSideState(
    initialValue: SideValue
): SideState {
    return remember {
        SideState(initialValue = initialValue)
    }
}
