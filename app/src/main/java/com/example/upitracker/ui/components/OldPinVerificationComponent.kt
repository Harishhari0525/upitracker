package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.upitracker.R
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.util.PinStorage
import kotlinx.coroutines.launch

@Composable
fun OldPinVerificationComponent(
    modifier: Modifier = Modifier,
    onOldPinVerified: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val incorrectCurrentPinText = stringResource(
        R.string.pin_change_error_incorrect_current_pin
    )

    var currentPinInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.md)
    ) {
        Text(
            text = stringResource(R.string.pin_change_enter_current_pin_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = currentPinInput,
            onValueChange = {
                if (it.length <= 6) {
                    currentPinInput = it.filter { char -> char.isDigit() }
                }
                errorText = null
            },
            label = {
                Text(stringResource(R.string.pin_change_current_pin_label))
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword
            ),
            isError = errorText != null,
            modifier = Modifier.fillMaxWidth(),
            shape = ExpressiveTokens.corners.medium
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
            Spacer(modifier = Modifier.size(2.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.sm)
        ) {
            OutlinedButton(
                onClick = onCancel,
                enabled = !isLoading,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp),
                shape = ExpressiveTokens.corners.medium
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
                            errorText = incorrectCurrentPinText
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading &&
                        currentPinInput.isNotBlank() &&
                        currentPinInput.length >= 4,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp),
                shape = ExpressiveTokens.corners.medium
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.pin_change_verify_button))
                }
            }
        }
    }
}