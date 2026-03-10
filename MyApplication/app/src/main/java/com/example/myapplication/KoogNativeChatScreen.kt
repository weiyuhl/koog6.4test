package com.example.myapplication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NativeChatScreen(
    provider: KoogProvider,
    prompt: String,
    isRunning: Boolean,
    messages: List<NativeChatMessage>,
    onPromptChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onMenuClick: () -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, isRunning) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("聊天")
                        Text(provider.displayName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) { Text("☰") }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                NativeChatComposer(
                    value = prompt,
                    enabled = !isRunning,
                    onValueChange = onPromptChanged,
                    onSendClick = onSendClick,
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        ) {
            item {
                AssistChip(onClick = {}, label = { Text(if (isRunning) "模型回复中" else "已就绪") })
            }
            if (messages.isEmpty()) {
                item {
                    Card {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("开始新对话", style = MaterialTheme.typography.titleMedium)
                            Text("先从左上角抽屉进入设置，确认模型配置后就可以直接开始聊天。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            items(messages, key = { it.id }) { message -> NativeMessageBubble(message = message) }
        }
    }
}

@Composable
private fun NativeMessageBubble(message: NativeChatMessage) {
    val isUser = message.role == NativeMessageRole.User
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = when (message.role) {
        NativeMessageRole.User -> MaterialTheme.colorScheme.primaryContainer
        NativeMessageRole.Assistant -> MaterialTheme.colorScheme.surfaceContainerHighest
        NativeMessageRole.System -> MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = if (message.role == NativeMessageRole.User) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Card(
            modifier = Modifier.fillMaxWidth(0.86f),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            message.label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
                SelectionContainer {
                    Text(text = message.text, style = MaterialTheme.typography.bodyLarge, color = textColor)
                }
            }
        }
    }
}

@Composable
private fun NativeChatComposer(value: String, enabled: Boolean, onValueChange: (String) -> Unit, onSendClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                label = { Text("输入消息") },
                placeholder = { Text("配置项在设置页中管理") },
                minLines = 2,
                maxLines = 5,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (enabled) "准备发送" else "正在请求模型…",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = onSendClick, enabled = enabled && value.isNotBlank()) {
                    Text("发送")
                }
            }
        }
    }
}

