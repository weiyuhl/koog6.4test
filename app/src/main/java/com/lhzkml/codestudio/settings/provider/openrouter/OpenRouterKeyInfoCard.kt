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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * OpenRouter 密钥信息卡片
 * 对应框架层 OpenRouterClient.getKeyInfo()
 */
@Composable
internal fun OpenRouterKeyInfoCard(
    keyInfo: com.lhzkml.codestudio.viewmodel.OpenRouterKeyInfo?,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isLoading, onClick = onClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = "API 密钥信息",
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
                    text = "查询",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFF2196F3)
                    )
                )
            }
        }
        
        if (keyInfo != null && !isLoading) {
            Spacer(modifier = Modifier.height(12.dp))
            
            if (keyInfo.errorMessage != null) {
                BasicText(
                    text = keyInfo.errorMessage,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFFE53935)
                    )
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    keyInfo.label?.let { label ->
                        InfoRow("标签", label)
                    }
                    
                    if (keyInfo.limitRemaining != null) {
                        InfoRow(
                            "当前余额", 
                            "$${String.format("%.4f", keyInfo.limitRemaining)}",
                            valueColor = if (keyInfo.limitRemaining > 0) Color(0xFF4CAF50) else Color(0xFFE53935)
                        )
                    }
                    
                    if (keyInfo.limit != null) {
                        InfoRow("总额度", "$${String.format("%.4f", keyInfo.limit)}")
                    }
                    
                    if (keyInfo.usage != null) {
                        InfoRow("已使用", "$${String.format("%.4f", keyInfo.usage)}")
                    }
                    
                    if (keyInfo.usageDaily != null) {
                        InfoRow("今日使用", "$${String.format("%.4f", keyInfo.usageDaily)}")
                    }
                    
                    if (keyInfo.usageWeekly != null) {
                        InfoRow("本周使用", "$${String.format("%.4f", keyInfo.usageWeekly)}")
                    }
                    
                    if (keyInfo.usageMonthly != null) {
                        InfoRow("本月使用", "$${String.format("%.4f", keyInfo.usageMonthly)}")
                    }
                    
                    if (keyInfo.limitReset != null) {
                        InfoRow("限额重置", keyInfo.limitReset)
                    }
                    
                    if (keyInfo.byokUsage != null) {
                        InfoRow("BYOK 使用", "$${String.format("%.4f", keyInfo.byokUsage)}")
                    }
                    
                    if (keyInfo.isFree) {
                        InfoRow("类型", "免费层级", valueColor = Color(0xFFFF9800))
                    }
                    
                    if (keyInfo.isManagementKey) {
                        InfoRow("密钥类型", "管理密钥", valueColor = Color(0xFF2196F3))
                    }
                    
                    keyInfo.rateLimit?.let { limit ->
                        InfoRow("速率限制", limit)
                    }
                    
                    keyInfo.expiresAt?.let { expires ->
                        InfoRow("过期时间", expires, valueColor = Color(0xFFFF9800))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFF333333)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                fontSize = 13.sp,
                color = Color(0xFF666666)
            )
        )
        BasicText(
            text = value,
            style = TextStyle(
                fontSize = 14.sp,
                color = valueColor,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
