package com.lhzkml.codestudio.di

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
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { SettingsDataStore(androidContext()) }
    single { MessagesDataStore(androidContext()) }
    
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<ChatRepository> { ChatRepositoryImpl(get()) }
    
    factory { SendMessageUseCase() }
    
    viewModel { ChatViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { NavigationViewModel() }
}
