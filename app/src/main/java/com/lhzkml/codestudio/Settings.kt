package com.lhzkml.codestudio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lhzkml.codestudio.components.Bar
import com.lhzkml.codestudio.components.DropdownField
import com.lhzkml.codestudio.components.Text
import com.lhzkml.codestudio.ui.model.SettingsHomeUiModel
import com.lhzkml.codestudio.ui.model.ProviderSettingsUiModel
import com.lhzkml.codestudio.ui.model.RuntimeSettingsUiModel

@Composable
internal fun SettingsHomeScreen(
    uiModel: SettingsHomeUiModel,
    onBackClick: () -> Unit,
    onOpenProvider: () -> Unit,
    onOpenRuntime: () -> Unit,
    onOpenOssLicenses: () -> Unit,
    onProviderChange: (Provider) -> Unit,
    onRuntimeChange: (Preset) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Bar(
            title = "设置",
            onBackClick = onBackClick
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (uiModel.errors.hasAny()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFEBEE))
                        .padding(16.dp)
                ) {
                    Text(
                        "⚠️ ${settingsSummary(uiModel.errors)}",
                        fontSize = 14.sp,
                        color = Color(0xFFC62828)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 当前供应商（点击进入配置）
                SettingsItem(
                    label = "模型供应商",
                    value = uiModel.providerDisplayName,
                    onClick = onOpenProvider
                )
                
                DropdownField(
                    label = "运行模式",
                    value = uiModel.runtimePresetTitle,
                    items = Preset.entries,
                    itemLabel = { it.title },
                    onItemSelected = { preset ->
                        onRuntimeChange(preset)
                        onOpenRuntime()
                    }
                )
                
                // 开源许可入口
                SettingsItem(
                    label = "开源许可",
                    onClick = onOpenOssLicenses
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsItem(
    label: String,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = label,
                fontSize = 15.sp,
                color = Color(0xFF333333),
                fontWeight = FontWeight.Medium
            )
            if (value != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 13.sp,
                    color = Color(0xFF999999)
                )
            }
        }
        Text(text = "→", fontSize = 16.sp, color = Color(0xFF999999))
    }
}

@Composable
internal fun ProviderSettingsScreen(
    uiModel: ProviderSettingsUiModel,
    onBackClick: () -> Unit,
    onProviderChange: (Provider) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onModelIdChanged: (String) -> Unit,
    onBaseUrlChanged: (String) -> Unit,
    onExtraConfigChanged: (String) -> Unit,
    onCheckBalance: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Bar(
            title = "供应商配置",
            onBackClick = onBackClick
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 供应商选择
                DropdownField(
                    label = "选择供应商",
                    value = uiModel.providerDisplayName,
                    items = Provider.entries.filter { it.isSupportedOnAndroid },
                    itemLabel = { it.displayName },
                    onItemSelected = onProviderChange
                )
                
                TextField(
                    "模型 ID",
                    uiModel.modelId,
                    onModelIdChanged,
                    uiModel.errors.modelId,
                    uiModel.modelIdPlaceholder
                )
                
                if (uiModel.showApiKey) {
                    TextField(
                        "API Key",
                        uiModel.apiKey,
                        onApiKeyChanged,
                        uiModel.errors.apiKey,
                        uiModel.apiKeyPlaceholder,
                        secure = true
                    )
                }
                
                if (uiModel.showBaseUrl) {
                    TextField(
                        uiModel.baseUrlLabel,
                        uiModel.baseUrl,
                        onBaseUrlChanged,
                        uiModel.errors.baseUrl,
                        uiModel.baseUrlPlaceholder,
                        keyboardType = KeyboardType.Uri
                    )
                }
                
                uiModel.extraFieldLabel?.let { label ->
                    if (uiModel.showExtraField) {
                        TextField(
                            label,
                            uiModel.extraConfig,
                            onExtraConfigChanged,
                            uiModel.errors.extraConfig,
                            uiModel.extraFieldPlaceholder
                        )
                    }
                }
                
                // 查询余额按钮
                if (uiModel.supportsBalanceCheck) {
                    BalanceCheckCard(
                        balanceInfo = uiModel.balanceInfo,
                        isChecking = uiModel.isCheckingBalance,
                        onClick = onCheckBalance
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BalanceCheckCard(
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
        // 标题和查询按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isChecking, onClick = onClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "账户余额",
                fontSize = 15.sp,
                color = Color(0xFF333333),
                fontWeight = FontWeight.Medium
            )
            if (isChecking) {
                Text(
                    text = "查询中...",
                    fontSize = 14.sp,
                    color = Color(0xFF999999)
                )
            } else {
                Text(
                    text = "查询",
                    fontSize = 14.sp,
                    color = Color(0xFF2196F3)
                )
            }
        }
        
        // 余额信息显示
        if (balanceInfo != null && !isChecking) {
            Spacer(modifier = Modifier.height(12.dp))
            
            if (balanceInfo.errorMessage != null) {
                // 错误信息
                Text(
                    text = balanceInfo.errorMessage,
                    fontSize = 14.sp,
                    color = Color(0xFFE53935)
                )
            } else {
                // 余额详情
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 总余额
                    BalanceRow(
                        label = "总余额",
                        value = "${balanceInfo.currency} ${balanceInfo.totalBalance}",
                        isTotal = true,
                        isAvailable = balanceInfo.isAvailable
                    )
                    
                    // 赠送余额
                    if (balanceInfo.grantedBalance != null) {
                        BalanceRow(
                            label = "赠送余额",
                            value = "${balanceInfo.currency} ${balanceInfo.grantedBalance}",
                            isTotal = false,
                            isAvailable = true
                        )
                    }
                    
                    // 充值余额
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
        Text(
            text = label,
            fontSize = if (isTotal) 14.sp else 13.sp,
            color = if (isTotal) Color(0xFF333333) else Color(0xFF666666),
            fontWeight = if (isTotal) FontWeight.Medium else FontWeight.Normal
        )
        Text(
            text = value,
            fontSize = if (isTotal) 16.sp else 14.sp,
            color = if (isAvailable) Color(0xFF4CAF50) else Color(0xFFE53935),
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
internal fun RuntimeSettingsScreen(
    uiModel: RuntimeSettingsUiModel,
    onBackClick: () -> Unit,
    onSystemPromptChanged: (String) -> Unit,
    onTemperatureChanged: (String) -> Unit,
    onMaxIterationsChanged: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Bar(
            title = uiModel.runtimePresetTitle,
            onBackClick = onBackClick
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    "System Prompt",
                    uiModel.systemPrompt,
                    onSystemPromptChanged,
                    placeholder = uiModel.systemPromptPlaceholder,
                    singleLine = false
                )
                
                TextField(
                    "Temperature",
                    uiModel.temperature,
                    onTemperatureChanged,
                    uiModel.errors.temperature,
                    uiModel.temperaturePlaceholder,
                    keyboardType = KeyboardType.Decimal
                )
                
                TextField(
                    "Max Iterations",
                    uiModel.maxIterations,
                    onMaxIterationsChanged,
                    uiModel.errors.maxIterations,
                    uiModel.maxIterationsPlaceholder,
                    keyboardType = KeyboardType.Number
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


