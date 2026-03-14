package com.lhzkml.codestudio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lhzkml.codestudio.components.Bar
import com.lhzkml.codestudio.components.IconButton
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import android.widget.TextView

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
    val coroutineScope = rememberCoroutineScope()
    
    // 使用 reverseLayout 让最新消息在底部
    // 监听消息变化并自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            // 新消息时，滚动到最后一项（索引 0，因为是 reverseLayout）
            listState.animateScrollToItem(0)
        }
    }
    
    // 监听最后一条消息的文本长度变化（流式更新）
    LaunchedEffect(messages.firstOrNull()?.text?.length) {
        if (messages.isNotEmpty() && listState.firstVisibleItemIndex == 0) {
            // 只有当最后一条消息可见时才自动滚动
            coroutineScope.launch {
                listState.animateScrollToItem(0)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Bar(
            title = "Chat",
            navigationIcon = {
                IconButton(onClick = onMenuClick) {
                    BasicText(
                        text = "☰",
                        style = TextStyle(
                            fontSize = 24.sp,
                            color = Color(0xFF333333)
                        )
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    reverseLayout = true  // 反转布局，最新消息在底部
                ) {
                    items(messages.reversed(), key = { it.id }) { message ->
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
            BasicText(
                text = "💬",
                style = TextStyle(
                    fontSize = 64.sp
                )
            )
            BasicText(
                text = "开始对话",
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF333333)
                )
            )
            BasicText(
                text = "在下方输入消息",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color(0xFF999999)
                )
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
    
    val isUser = message.role == MessageRole.User
    val isAssistant = message.role == MessageRole.Assistant

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // 头像和角色名称行
        Row(
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isUser) {
                // AI/系统消息：头像在左
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(roleColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = roleLabel.first().toString(),
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                BasicText(
                    text = roleLabel,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                )
            } else {
                // 用户消息：头像在右
                BasicText(
                    text = roleLabel,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                )
                Spacer(modifier = Modifier.size(8.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(roleColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = roleLabel.first().toString(),
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
        
        // 消息内容
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            if (isAssistant) {
                // AI 消息使用 Markdown 渲染
                MarkdownText(
                    text = message.text,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // 用户和系统消息使用普通文本
                SelectionContainer {
                    BasicText(
                        text = message.text,
                        style = TextStyle(
                            fontSize = 15.sp,
                            color = Color(0xFF333333),
                            lineHeight = 22.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())  // 删除线支持 ~~text~~
            .usePlugin(TablePlugin.create(context))  // 表格支持
            .usePlugin(TaskListPlugin.create(context))  // 任务列表支持 - [ ] 和 - [x]
            .usePlugin(HtmlPlugin.create())  // HTML 标签支持
            .usePlugin(ImagesPlugin.create())  // 图片支持
            .usePlugin(LinkifyPlugin.create())  // 自动识别链接
            .build()
    }
    
    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                textSize = 15f
                setTextColor(Color(0xFF333333).toArgb())
                setTextIsSelectable(true)
                // 设置行间距，让表格和列表更易读
                setLineSpacing(0f, 1.2f)
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, text)
        },
        modifier = modifier
    )
}

@Composable
private fun ChatComposer(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isRunning: Boolean
) {
    val composerShape = RoundedCornerShape(22.dp)
    val sendEnabled = enabled && value.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (isRunning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BasicText(
                    text = "⏳",
                    style = TextStyle(
                        color = Color(0xFF10A37F),
                        fontSize = 12.sp
                    )
                )
                BasicText(
                    text = "正在生成回复...",
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = Color(0xFF666666)
                    )
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFE8E8E8), composerShape)
                .background(Color.White, composerShape)
                .padding(start = 16.dp, top = 10.dp, end = 12.dp, bottom = 8.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                singleLine = false,
                maxLines = 3,
                textStyle = TextStyle(
                    color = Color(0xFF1F2937),
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                cursorBrush = SolidColor(Color(0xFF111111)),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, end = 8.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        if (value.isEmpty()) {
                            BasicText(
                                text = "询问任何问题",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, Color(0xFFE5E7EB), CircleShape)
                        .background(Color(0xFFF8F8F8), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "+",
                        style = TextStyle(
                            fontSize = 20.sp,
                            color = Color(0xFF6B7280),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                IconButton(
                    onClick = onSendClick,
                    enabled = sendEnabled,
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            if (sendEnabled) Color(0xFF111111) else Color(0xFFF3F4F6),
                            CircleShape
                        )
                ) {
                    BasicText(
                        text = "↑",
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sendEnabled) Color.White else Color(0xFF9CA3AF)
                        )
                    )
                }
            }
        }
    }
}

