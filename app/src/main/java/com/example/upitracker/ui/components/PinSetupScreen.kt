package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
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
fun PinSetupScreen(
    modifier: Modifier = Modifier,
    onPinSet: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val pinMinDigitsError = stringResource(R.string.pin_setup_pin_must_be_4_digits)
    val pinsMismatchError = stringResource(R.string.pin_setup_pins_do_not_match)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp) // ✨ Consistent spacing
    ) {
        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.length <= 6) pin = it.filter { char -> char.isDigit() }
                errorText = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.pin_setup_new_pin_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            isError = errorText != null && (errorText == pinMinDigitsError || errorText == pinsMismatchError)
        )

        OutlinedTextField(
            value = confirmPin,
            onValueChange = {
                if (it.length <= 6) confirmPin = it.filter { char -> char.isDigit() }
                errorText = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.pin_setup_confirm_pin_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            leadingIcon = { Icon(Icons.Filled.Password, contentDescription = null) },
            isError = errorText != null && errorText == pinsMismatchError
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onCancel != null) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(stringResource(R.string.dialog_button_cancel))
                }
            }
            Button(
                onClick = {
                    when {
                        pin.length < 4 -> errorText = pinMinDigitsError
                        pin != confirmPin -> errorText = pinsMismatchError
                        else -> {
                            isLoading = true
                            errorText = null
                            scope.launch {
                                PinStorage.savePin(context, pin)
                                isLoading = false
                                onPinSet()
                            }
                        }
                    }
                },
                enabled = !isLoading && pin.isNotBlank() && confirmPin.isNotBlank() && pin.length >= 4 && pin == confirmPin,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.pin_setup_save_pin))
                }
            }
        }
    }
}