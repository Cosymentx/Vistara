package com.obscura.wallpapers.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/**
 * 文本输入对话框 - 升级为全屏沉浸式毛玻璃效果
 */
@Composable
fun TextInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    title: String,
    label: String,
    placeholder: String = "",
    initialValue: String = "",
    confirmText: String,
    dismissText: String,
    isPassword: Boolean = false,
    isLoading: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    properties: DialogProperties = DialogProperties(
        usePlatformDefaultWidth = false
    ),
) {
    var inputText by remember { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Dialog(onDismissRequest = onDismiss, properties = properties) {
        val view = LocalView.current
        // 强制对话框 Window 沉浸式，消除状态栏灰色条
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window
            window?.let {
                WindowCompat.setDecorFitsSystemWindows(it, false)
                it.statusBarColor = android.graphics.Color.TRANSPARENT
                it.navigationBarColor = android.graphics.Color.TRANSPARENT
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        focusManager.clearFocus()
                        onDismiss()
                    }), contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(28.dp))
                    .border(
                        BorderStroke(
                            1.dp, Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.05f)
                                )
                            )
                        ), shape = RoundedCornerShape(28.dp)
                    )
                    .clickable(enabled = false) { }) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(
                        thickness = 0.5.dp, color = textColor.copy(alpha = 0.1f)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text(label, color = textColor.copy(alpha = 0.6f)) },
                        placeholder = { Text(placeholder, color = textColor.copy(alpha = 0.3f)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = textColor.copy(alpha = 0.2f),
                            cursorColor = MaterialTheme.colorScheme.primary
                        ),
                        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = keyboardType, imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (inputText.isNotEmpty()) {
                                    onConfirm(inputText)
                                }
                            })
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                focusManager.clearFocus()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, textColor.copy(alpha = 0.12f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = textColor
                            ),
                            modifier = Modifier
                                .height(50.dp)
                                .weight(1f)
                        ) {
                            Text(
                                text = dismissText, style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                onConfirm(inputText)
                            },
                            enabled = !isLoading && inputText.isNotEmpty(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.5f
                                )
                            ),
                            modifier = Modifier
                                .height(50.dp)
                                .weight(1f)
                        ) {
                            Text(
                                text = confirmText, style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
