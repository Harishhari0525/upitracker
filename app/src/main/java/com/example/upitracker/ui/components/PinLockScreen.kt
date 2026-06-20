package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.example.upitracker.util.BiometricHelper
import com.example.upitracker.util.ExpressiveTokens
import com.example.upitracker.util.PinStorage
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinLockScreen(
    modifier: Modifier = Modifier,
    onUnlock: () -> Unit,
    onSetPin: () -> Unit,
    onAttemptBiometricUnlock: () -> Unit
) {
    val context = LocalContext.current

    val minDigitsErrorText = stringResource(R.string.pin_lock_error_min_digits)
    val mismatchErrorText = stringResource(R.string.pin_lock_error_mismatch)
    val incorrectPinText = stringResource(R.string.pin_lock_feedback_incorrect_pin)

    var pinInput by remember { mutableStateOf("") }
    var feedbackText by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var isSettingPinMode by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var lockoutTimeLeft by remember { mutableLongStateOf(0L) }

    var isPinActuallySet by remember { mutableStateOf(false) }
    val biometricReady = remember { BiometricHelper.isBiometricReady(context) }

    LaunchedEffect(isPinActuallySet, lockoutTimeLeft) {
        if (lockoutTimeLeft == 0L) {
            val lockoutUntil = PinStorage.getPinLockoutUntil(context)
            val now = System.currentTimeMillis()
            if (lockoutUntil > now) {
                lockoutTimeLeft = (lockoutUntil - now) / 1000 + 1
            }
        }
    }

    LaunchedEffect(lockoutTimeLeft) {
        if (lockoutTimeLeft > 0) {
            kotlinx.coroutines.delay(1000.milliseconds)
            lockoutTimeLeft -= 1
        }
    }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ExpressiveTokens.spacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.offset(y = (-10).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isSettingPinMode || !isPinActuallySet) {
                    Icons.Filled.Lock
                } else {
                    Icons.Filled.LockOpen
                },
                contentDescription = if (isSettingPinMode) "Set PIN" else "Unlock",
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.lg))
        }

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
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
                            feedbackText = minDigitsErrorText to true
                            return@PinSetupContent
                        }

                        if (newPin != confirmNewPin) {
                            feedbackText = mismatchErrorText to true
                            return@PinSetupContent
                        }

                        isLoading = true
                        feedbackText = null

                        coroutineScope.launch {
                            PinStorage.savePin(context, newPin)
                            isLoading = false
                            onSetPin()
                        }
                    },
                    onCancelClick = {
                        isSettingPinMode = false
                        feedbackText = null
                        newPin = ""
                        confirmNewPin = ""
                    }
                )
            } else {
                val lockoutMessage = stringResource(R.string.pin_lock_feedback_locked_out, lockoutTimeLeft)
                UnlockContent(
                    pinInput = pinInput,
                    onPinInputChange = { pinInput = it },
                    feedbackText = if (lockoutTimeLeft > 0) {
                        lockoutMessage to true
                    } else {
                        feedbackText
                    },
                    isLoading = isLoading,
                    isLockedOut = lockoutTimeLeft > 0,
                    biometricReady = biometricReady,
                    isPinActuallySet = isPinActuallySet,
                    onUnlockClick = {
                        isLoading = true
                        feedbackText = null

                        coroutineScope.launch {
                            if (PinStorage.checkPin(context, pinInput)) {
                                onUnlock()
                            } else {
                                val lockoutUntil = PinStorage.getPinLockoutUntil(context)
                                val now = System.currentTimeMillis()
                                if (lockoutUntil > now) {
                                    lockoutTimeLeft = (lockoutUntil - now) / 1000 + 1
                                } else {
                                    feedbackText = incorrectPinText to true
                                }
                                isLoading = false
                            }
                        }
                    },
                    onAttemptBiometricUnlock = {
                        if (lockoutTimeLeft == 0L) {
                            onAttemptBiometricUnlock()
                        }
                    },
                    onForgotPinClick = {
                        if (lockoutTimeLeft == 0L) {
                            isSettingPinMode = true
                            feedbackText = null
                            pinInput = ""
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.xl))
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
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (isPinActuallySet) {
                stringResource(R.string.pin_lock_change_pin_title)
            } else {
                stringResource(R.string.pin_lock_set_new_pin_title)
            },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.sm))

        Text(
            text = if (isPinActuallySet) {
                stringResource(R.string.pin_lock_change_pin_subtitle)
            } else {
                stringResource(R.string.pin_lock_set_new_pin_subtitle)
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.lg))

        OutlinedTextField(
            value = newPin,
            onValueChange = {
                if (it.length <= 6) {
                    onNewPinChange(it.filter(Char::isDigit))
                }
            },
            label = { Text(stringResource(R.string.pin_lock_new_pin_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = ExpressiveTokens.corners.medium
        )

        Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.md))

        OutlinedTextField(
            value = confirmNewPin,
            onValueChange = {
                if (it.length <= 6) {
                    onConfirmNewPinChange(it.filter(Char::isDigit))
                }
            },
            label = { Text(stringResource(R.string.pin_lock_confirm_new_pin_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword
            ),
            modifier = Modifier.fillMaxWidth(),
            isError = newPin.isNotEmpty() &&
                    confirmNewPin.isNotEmpty() &&
                    newPin != confirmNewPin,
            shape = ExpressiveTokens.corners.medium
        )

        if (newPin.isNotEmpty() && confirmNewPin.isNotEmpty() && newPin != confirmNewPin) {
            Text(
                text = stringResource(R.string.pin_lock_error_mismatch),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.lg))

        Button(
            onClick = onSaveClick,
            enabled = !isLoading &&
                    newPin.isNotBlank() &&
                    confirmNewPin.isNotBlank() &&
                    newPin == confirmNewPin &&
                    newPin.length >= 4,
            modifier = Modifier
                .fillMaxWidth()
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
                Text(stringResource(R.string.pin_lock_save_new_pin_button))
            }
        }

        feedbackText?.let { (message, isError) ->
            Text(
                text = message,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = ExpressiveTokens.spacing.sm)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        if (isPinActuallySet) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
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
    isLockedOut: Boolean,
    biometricReady: Boolean,
    isPinActuallySet: Boolean,
    onUnlockClick: () -> Unit,
    onAttemptBiometricUnlock: () -> Unit,
    onForgotPinClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.pin_lock_enter_pin_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.lg))

        OutlinedTextField(
            value = pinInput,
            onValueChange = {
                if (it.length <= 6) {
                    onPinInputChange(it.filter(Char::isDigit))
                }
            },
            label = { Text(stringResource(R.string.pin_lock_pin_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword
            ),
            isError = feedbackText?.second == true,
            enabled = !isLoading && !isLockedOut,
            modifier = Modifier.fillMaxWidth(),
            shape = ExpressiveTokens.corners.medium
        )

        Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ExpressiveTokens.spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onUnlockClick,
                enabled = !isLoading &&
                        !isLockedOut &&
                        pinInput.isNotBlank() &&
                        pinInput.length >= 4,
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
                    Text(stringResource(R.string.pin_lock_unlock_button))
                }
            }

            if (biometricReady && isPinActuallySet) {
                IconButton(
                    onClick = {
                        if (!isLoading && !isLockedOut) {
                            onAttemptBiometricUnlock()
                        }
                    },
                    enabled = !isLoading && !isLockedOut,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Fingerprint,
                        contentDescription = stringResource(R.string.biometric_icon_description),
                        modifier = Modifier.size(28.dp),
                        tint = if (isLockedOut) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }

        feedbackText?.let { (message, isError) ->
            Text(
                text = message,
                color = if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = ExpressiveTokens.spacing.md)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(ExpressiveTokens.spacing.xl))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            TextButton(
                onClick = onForgotPinClick,
                enabled = !isLoading && !isLockedOut
            ) {
                Text(
                    stringResource(
                        if (isPinActuallySet) {
                            R.string.pin_lock_forgot_change_pin_button
                        } else {
                            R.string.pin_lock_set_initial_pin_button
                        }
                    )
                )
            }
        }
    }
}