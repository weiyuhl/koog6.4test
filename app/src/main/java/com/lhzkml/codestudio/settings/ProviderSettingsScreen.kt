package com.lhzkml.codestudio.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lhzkml.codestudio.TextField
import com.lhzkml.codestudio.components.Bar
import com.lhzkml.codestudio.settings.provider.deepseek.DeepSeekBalanceCard
import com.lhzkml.codestudio.settings.provider.openrouter.OpenRouterKeyInfoCard
import com.lhzkml.codestudio.settings.provider.openrouter.OpenRouterModelsCard
import com.lhzkml.codestudio.settings.provider.siliconflow.SiliconFlowBalanceCard
import com.lhzkml.codestudio.settings.provider.siliconflow.SiliconFlowModelsCard
import com.lhzkml.codestudio.ui.model.ProviderSettingsUiModel

@Composable
internal fun ProviderSettingsScreen(
    uiModel: ProviderSettingsUiModel,
    onBackClick: () -> Unit,
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
    onModelSortByChange: (com.lhzkml.codestudio.viewmodel.ModelSortOption) -> Unit = {},
    onLoadSiliconFlowModels: () -> Unit = {},
    siliconFlowModels: List<com.lhzkml.codestudio.viewmodel.SiliconFlowModelInfo> = emptyList(),
    isLoadingSiliconFlowModels: Boolean = false,
    siliconFlowModelSearchQuery: String = "",
    onSiliconFlowModelSearchQueryChange: (String) -> Unit = {},
    siliconFlowModelFilterType: String? = null,
    onSiliconFlowModelFilterTypeChange: (String?) -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("配置", "模型")

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        Bar(
            title = uiModel.providerDisplayName,
            onBackClick = onBackClick
        )

        // 内容区域
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> ConfigTab(
                    uiModel = uiModel,
                    onApiKeyChanged = onApiKeyChanged,
                    onModelIdChanged = onModelIdChanged,
                    onBaseUrlChanged = onBaseUrlChanged,
                    onExtraConfigChanged = onExtraConfigChanged,
                    onCheckBalance = onCheckBalance,
                    onLoadOpenRouterKeyInfo = onLoadOpenRouterKeyInfo,
                    openRouterKeyInfo = openRouterKeyInfo,
                    isLoadingKeyInfo = isLoadingKeyInfo
                )
                1 -> ModelsTab(
                    uiModel = uiModel,
                    availableModels = availableModels,
                    isLoadingModels = isLoadingModels,
                    onLoadAvailableModels = onLoadAvailableModels,
                    onModelIdChanged = onModelIdChanged,
                    modelSearchQuery = modelSearchQuery,
                    onModelSearchQueryChange = onModelSearchQueryChange,
                    modelFilterFree = modelFilterFree,
                    onModelFilterFreeChange = onModelFilterFreeChange,
                    modelFilterInputModalities = modelFilterInputModalities,
                    onToggleInputModality = onToggleInputModality,
                    modelFilterOutputModalities = modelFilterOutputModalities,
                    onToggleOutputModality = onToggleOutputModality,
                    modelSortBy = modelSortBy,
                    onModelSortByChange = onModelSortByChange,
                    onLoadSiliconFlowModels = onLoadSiliconFlowModels,
                    siliconFlowModels = siliconFlowModels,
                    isLoadingSiliconFlowModels = isLoadingSiliconFlowModels,
                    siliconFlowModelSearchQuery = siliconFlowModelSearchQuery,
                    onSiliconFlowModelSearchQueryChange = onSiliconFlowModelSearchQueryChange,
                    siliconFlowModelFilterType = siliconFlowModelFilterType,
                    onSiliconFlowModelFilterTypeChange = onSiliconFlowModelFilterTypeChange
                )
            }
        }

        // 底部 Tab 导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(top = 1.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTab == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = index }
                        .padding(vertical = 16.dp), // 增加垂直内边距使高度更高，更好点击
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BasicText(
                        text = title,
                        style = TextStyle(
                            fontSize = 18.sp, // 字号放大到 18.sp
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF1976D2) else Color(0xFF666666),
                            textAlign = TextAlign.Center
                        )
                    )
                    // 选中指示条
                    if (isSelected) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(3.dp)
                                .background(Color(0xFF1976D2))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab 1: 配置 + 余额查询
 */
@Composable
private fun ConfigTab(
    uiModel: ProviderSettingsUiModel,
    onApiKeyChanged: (String) -> Unit,
    onModelIdChanged: (String) -> Unit,
    onBaseUrlChanged: (String) -> Unit,
    onExtraConfigChanged: (String) -> Unit,
    onCheckBalance: () -> Unit,
    onLoadOpenRouterKeyInfo: () -> Unit,
    openRouterKeyInfo: com.lhzkml.codestudio.viewmodel.OpenRouterKeyInfo?,
    isLoadingKeyInfo: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

            // 余额查询 - 按供应商分类
            when (uiModel.providerDisplayName) {
                "OpenRouter" -> {
                    OpenRouterKeyInfoCard(
                        keyInfo = openRouterKeyInfo,
                        isLoading = isLoadingKeyInfo,
                        onClick = onLoadOpenRouterKeyInfo
                    )
                }
                "DeepSeek" -> {
                    DeepSeekBalanceCard(
                        balanceInfo = uiModel.balanceInfo,
                        isChecking = uiModel.isCheckingBalance,
                        onClick = onCheckBalance
                    )
                }
                "硅基流动" -> {
                    SiliconFlowBalanceCard(
                        balanceInfo = uiModel.balanceInfo,
                        isChecking = uiModel.isCheckingBalance,
                        onClick = onCheckBalance
                    )
                }
                else -> {
                    if (uiModel.supportsBalanceCheck) {
                        DeepSeekBalanceCard(
                            balanceInfo = uiModel.balanceInfo,
                            isChecking = uiModel.isCheckingBalance,
                            onClick = onCheckBalance
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Tab 2: 模型列表
 */
@Composable
private fun ModelsTab(
    uiModel: ProviderSettingsUiModel,
    availableModels: List<com.lhzkml.codestudio.viewmodel.OpenRouterModelInfo>,
    isLoadingModels: Boolean,
    onLoadAvailableModels: () -> Unit,
    onModelIdChanged: (String) -> Unit,
    modelSearchQuery: String,
    onModelSearchQueryChange: (String) -> Unit,
    modelFilterFree: Boolean?,
    onModelFilterFreeChange: (Boolean?) -> Unit,
    modelFilterInputModalities: Set<String>,
    onToggleInputModality: (String) -> Unit,
    modelFilterOutputModalities: Set<String>,
    onToggleOutputModality: (String) -> Unit,
    modelSortBy: com.lhzkml.codestudio.viewmodel.ModelSortOption,
    onModelSortByChange: (com.lhzkml.codestudio.viewmodel.ModelSortOption) -> Unit,
    onLoadSiliconFlowModels: () -> Unit,
    siliconFlowModels: List<com.lhzkml.codestudio.viewmodel.SiliconFlowModelInfo>,
    isLoadingSiliconFlowModels: Boolean,
    siliconFlowModelSearchQuery: String,
    onSiliconFlowModelSearchQueryChange: (String) -> Unit,
    siliconFlowModelFilterType: String?,
    onSiliconFlowModelFilterTypeChange: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (uiModel.providerDisplayName) {
                "OpenRouter" -> {
                    OpenRouterModelsCard(
                        models = availableModels,
                        isLoading = isLoadingModels,
                        onClick = onLoadAvailableModels,
                        selectedModelId = uiModel.modelId,
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
                "硅基流动" -> {
                    SiliconFlowModelsCard(
                        models = siliconFlowModels,
                        isLoading = isLoadingSiliconFlowModels,
                        onClick = onLoadSiliconFlowModels,
                        selectedModelId = uiModel.modelId,
                        onModelSelected = onModelIdChanged,
                        searchQuery = siliconFlowModelSearchQuery,
                        onSearchQueryChange = onSiliconFlowModelSearchQueryChange,
                        filterType = siliconFlowModelFilterType,
                        onFilterTypeChange = onSiliconFlowModelFilterTypeChange
                    )
                }
                else -> {
                    // 没有模型列表功能的供应商
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            text = "该供应商暂不支持模型列表查询",
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = Color(0xFF999999)
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
