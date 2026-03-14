package com.lhzkml.codestudio.settings.provider.siliconflow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lhzkml.codestudio.TextField
import com.lhzkml.codestudio.settings.components.FilterChip

/**
 * 硅基流动可用模型列表卡片
 * 对应框架层 SiliconFlowClient.listModels()（继承自 OpenAICompatibleClient）
 */
@Composable
internal fun SiliconFlowModelsCard(
    models: List<com.lhzkml.codestudio.viewmodel.SiliconFlowModelInfo>,
    isLoading: Boolean,
    onClick: () -> Unit,
    onModelSelected: (String) -> Unit,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    filterType: String? = null,
    onFilterTypeChange: (String?) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        // 标题和加载按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isLoading, onClick = onClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = "可用模型列表",
                style = TextStyle(
                    fontSize = 15.sp,
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.Medium
                )
            )
            if (isLoading) {
                BasicText(
                    text = "加载中...",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFF999999)
                    )
                )
            } else {
                BasicText(
                    text = if (models.isEmpty()) "加载" else "刷新",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFF2196F3)
                    )
                )
            }
        }
        
        if (models.isNotEmpty() && !isLoading) {
            Spacer(modifier = Modifier.height(12.dp))
            
            // 搜索框
            TextField(
                "搜索模型",
                searchQuery,
                onSearchQueryChange,
                placeholder = "输入模型名称或 ID"
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 类型筛选
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicText(
                    text = "模型类型:",
                    style = TextStyle(fontSize = 12.sp, color = Color(0xFF666666))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        text = "全部",
                        selected = filterType == null,
                        onClick = { onFilterTypeChange(null) }
                    )
                    FilterChip(
                        text = "文本",
                        selected = filterType == "text",
                        onClick = { onFilterTypeChange("text") }
                    )
                    FilterChip(
                        text = "图像",
                        selected = filterType == "image",
                        onClick = { onFilterTypeChange("image") }
                    )
                    FilterChip(
                        text = "音频",
                        selected = filterType == "audio",
                        onClick = { onFilterTypeChange("audio") }
                    )
                    FilterChip(
                        text = "视频",
                        selected = filterType == "video",
                        onClick = { onFilterTypeChange("video") }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            BasicText(
                text = "共 ${models.size} 个模型",
                style = TextStyle(
                    fontSize = 13.sp,
                    color = Color(0xFF666666)
                )
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 模型列表
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                models.forEach { model ->
                    ModelInfoItem(
                        model = model,
                        onSelect = { onModelSelected(model.id) }
                    )
                }
                
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun ModelInfoItem(
    model: com.lhzkml.codestudio.viewmodel.SiliconFlowModelInfo,
    onSelect: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            .clickable(onClick = onSelect)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                BasicText(
                    text = model.name ?: model.id,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFF333333),
                        fontWeight = FontWeight.Medium
                    )
                )
                
                if (model.name != null && model.name != model.id) {
                    Spacer(modifier = Modifier.height(2.dp))
                    BasicText(
                        text = model.id,
                        style = TextStyle(
                            fontSize = 11.sp,
                            color = Color(0xFF999999)
                        )
                    )
                }
            }
            
            BasicText(
                text = "选择",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color(0xFF2196F3),
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}
