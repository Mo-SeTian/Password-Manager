package com.mosetian.passwordmanager.feature.security

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun ChangeAppLockPasswordDialog(
    onDismiss: () -> Unit,
    onSubmit: (currentPassword: String, newPassword: String) -> String?
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                errorText = when {
                    currentPassword.isBlank() -> "请输入当前主密码"
                    newPassword.length < 4 -> "新主密码至少 4 位"
                    newPassword != confirmPassword -> "两次输入的新密码不一致"
                    else -> onSubmit(currentPassword, newPassword)
                }
            }) {
                Text("确认修改")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        title = { Text("修改主密码") },
        text = {
            androidx.compose.foundation.layout.Column {
                Text("修改主密码前，需要先校验当前主密码。")
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        errorText = null
                    },
                    label = { Text("当前主密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                        errorText = null
                    },
                    label = { Text("新主密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorText = null
                    },
                    label = { Text("确认新主密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                errorText?.let { Text(it) }
            }
        }
    )
}

@Composable
fun DisableAppLockDialog(
    onDismiss: () -> Unit,
    onSubmit: (currentPassword: String) -> String?
) {
    var currentPassword by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                errorText = if (currentPassword.isBlank()) {
                    "请输入当前主密码"
                } else {
                    onSubmit(currentPassword)
                }
            }) {
                Text("确认关闭")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        title = { Text("关闭应用锁") },
        text = {
            androidx.compose.foundation.layout.Column {
                Text("关闭应用锁前，需要先验证当前主密码。")
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                        errorText = null
                    },
                    label = { Text("当前主密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
                errorText?.let { Text(it) }
            }
        }
    )
}
