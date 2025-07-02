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
import androidx.compose.ui.unit.dp

import com.example.upitracker.R // Ensure this is imported
import com.example.upitracker.util.PinStorage
import kotlinx.coroutines.launch

@Composable
fun PinSetupScreen(
    modifier: Modifier = Modifier,
    onPinSet: () -> Unit,
    onCancel: (() -> Unit)? = null // ✨ Parameter name changed to onCancel ✨
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
            .fillMaxSize() // Fills the dialog space
            .verticalScroll(rememberScrollState()) // Makes the whole area scrollable if needed
            .padding(horizontal = 8.dp, vertical = 16.dp)
            .imePadding(),
    ) {
        Column {
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= 6) pin = it.filter { char -> char.isDigit() }
                    errorText = null // Clear error on input change
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.pin_setup_new_pin_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                leadingIcon = {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = stringResource(R.string.pin_setup_new_pin_label)
                    )
                },
                isError = errorText != null && (errorText == pinMinDigitsError || errorText == pinsMismatchError)
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPin,
                onValueChange = {
                    if (it.length <= 6) confirmPin = it.filter { char -> char.isDigit() }
                    errorText = null // Clear error on input change
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.pin_setup_confirm_pin_label)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                leadingIcon = {
                    Icon(
                        Icons.Filled.Password,
                        contentDescription = stringResource(R.string.pin_setup_confirm_pin_label)
                    )
                },
                isError = errorText != null && errorText == pinsMismatchError
            )
            Spacer(Modifier.weight(1f))
            Column {
                errorText?.let {
                    Text(
                        text = it, // errorText is already a resolved string (from resources)
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (onCancel != null) Arrangement.SpaceBetween else Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onCancel != null) {
                        OutlinedButton(
                            onClick = onCancel, // Use the onCancel lambda
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Text(stringResource(R.string.dialog_button_cancel))
                        }
                        Spacer(Modifier.width(12.dp))
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
                        modifier = if (onCancel != null) Modifier.weight(1f)
                            .heightIn(min = 48.dp) else Modifier.heightIn(min = 48.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(stringResource(R.string.pin_setup_save_pin))
                        }
                    }
                }
            }
        }
    }
}