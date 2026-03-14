package com.lhzkml.codestudio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.BasicText
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

@Composable
internal fun TextField(
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
            .background(Colors.Surface, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                fontSize = 13.sp,
                color = Colors.OnSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                color = Colors.OnSurface,
                fontSize = 15.sp
            ),
            cursorBrush = SolidColor(Colors.Primary),
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (secure) PasswordVisualTransformation() else VisualTransformation.None,
            decorationBox = { innerTextField ->
                if (value.isEmpty() && placeholder.isNotBlank()) {
                    BasicText(
                        text = placeholder,
                        style = TextStyle(
                            fontSize = 15.sp,
                            color = Colors.OnSurfaceVariant
                        )
                    )
                }
                innerTextField()
            }
        )
        if (error != null) {
            Spacer(modifier = Modifier.height(4.dp))
            BasicText(
                text = error,
                style = TextStyle(
                    color = Colors.Error,
                    fontSize = 12.sp
                )
            )
        }
    }
}

