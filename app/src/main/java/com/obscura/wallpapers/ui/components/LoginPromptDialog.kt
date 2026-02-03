package com.obscura.wallpapers.ui.components

import androidx.compose.runtime.Composable
import com.obscura.wallpapers.ui.theme.stringResource
import com.obscura.wallpapers.R

/**
 * 登录提示对话框
 * 用于在需要登录时提示用户
 *
 * @param onDismiss 取消回调
 * @param onConfirm 确认回调，用户点击"去登录"按钮时触发
 * @param title 对话框标题
 * @param message 对话框消息
 */
@Composable
fun LoginPromptDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    title: String = stringResource(R.string.login_required),
    message: String = stringResource(R.string.login_required_message)
) {
    ConfirmDialog(
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        title = title,
        message = message,
        confirmText = stringResource(R.string.go_to_login),
        dismissText = stringResource(R.string.cancel)
    )
}
