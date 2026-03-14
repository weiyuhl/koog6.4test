package com.lhzkml.codestudio.oss

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lhzkml.codestudio.components.Bar

@Composable
fun OssLicensesListScreen(
    title: String = "开源许可",
    onBack: () -> Unit,
    onPluginLicenseClick: (OssLicenseEntry) -> Unit,
    onManualLicenseClick: (ManualLicenseEntry) -> Unit
) {
    val context = LocalContext.current
    val pluginList = remember { OssLicenseLoader.loadLicenseList(context) }
    val manualList = remember { OssLicenseLoader.manualLicenses }
    val hasLicenses = remember { OssLicenseLoader.hasLicenses(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Bar(
            title = title,
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasLicenses && manualList.isEmpty()) {
                BasicText(
                    text = "许可信息仅在 release 构建中提供。请使用 release 版本查看开源许可。",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    ),
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                // 合并并去重：使用 distinctBy 按库名去重
                val allLicenses = (manualList.map { it.name to { onManualLicenseClick(it) } } +
                    pluginList.map { it.name to { onPluginLicenseClick(it) } })
                    .distinctBy { it.first }
                    .sortedBy { it.first }
                
                allLicenses.forEach { (name, onClick) ->
                    OssLicenseListItem(
                        name = name,
                        onClick = onClick
                    )
                }
            }
        }
    }
}

@Composable
private fun OssLicenseListItem(
    name: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BasicText(
            text = name,
            style = TextStyle(
                fontSize = 15.sp,
                color = Color(0xFF333333)
            ),
            modifier = Modifier.weight(1f)
        )
        BasicText(
            text = "→",
            style = TextStyle(
                fontSize = 16.sp,
                color = Color(0xFF999999)
            )
        )
    }
}
