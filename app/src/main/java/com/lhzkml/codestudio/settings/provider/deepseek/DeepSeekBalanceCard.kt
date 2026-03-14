package com.lhzkml.codestudio.settings.provider.deepseek

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
 * DeepSeek 供应商的余额查询卡片
 * 对应框架层 DeepSeekClient.getBalance()
 */
@Composable
internal fun DeepSeekBalanceCard(
    balanceInfo: com.lhzkml.codestudio.ui.model.BalanceDisplayInfo?,
    isChecking: Boolean,
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
                .clickable(enabled = !isChecking, onClick = onClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(
                text = "账户余额",
                style = TextStyle(
                    fontSize = 15.sp,
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.Medium
                )
            )
            if (isChecking) {
                BasicText(
                    text = "查询中...",
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
        
        if (balanceInfo != null && !isChecking) {
            Spacer(modifier = Modifier.height(12.dp))
            
            if (balanceInfo.errorMessage != null) {
                BasicText(
                    text = balanceInfo.errorMessage,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFFE53935)
                    )
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BalanceRow(
                        label = "总余额",
                        value = "${balanceInfo.currency} ${balanceInfo.totalBalance}",
                        isTotal = true,
                        isAvailable = balanceInfo.isAvailable
                    )
                    
                    if (balanceInfo.grantedBalance != null) {
                        BalanceRow(
                            label = "赠送余额",
                            value = "${balanceInfo.currency} ${balanceInfo.grantedBalance}",
                            isTotal = false,
                            isAvailable = true
                        )
                    }
                    
                    if (balanceInfo.toppedUpBalance != null) {
                        BalanceRow(
                            label = "充值余额",
                            value = "${balanceInfo.currency} ${balanceInfo.toppedUpBalance}",
                            isTotal = false,
                            isAvailable = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceRow(
    label: String,
    value: String,
    isTotal: Boolean,
    isAvailable: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                fontSize = if (isTotal) 14.sp else 13.sp,
                color = if (isTotal) Color(0xFF333333) else Color(0xFF666666),
                fontWeight = if (isTotal) FontWeight.Medium else FontWeight.Normal
            )
        )
        BasicText(
            text = value,
            style = TextStyle(
                fontSize = if (isTotal) 16.sp else 14.sp,
                color = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFE53935),
                fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}
