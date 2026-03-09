package com.mosetian.passwordmanager.feature.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AppLockScreen(
    lockEnabled: Boolean,
    hasPassword: Boolean,
    onCreatePassword: (String) -> Unit,
    onUnlock: (String) -> Boolean,
    onResetAllData: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {}

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (lockEnabled && hasPassword) "输入主密码" else "设置主密码",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (lockEnabled && hasPassword) "解锁后才能查看你的凭据。" else "主密码用于保护应用访问，忘记后只能清空全部数据重置。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorText = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (lockEnabled && hasPassword) "主密码" else "设置主密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            if (!lockEnabled || !hasPassword) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorText = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("确认主密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
            errorText?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    if (lockEnabled && hasPassword) {
                        if (!onUnlock(password)) {
                            errorText = "主密码错误"
                        }
                    } else {
                        if (password.length < 4) {
                            errorText = "主密码至少 4 位"
                        } else if (password != confirmPassword) {
                            errorText = "两次输入的密码不一致"
                        } else {
                            onCreatePassword(password)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (lockEnabled && hasPassword) "解锁" else "保存并进入")
            }
            if (lockEnabled && hasPassword) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = { showResetConfirm = true }) {
                    Text("忘记密码？清空全部数据")
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    onResetAllData()
                }) {
                    Text("确认清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("取消")
                }
            },
            title = { Text("清空全部数据") },
            text = { Text("清空后将删除所有凭据、分组和主密码，且无法恢复。") }
        )
    }
}
