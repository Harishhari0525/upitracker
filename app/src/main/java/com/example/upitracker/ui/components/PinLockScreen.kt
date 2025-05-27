package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource // ✨
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.upitracker.R // ✨
import com.example.upitracker.util.PinStorage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinLockScreen(
    modifier: Modifier = Modifier,
    onUnlock: () -> Unit,
    onSetPin: () -> Unit
) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var feedbackText by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var isSettingPinMode by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    var isPinActuallySet by remember { mutableStateOf(false) }
    LaunchedEffect(Unit, isSettingPinMode) {
        isPinActuallySet = PinStorage.isPinSet(context)
        if (!isPinActuallySet && !isSettingPinMode) {
            isSettingPinMode = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isSettingPinMode || !isPinActuallySet) Icons.Filled.Lock else Icons.Filled.LockOpen,
            contentDescription = if (isSettingPinMode) stringResource(R.string.pin_lock_set_pin_desc) else stringResource(R.string.pin_lock_unlock_desc), // ✨
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))

        if (isSettingPinMode) {
            Text(
                if (isPinActuallySet) stringResource(R.string.pin_lock_change_pin_title) else stringResource(R.string.pin_lock_set_new_pin_title), // ✨
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (isPinActuallySet) stringResource(R.string.pin_lock_change_pin_subtitle) else stringResource(R.string.pin_lock_set_new_pin_subtitle), // ✨
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = newPin,
                onValueChange = { if (it.length <= 6) newPin = it.filter { char -> char.isDigit() } },
                label = { Text(stringResource(R.string.pin_lock_new_pin_label)) }, // ✨
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = confirmNewPin,
                onValueChange = { if (it.length <= 6) confirmNewPin = it.filter { char -> char.isDigit() } },
                label = { Text(stringResource(R.string.pin_lock_confirm_new_pin_label)) }, // ✨
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
                isError = newPin.isNotEmpty() && confirmNewPin.isNotEmpty() && newPin != confirmNewPin
            )
            if (newPin.isNotEmpty() && confirmNewPin.isNotEmpty() && newPin != confirmNewPin) {
                Text(stringResource(R.string.pin_lock_error_mismatch), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) // ✨
            }
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (newPin.length < 4) {
                        feedbackText = context.getString(R.string.pin_lock_error_min_digits) to true; return@Button // ✨
                    }
                    if (newPin != confirmNewPin) {
                        feedbackText = context.getString(R.string.pin_lock_error_mismatch) to true; return@Button // ✨
                    }
                    isLoading = true; feedbackText = null
                    coroutineScope.launch {
                        PinStorage.savePin(context, newPin)
                        feedbackText = context.getString(R.string.pin_lock_feedback_pin_updated) to false // ✨
                        isPinActuallySet = true
                        isSettingPinMode = false
                        pinInput = ""; newPin = ""; confirmNewPin = ""
                        isLoading = false
                        onSetPin()
                    }
                },
                enabled = !isLoading && newPin.isNotBlank() && confirmNewPin.isNotBlank() && newPin == confirmNewPin && newPin.length >=4,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text(stringResource(R.string.pin_lock_save_new_pin_button)) // ✨
            }
            feedbackText?.let { (message, _) -> // Error color is handled by isError flag
                Text(
                    message, // message is already resolved string
                    color = if (feedbackText!!.second) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            if (isPinActuallySet) {
                TextButton(onClick = { isSettingPinMode = false; feedbackText = null; newPin = ""; confirmNewPin = "" }) {
                    Text(stringResource(R.string.pin_lock_cancel_change_button)) // ✨
                }
            }

        } else { // Unlock Mode
            Text(
                stringResource(R.string.pin_lock_enter_pin_title), // ✨
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = pinInput,
                onValueChange = { if (it.length <= 6) pinInput = it.filter {char -> char.isDigit()}; feedbackText = null },
                label = { Text(stringResource(R.string.pin_lock_pin_label)) }, // ✨
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = feedbackText?.second == true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    isLoading = true; feedbackText = null
                    coroutineScope.launch {
                        if (PinStorage.checkPin(context, pinInput)) {
                            onUnlock()
                        } else {
                            feedbackText = context.getString(R.string.pin_lock_feedback_incorrect_pin) to true // ✨
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading && pinInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text(stringResource(R.string.pin_lock_unlock_button)) // ✨
            }
            feedbackText?.let { (message, isError) ->
                Text(
                    message, // message is already resolved string
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(32.dp))
            TextButton(
                onClick = { isSettingPinMode = true; feedbackText = null; pinInput = "" },
                enabled = !isLoading
            ) {
                Text(if (isPinActuallySet) stringResource(R.string.pin_lock_forgot_change_pin_button) else stringResource(R.string.pin_lock_set_initial_pin_button)) // ✨
            }
        }
    }
}