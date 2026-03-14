package com.lhzkml.codestudio.settings.components

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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SettingsItem(
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
internal fun FilterChip(
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
internal fun SortOption(
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
