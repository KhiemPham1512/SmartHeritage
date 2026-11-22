package com.example.smartheritage_mobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartheritage_mobile.config.AppStrings
import com.example.smartheritage_mobile.config.ServerAddress
import com.example.smartheritage_mobile.data.local.Session
import com.example.smartheritage_mobile.data.remote.RetrofitClient
import kotlinx.coroutines.launch

/** Cho phép nhập IP máy tính chạy backend để điện thoại thật kết nối qua Wi-Fi (không cần cáp USB / adb reverse). */
@Composable
fun ServerSettingsDialog(strings: AppStrings, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(Session.serverAddress) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusOk by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val normalized = ServerAddress.normalize(text)
    val invalid = text.isNotBlank() && normalized == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.serverDialogTitle, fontSize = 17.sp) },
        text = {
            Column {
                Text(strings.serverDialogHint, fontSize = 12.sp, color = Color(0xFF718096))
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        status = null
                    },
                    singleLine = true,
                    isError = invalid,
                    placeholder = { Text("192.168.1.5:8000", fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    supportingText = {
                        if (invalid) Text(strings.serverInvalid) else Text(normalized ?: strings.serverAuto)
                    }
                )
                status?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, fontSize = 12.sp, color = if (statusOk) Color(0xFF2E7D32) else Color(0xFFC62828))
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !invalid,
                onClick = {
                    Session.serverAddress = normalized ?: ""
                    onDismiss()
                }
            ) { Text(strings.serverSaveBtn) }
        },
        dismissButton = {
            Column {
                TextButton(
                    enabled = !invalid && !testing,
                    onClick = {
                        testing = true
                        status = null
                        scope.launch {
                            statusOk = RetrofitClient.ping(normalized)
                            status = if (statusOk) strings.serverOk else strings.serverFail
                            testing = false
                        }
                    }
                ) { Text(if (testing) "..." else strings.serverTestBtn) }
                TextButton(onClick = onDismiss) { Text(strings.cancelBtn) }
            }
        }
    )
}
