package com.example.upitracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PinLockScreen(onUnlock: () -> Unit) {
    var pinInput by remember { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Enter PIN to unlock")
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = pinInput,
            onValueChange = { pinInput = it },
            label = { Text("PIN") }
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = { if (pinInput == "1234") onUnlock() }) { Text("Unlock") }
    }
}
