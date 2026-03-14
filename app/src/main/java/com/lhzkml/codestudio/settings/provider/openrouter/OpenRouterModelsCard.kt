package com.lhzkml.codestudio.settings.provider.openrouter

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
import com.lhzkml.codestudio.settings.components.SortOption

/**
 * OpenRouter 可用模型列表卡片
 * 对应框架层 OpenRouterClient.listModelsRaw()
 */
@Composable
internal fun OpenRouterModelsCard(
    models: List<com.lhzkml.codestudio.viewmodel.OpenRouterModelInfo>,
    isLoading: Boolean,
    onClick: () -> Unit,
    onModelSelected: (String) -> Unit,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    filterFree: Boolean? = null,
    onFilterFreeChange: (Boolean?) -> Unit = {},
    filterInputModalities: Set<String> = emptySet(),
    onToggleInputModality: (String) -> Unit = {},
    filterOutputModalities: Set<String> = emptySet(),
    onToggleOutputModality: (String) -> Unit = {},
    sortBy: com.lhzkml.codestudio.viewmodel.ModelSortOption = com.lhzkml.codestudio.viewmodel.ModelSortOption.NEWEST,
    onSortByChange: (com.lhzkml.codestudio.viewmodel.ModelSortOption) -> Unit = {}
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
            
            // 筛选选项
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 价格筛选
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        text = "全部",
                        selected = filterFree == null,
                        onClick = { onFilterFreeChange(null) }
                    )
                    FilterChip(
                        text = "免费",
                        selected = filterFree == true,
                        onClick = { onFilterFreeChange(true) }
                    )
                    FilterChip(
                        text = "付费",
                        selected = filterFree == false,
                        onClick = { onFilterFreeChange(false) }
                    )
                }
                
                // 输入模态筛选
                BasicText(
                    text = "输入模态:",
                    style = TextStyle(fontSize = 12.sp, color = Color(0xFF666666))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("text", "image", "audio", "video").forEach { modality ->
                        FilterChip(
                            text = modality,
                            selected = modality in filterInputModalities,
                            onClick = { onToggleInputModality(modality) }
                        )
                    }
                }
                
                // 输出模态筛选
                BasicText(
                    text = "输出模态:",
                    style = TextStyle(fontSize = 12.sp, color = Color(0xFF666666))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("text", "image", "audio").forEach { modality ->
                        FilterChip(
                            text = modality,
                            selected = modality in filterOutputModalities,
                            onClick = { onToggleOutputModality(modality) }
                        )
                    }
                }
                
                // 排序选项
                BasicText(
                    text = "排序:",
                    style = TextStyle(fontSize = 12.sp, color = Color(0xFF666666))
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SortOption("最新", sortBy == com.lhzkml.codestudio.viewmodel.ModelSortOption.NEWEST) {
                        onSortByChange(com.lhzkml.codestudio.viewmodel.ModelSortOption.NEWEST)
                    }
                    SortOption("价格: 低到高", sortBy == com.lhzkml.codestudio.viewmodel.ModelSortOption.PRICE_LOW_TO_HIGH) {
                        onSortByChange(com.lhzkml.codestudio.viewmodel.ModelSortOption.PRICE_LOW_TO_HIGH)
                    }
                    SortOption("价格: 高到低", sortBy == com.lhzkml.codestudio.viewmodel.ModelSortOption.PRICE_HIGH_TO_LOW) {
                        onSortByChange(com.lhzkml.codestudio.viewmodel.ModelSortOption.PRICE_HIGH_TO_LOW)
                    }
                    SortOption("上下文: 高到低", sortBy == com.lhzkml.codestudio.viewmodel.ModelSortOption.CONTEXT_HIGH_TO_LOW) {
                        onSortByChange(com.lhzkml.codestudio.viewmodel.ModelSortOption.CONTEXT_HIGH_TO_LOW)
                    }
                    SortOption("上下文: 低到高", sortBy == com.lhzkml.codestudio.viewmodel.ModelSortOption.CONTEXT_LOW_TO_HIGH) {
                        onSortByChange(com.lhzkml.codestudio.viewmodel.ModelSortOption.CONTEXT_LOW_TO_HIGH)
                    }
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
    model: com.lhzkml.codestudio.viewmodel.OpenRouterModelInfo,
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
        
        if (model.description != null) {
            Spacer(modifier = Modifier.height(6.dp))
            BasicText(
                text = model.description,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    lineHeight = 16.sp
                ),
                maxLines = 2
            )
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            model.contextLength?.let { length ->
                ModelInfoChip("上下文: ${formatNumber(length)}")
            }
            
            model.maxOutputTokens?.let { tokens ->
                ModelInfoChip("输出: ${formatNumber(tokens)}")
            }
        }
        
        if (model.promptPrice != null || model.completionPrice != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                model.promptPrice?.let { price ->
                    ModelInfoChip("输入: $price/1M tokens", Color(0xFF4CAF50))
                }
                
                model.completionPrice?.let { price ->
                    ModelInfoChip("输出: $price/1M tokens", Color(0xFFFF9800))
                }
            }
        }
        
        if (!model.supportedParameters.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            BasicText(
                text = "支持的参数: ${model.supportedParameters.joinToString(", ")}",
                style = TextStyle(
                    fontSize = 11.sp,
                    color = Color(0xFF666666),
                    lineHeight = 14.sp
                )
            )
        }
    }
}

@Composable
private fun ModelInfoChip(
    text: String,
    textColor: Color = Color(0xFF666666)
) {
    BasicText(
        text = text,
        style = TextStyle(
            fontSize = 11.sp,
            color = textColor
        )
    )
}

private fun formatNumber(num: Int): String {
    return when {
        num >= 1000000 -> "${num / 1000000}M"
        num >= 1000 -> "${num / 1000}K"
        else -> num.toString()
    }
}
