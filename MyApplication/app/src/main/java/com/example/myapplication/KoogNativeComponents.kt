package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.components.Text

@Composable
internal fun NativeTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    error: String? = null,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    secure: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = AppColors.OnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                color = AppColors.OnSurface,
                fontSize = 15.sp
            ),
            cursorBrush = SolidColor(AppColors.Primary),
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (secure) PasswordVisualTransformation() else VisualTransformation.None,
            decorationBox = { innerTextField ->
                if (value.isEmpty() && placeholder.isNotBlank()) {
                    Text(
                        placeholder,
                        fontSize = 15.sp,
                        color = AppColors.OnSurfaceVariant
                    )
                }
                innerTextField()
            }
        )
        if (error != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                error,
                color = AppColors.Error,
                fontSize = 12.sp
            )
        }
    }
}
