package com.paradox.finance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradox.finance.theme.*

@Composable
fun ParadoxCard(
    modifier: Modifier = Modifier,
    borderColor: Color = ParadoxZinc800,
    backgroundColor: Color = ParadoxZinc900,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun ParadoxGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ParadoxIndigo,
            disabledContainerColor = ParadoxZinc800
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = ParadoxZinc100,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@Composable
fun ParadoxTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    isError: Boolean = false
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    color = ParadoxZinc400
                ),
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium.copy(color = ParadoxZinc700)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            isError = isError,
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ParadoxZinc900,
                unfocusedContainerColor = ParadoxZinc900,
                disabledContainerColor = ParadoxZinc900,
                focusedBorderColor = ParadoxIndigo,
                unfocusedBorderColor = ParadoxZinc800,
                focusedTextColor = ParadoxZinc100,
                unfocusedTextColor = ParadoxZinc100
            )
        )
    }
}
