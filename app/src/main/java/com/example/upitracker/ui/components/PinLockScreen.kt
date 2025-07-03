package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
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
import com.example.upitracker.util.BiometricHelper
import com.example.upitracker.util.PinStorage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinLockScreen(
    modifier: Modifier = Modifier,
    onUnlock: () -> Unit,
    onSetPin: () -> Unit,
    onAttemptBiometricUnlock: () -> Unit
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
    val biometricReady = remember { BiometricHelper.isBiometricReady(context) }

    LaunchedEffect(Unit, isSettingPinMode) {
        isPinActuallySet = PinStorage.isPinSet(context)
        if (!isPinActuallySet && !isSettingPinMode) {
            isSettingPinMode = true
        }
        if (!isSettingPinMode) {
            pinInput = ""
        }
        feedbackText = null
    }

    // ✨ NEW, CORRECTED LAYOUT STRUCTURE ✨
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding() // Pushes the entire layout up when keyboard appears
            .verticalScroll(rememberScrollState()) // Makes the screen scrollable
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // This column holds the icon and title
        Column(
            modifier = Modifier.offset(y = (-10).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isSettingPinMode || !isPinActuallySet) Icons.Filled.Lock else Icons.Filled.LockOpen,
                contentDescription = if (isSettingPinMode) "Set PIN" else "Unlock",
                modifier = Modifier.size(56.dp), // Slightly smaller icon
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))
        }

        // The content area for either setting a pin or unlocking
        Box(modifier = Modifier.fillMaxWidth()) {
            if (isSettingPinMode) {
                PinSetupContent(
                    isPinActuallySet = isPinActuallySet,
                    newPin = newPin,
                    onNewPinChange = { newPin = it },
                    confirmNewPin = confirmNewPin,
                    onConfirmNewPinChange = { confirmNewPin = it },
                    feedbackText = feedbackText,
                    isLoading = isLoading,
                    onSaveClick = {
                        val minPinLength = 4
                        if (newPin.length < minPinLength) {
                            feedbackText = context.getString(R.string.pin_lock_error_min_digits) to true; return@PinSetupContent
                        }
                        if (newPin != confirmNewPin) {
                            feedbackText = context.getString(R.string.pin_lock_error_mismatch) to true; return@PinSetupContent
                        }
                        isLoading = true; feedbackText = null
                        coroutineScope.launch {
                            PinStorage.savePin(context, newPin)
                            isLoading = false
                            onSetPin() // Call the callback to notify MainActivity
                        }
                    },
                    onCancelClick = { isSettingPinMode = false; feedbackText = null; newPin = ""; confirmNewPin = "" }
                )
            } else {
                UnlockContent(
                    pinInput = pinInput,
                    onPinInputChange = { pinInput = it },
                    feedbackText = feedbackText,
                    isLoading = isLoading,
                    biometricReady = biometricReady,
                    isPinActuallySet = isPinActuallySet,
                    onUnlockClick = {
                        isLoading = true; feedbackText = null
                        coroutineScope.launch {
                            if (PinStorage.checkPin(context, pinInput)) {
                                onUnlock()
                            } else {
                                feedbackText = context.getString(R.string.pin_lock_feedback_incorrect_pin) to true
                                isLoading = false
                            }
                        }
                    },
                    onAttemptBiometricUnlock = onAttemptBiometricUnlock,
                    onForgotPinClick = { isSettingPinMode = true; feedbackText = null; pinInput = "" }
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PinSetupContent(
    isPinActuallySet: Boolean,
    newPin: String,
    onNewPinChange: (String) -> Unit,
    confirmNewPin: String,
    onConfirmNewPinChange: (String) -> Unit,
    feedbackText: Pair<String, Boolean>?,
    isLoading: Boolean,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            if (isPinActuallySet) stringResource(R.string.pin_lock_change_pin_title) else stringResource(R.string.pin_lock_set_new_pin_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (isPinActuallySet) stringResource(R.string.pin_lock_change_pin_subtitle) else stringResource(R.string.pin_lock_set_new_pin_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = newPin,
            onValueChange = { if (it.length <= 6) onNewPinChange(it.filter(Char::isDigit)) },
            label = { Text(stringResource(R.string.pin_lock_new_pin_label)) },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = confirmNewPin,
            onValueChange = { if (it.length <= 6) onConfirmNewPinChange(it.filter(Char::isDigit)) },
            label = { Text(stringResource(R.string.pin_lock_confirm_new_pin_label)) },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
            isError = newPin.isNotEmpty() && confirmNewPin.isNotEmpty() && newPin != confirmNewPin
        )
        if (newPin.isNotEmpty() && confirmNewPin.isNotEmpty() && newPin != confirmNewPin) {
            Text(stringResource(R.string.pin_lock_error_mismatch), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onSaveClick,
            enabled = !isLoading && newPin.isNotBlank() && confirmNewPin.isNotBlank() && newPin == confirmNewPin && newPin.length >= 4,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
        ) {
            if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text(stringResource(R.string.pin_lock_save_new_pin_button))
        }

        feedbackText?.let { (message, isError) ->
            Text(
                text = message,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        if (isPinActuallySet) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = onCancelClick) {
                    Text(stringResource(R.string.pin_lock_cancel_change_button))
                }
            }
        }
    }
}

@Composable
private fun UnlockContent(
    pinInput: String,
    onPinInputChange: (String) -> Unit,
    feedbackText: Pair<String, Boolean>?,
    isLoading: Boolean,
    biometricReady: Boolean,
    isPinActuallySet: Boolean,
    onUnlockClick: () -> Unit,
    onAttemptBiometricUnlock: () -> Unit,
    onForgotPinClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.pin_lock_enter_pin_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = pinInput,
            onValueChange = { if (it.length <= 6) onPinInputChange(it.filter(Char::isDigit)) },
            label = { Text(stringResource(R.string.pin_lock_pin_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = feedbackText?.second == true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onUnlockClick,
                enabled = !isLoading && pinInput.isNotBlank() && pinInput.length >= 4,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                else Text(stringResource(R.string.pin_lock_unlock_button))
            }
            if (biometricReady && isPinActuallySet) {
                IconButton(
                    onClick = { if (!isLoading) onAttemptBiometricUnlock() },
                    enabled = !isLoading,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Filled.Fingerprint,
                        contentDescription = stringResource(R.string.biometric_icon_description),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Spacer(Modifier.size(56.dp))
            }
        }
        feedbackText?.let { (message, isError) ->
            Text(message, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(32.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButton(
                onClick = onForgotPinClick,
                enabled = !isLoading
            ) {
                Text(stringResource(if (isPinActuallySet) R.string.pin_lock_forgot_change_pin_button else R.string.pin_lock_set_initial_pin_button))
            }
        }
    }
}