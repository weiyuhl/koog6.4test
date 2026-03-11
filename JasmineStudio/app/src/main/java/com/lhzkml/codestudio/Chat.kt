package com.lhzkml.codestudio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lhzkml.codestudio.components.Bar
import com.lhzkml.codestudio.components.Icon
import com.lhzkml.codestudio.components.IconButton
import com.lhzkml.codestudio.components.Text

@Composable
internal fun ChatScreen(
    provider: Provider,
    prompt: String,
    isRunning: Boolean,
    messages: List<ChatMessage>,
    onPromptChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onMenuClick: () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, isRunning) { 
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) 
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Bar(
            title = "Chat",
            navigationIcon = {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "菜单",
                        tint = Color(0xFF333333),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (messages.isEmpty()) {
                EmptyStateView()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(message = message)
                    }
                }
            }
        }
        
        ChatComposer(
            value = prompt,
            enabled = !isRunning,
            onValueChange = onPromptChanged,
            onSendClick = onSendClick,
            isRunning = isRunning
        )
    }
}

@Composable
private fun EmptyStateView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "💬",
                fontSize = 64.sp
            )
            Text(
                "开始对话",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333)
            )
            Text(
                "在下方输入消息",
                fontSize = 14.sp,
                color = Color(0xFF999999)
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val roleLabel = when (message.role) {
        MessageRole.User -> "我"
        MessageRole.Assistant -> message.label ?: "AI"
        MessageRole.System -> "系统"
    }
    
    val roleColor = when (message.role) {
        MessageRole.User -> Color(0xFF007AFF)
        MessageRole.Assistant -> Color(0xFF34C759)
        MessageRole.System -> Color(0xFFFF9500)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(roleColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = roleLabel.first().toString(),
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = roleLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            Spacer(modifier = Modifier.height(6.dp))
            SelectionContainer {
                Text(
                    text = message.text,
                    fontSize = 15.sp,
                    color = Color(0xFF333333),
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun ChatComposer(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isRunning: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .navigationBarsPadding()
            .imePadding()
    ) {
        if (isRunning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "⏳",
                    color = Color(0xFF007AFF),
                    fontSize = 12.sp
                )
                Text(
                    "正在生成回复...",
                    fontSize = 13.sp,
                    color = Color(0xFF666666)
                )
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFFF0F0F0), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                textStyle = TextStyle(
                    color = Color(0xFF333333),
                    fontSize = 15.sp
                ),
                cursorBrush = SolidColor(Color(0xFF007AFF)),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            "输入消息",
                            fontSize = 15.sp,
                            color = Color(0xFF999999)
                        )
                    }
                    innerTextField()
                }
            )
            
            IconButton(
                onClick = onSendClick,
                enabled = enabled && value.isNotBlank(),
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (enabled && value.isNotBlank()) Color(0xFF007AFF)
                        else Color(0xFFE0E0E0),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "发送",
                    tint = if (enabled && value.isNotBlank()) Color.White
                    else Color(0xFF999999),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

