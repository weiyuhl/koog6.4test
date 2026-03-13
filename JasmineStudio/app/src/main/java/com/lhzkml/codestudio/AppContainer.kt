package com.lhzkml.codestudio

import android.content.Context
import com.lhzkml.codestudio.data.MessagesDataStore
import com.lhzkml.codestudio.data.SettingsDataStore
import com.lhzkml.codestudio.repository.ChatRepository
import com.lhzkml.codestudio.repository.ChatRepositoryImpl
import com.lhzkml.codestudio.repository.SettingsRepository
import com.lhzkml.codestudio.repository.SettingsRepositoryImpl
import com.lhzkml.codestudio.usecase.SendMessageUseCase
import com.lhzkml.codestudio.viewmodel.ChatViewModel
import com.lhzkml.codestudio.viewmodel.NavigationViewModel
import com.lhzkml.codestudio.viewmodel.SettingsViewModel

/**
 * 简单的依赖注入容器
 */
internal class AppContainer(context: Context) {
    
    // DataStore 层
    private val settingsDataStore = SettingsDataStore(context)
    private val messagesDataStore = MessagesDataStore(context)
    
    // Repository 层
    val settingsRepository: SettingsRepository = SettingsRepositoryImpl(settingsDataStore)
    val chatRepository: ChatRepository = ChatRepositoryImpl(messagesDataStore)
    
    // UseCase 层
    val sendMessageUseCase = SendMessageUseCase()
    
    // ViewModel 工厂方法
    fun createNavigationViewModel() = NavigationViewModel()
    
    fun createChatViewModel() = ChatViewModel(
        chatRepository = chatRepository,
        settingsRepository = settingsRepository,
        sendMessageUseCase = sendMessageUseCase
    )
    
    fun createSettingsViewModel() = SettingsViewModel(
        settingsRepository = settingsRepository
    )
}
