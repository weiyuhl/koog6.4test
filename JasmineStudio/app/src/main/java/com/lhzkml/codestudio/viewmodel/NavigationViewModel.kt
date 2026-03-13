package com.lhzkml.codestudio.viewmodel

import androidx.lifecycle.ViewModel
import com.lhzkml.codestudio.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

internal data class NavigationUiState(
    val currentRoute: String = Route.Chat.value
)

internal sealed interface NavigationEvent {
    data class NavigateTo(val route: String) : NavigationEvent
    data object NavigateBack : NavigationEvent
}

@HiltViewModel
internal class NavigationViewModel @Inject constructor() : ViewModel() {
    
    private val _uiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()
    
    private val backStack = mutableListOf<String>()
    
    fun onEvent(event: NavigationEvent) {
        when (event) {
            is NavigationEvent.NavigateTo -> navigateTo(event.route)
            is NavigationEvent.NavigateBack -> navigateBack()
        }
    }
    
    private fun navigateTo(route: String) {
        val currentRoute = _uiState.value.currentRoute
        if (currentRoute != route) {
            backStack.add(currentRoute)
            _uiState.update { it.copy(currentRoute = route) }
        }
    }
    
    private fun navigateBack() {
        if (backStack.isNotEmpty()) {
            val previousRoute = backStack.removeLast()
            _uiState.update { it.copy(currentRoute = previousRoute) }
        }
    }
    
    fun canNavigateBack(): Boolean = backStack.isNotEmpty()
}
