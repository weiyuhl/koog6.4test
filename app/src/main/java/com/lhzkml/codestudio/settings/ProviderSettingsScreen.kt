package com.lhzkml.codestudio.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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
                
                // 供应商特有功能 - 按供应商分类
                when (uiModel.providerDisplayName) {
                    "OpenRouter" -> {
                        // OpenRouter 特有功能
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
                    "DeepSeek" -> {
                        // DeepSeek 特有功能
                        DeepSeekBalanceCard(
                            balanceInfo = uiModel.balanceInfo,
                            isChecking = uiModel.isCheckingBalance,
                            onClick = onCheckBalance
                        )
                    }
                    "硅基流动" -> {
                        // 硅基流动特有功能
                        SiliconFlowBalanceCard(
                            balanceInfo = uiModel.balanceInfo,
                            isChecking = uiModel.isCheckingBalance,
                            onClick = onCheckBalance
                        )
                        
                        SiliconFlowModelsCard(
                            models = siliconFlowModels,
                            isLoading = isLoadingSiliconFlowModels,
                            onClick = onLoadSiliconFlowModels,
                            onModelSelected = onModelIdChanged,
                            searchQuery = siliconFlowModelSearchQuery,
                            onSearchQueryChange = onSiliconFlowModelSearchQueryChange,
                            filterType = siliconFlowModelFilterType,
                            onFilterTypeChange = onSiliconFlowModelFilterTypeChange
                        )
                    }
                    else -> {
                        // 其他供应商的通用余额查询
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
}
