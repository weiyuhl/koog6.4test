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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lhzkml.codestudio.Preset
import com.lhzkml.codestudio.Provider
import com.lhzkml.codestudio.components.Bar
import com.lhzkml.codestudio.components.DropdownField
import com.lhzkml.codestudio.components.Switch
import com.lhzkml.codestudio.settings.components.SettingsItem
import com.lhzkml.codestudio.settingsSummary
import com.lhzkml.codestudio.ui.model.SettingsHomeUiModel

@Composable
internal fun SettingsHomeScreen(
    uiModel: SettingsHomeUiModel,
    onBackClick: () -> Unit,
    onOpenProvider: (Provider) -> Unit,
    onOpenRuntime: () -> Unit,
    onOpenOssLicenses: () -> Unit,
    onProviderEnabledChange: (Provider, Boolean) -> Unit,
    onRuntimeChange: (Preset) -> Unit,
    enabledProviders: Set<String> = emptySet()
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
                // 供应商列表标题
                BasicText(
                    text = "模型供应商",
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color(0xFF333333),
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                // 供应商列表
                Provider.entries.filter { it.isSupportedOnAndroid }.forEach { provider ->
                    val isEnabled = enabledProviders.contains(provider.name)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .clickable { onOpenProvider(provider) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            BasicText(
                                text = provider.displayName,
                                style = TextStyle(
                                    fontSize = 15.sp,
                                    color = Color(0xFF333333),
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                        
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { enabled ->
                                onProviderEnabledChange(provider, enabled)
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
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
