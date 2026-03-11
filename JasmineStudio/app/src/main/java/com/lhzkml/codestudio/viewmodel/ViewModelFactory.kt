package com.lhzkml.codestudio.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lhzkml.codestudio.LocalStore
import com.lhzkml.codestudio.repository.ChatRepositoryImpl
import com.lhzkml.codestudio.repository.SettingsRepositoryImpl
import com.lhzkml.codestudio.usecase.SendMessageUseCase

internal class ViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    
    private val localStore by lazy { LocalStore(context) }
    private val chatRepository by lazy { ChatRepositoryImpl(localStore) }
    private val settingsRepository by lazy { SettingsRepositoryImpl(localStore) }
    private val sendMessageUseCase by lazy { SendMessageUseCase() }
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> {
                ChatViewModel(chatRepository, settingsRepository, sendMessageUseCase) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(settingsRepository) as T
            }
            modelClass.isAssignableFrom(NavigationViewModel::class.java) -> {
                NavigationViewModel() as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
