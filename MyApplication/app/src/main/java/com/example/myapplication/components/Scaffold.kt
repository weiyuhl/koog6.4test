package com.example.myapplication.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.AppColors

@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    backgroundColor: Color = AppColors.Background,
    content: @Composable (PaddingValues) -> Unit
) {
    Column(modifier = modifier.background(backgroundColor).fillMaxSize()) {
        topBar()
        Box(modifier = Modifier.weight(1f)) {
            content(PaddingValues(0.dp))
        }
        bottomBar()
    }
}
