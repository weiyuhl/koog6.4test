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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lhzkml.codestudio.components.Bar
import com.lhzkml.codestudio.components.DropdownField
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
                    BasicText(
                        text = "⚠️ ${settingsSummary(uiModel.errors)}",
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = Color(0xFFC62828)
                        )
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
            BasicText(
                text = label,
                style = TextStyle(
                    fontSize = 15.sp,
                    color = Color(0xFF333333),
                    fontWeight = FontWeight.Medium
                )
            )
            if (value != null) {
                Spacer(modifier = Modifier.height(4.dp))
                BasicText(
                    text = value,
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = Color(0xFF999999)
                    )
                )
            }
        }
        BasicText(
            text = "→",
            style = TextStyle(
                fontSize = 16.sp,
                color = Color(0xFF999999)
            )
        )
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
    onLoadOpenRouterKeyInfo: () -> Unit = {},
    onLoadAvailableModels: () -> Unit = {},
    openRouterKeyInfo: com.lhzkml.codestudio.viewmodel.OpenRouterKeyInfo? = null,
    isLoadingKeyInfo: Boolean = false,
    availableModels: List<com.lhzkml.codestudio.viewmodel.OpenRouterModelInfo> = emptyList(),
    isLoadingModels: Boolean = false,
    modelSearchQuery: String = "",
    onModelSearchQueryChange: (String) -> Unit = {},
    modelFilterFree: Boolean? = null,
    onModelFilterFreeChange: (Boolean?) -> Unit = {},
    modelFilterInputModalities: Set<String> = emptySet(),
    onToggleInputModality: (String) -> Unit = {},
    modelFilterOutputModalities: Set<String> = emptySet(),
    onToggleOutputModality: (String) -> Unit = {},
    modelSortBy: com.lhzkml.codestudio.viewmodel.ModelSortOption = com.lhzkml.codestudio.viewmodel.ModelSortOption.NEWEST,
    onModelSortByChange: (com.lhzkml.codestudio.viewmodel.ModelSortOption) -> Unit = {}
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
                
                // OpenRouter 特有功能
                if (uiModel.providerDisplayName == "OpenRouter") {
                    OpenRouterKeyInfoCard(
                        keyInfo = openRouterKeyInfo,
                        isLoading = isLoadingKeyInfo,
                        onClick = onLoadOpenRouterKeyInfo
                    )
                    
                    OpenRouterModelsCard(
                        models = availableModels,
                        isLoading = isLoadingModels,
                        onClick = onLoadAvailableModels,
                        onModelSelected = onModelIdChanged,
                        searchQuery = modelSearchQuery,
                        onSearchQueryChange = onModelSearchQueryChange,
                        filterFree = modelFilterFree,
                        onFilterFreeChange = onModelFilterFreeChange,
                        filterInputModalities = modelFilterInputModalities,
                        onToggleInputModality = onToggleInputModality,
                        filterOutputModalities = modelFilterOutputModalities,
                        onToggleOutputModality = onToggleOutputModality,
                        sortBy = modelSortBy,
                        onSortByChange = onModelSortByChange
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
        
        // 余额信息显示
        if (balanceInfo != null && !isChecking) {
            Spacer(modifier = Modifier.height(12.dp))
            
            if (balanceInfo.errorMessage != null) {
                // 错误信息
                BasicText(
                    text = balanceInfo.errorMessage,
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFFE53935)
                    )
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

@Composable
private fun OpenRouterKeyInfoCard(
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
private fun OpenRouterModelsCard(
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
private fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (selected) Color(0xFF2196F3) else Color(0xFFE0E0E0),
                androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        BasicText(
            text = text,
            style = TextStyle(
                fontSize = 12.sp,
                color = if (selected) Color.White else Color(0xFF666666)
            )
        )
    }
}

@Composable
private fun SortOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    if (selected) Color(0xFF2196F3) else Color.Transparent,
                    androidx.compose.foundation.shape.CircleShape
                )
                .padding(2.dp)
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        BasicText(
            text = text,
            style = TextStyle(
                fontSize = 13.sp,
                color = if (selected) Color(0xFF333333) else Color(0xFF666666)
            )
        )
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
