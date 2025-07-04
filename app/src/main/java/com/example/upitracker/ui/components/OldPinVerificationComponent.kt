package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.util.PinStorage
import kotlinx.coroutines.launch

@Composable
fun OldPinVerificationComponent(
    modifier: Modifier = Modifier,
    onOldPinVerified: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var currentPinInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp) // ✨ Consistent spacing
    ) {
        Text(
            text = stringResource(R.string.pin_change_enter_current_pin_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = currentPinInput,
            onValueChange = {
                if (it.length <= 6) currentPinInput = it.filter { char -> char.isDigit() }
                errorText = null
            },
            label = { Text(stringResource(R.string.pin_change_current_pin_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = errorText != null,
            modifier = Modifier.fillMaxWidth()
        )

        if (errorText != null) {
            Text(
                text = errorText!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Spacer(Modifier.height(2.dp)) // Maintain space when no error
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                enabled = !isLoading,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                Text(stringResource(R.string.dialog_button_cancel))
            }
            Button(
                onClick = {
                    isLoading = true
                    errorText = null
                    scope.launch {
                        if (PinStorage.checkPin(context, currentPinInput)) {
                            onOldPinVerified()
                        } else {
                            errorText = context.getString(R.string.pin_change_error_incorrect_current_pin)
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading && currentPinInput.isNotBlank() && currentPinInput.length >= 4,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text(stringResource(R.string.pin_change_verify_button))
            }
        }
    }
}