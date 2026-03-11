package com.obscura.wallpapers.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import com.obscura.wallpapers.ui.icons.ObscuraIcons
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.AppLanguage
import com.obscura.wallpapers.ui.theme.LocalAppResources
import androidx.compose.runtime.key
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
/**
 * 语言选择器组件
 * 显示当前选择的语言，并允许用户从下拉菜单中选择新的语言
 */
@Composable
fun LanguageSelector(
    currentLanguage: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用 key 参数确保当 currentLanguage 变化时，组件能够正确重组
    key(currentLanguage) {
        Log.d("LanguageSelector", "LanguageSelector recomposed with currentLanguage: $currentLanguage")
        var expanded by remember { mutableStateOf(false) }
        val resources = LocalAppResources.current

        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            onClick = { expanded = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
                ) {
                    Icon(
                        imageVector = ObscuraIcons.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Box(modifier = Modifier.weight(1f)) {
                    Column {
                        Text(
                            text = resources.getString(R.string.settings_language),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = getLanguageText(currentLanguage),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        AppLanguage.entries.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(getLanguageText(language)) },
                                onClick = {
                                    onLanguageSelected(language)
                                    expanded = false
                                },
                                trailingIcon = {
                                    val isSelected = language == currentLanguage
                                    Log.d("LanguageSelector", "Current language: $currentLanguage, Selected language: $language, isSelected: $isSelected")
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 获取语言显示文本
 * 使用资源文件中的文本，随系统语言变化
 */
@Composable
private fun getLanguageText(language: AppLanguage): String {
    val resources = LocalAppResources.current

    return when (language) {
        AppLanguage.SYSTEM -> resources.getString(R.string.settings_language_system)
        AppLanguage.ENGLISH -> resources.getString(R.string.settings_language_english)
        AppLanguage.JAPANESE -> resources.getString(R.string.settings_language_japanese)
        AppLanguage.KOREAN -> resources.getString(R.string.settings_language_korean)
        AppLanguage.CHINESE -> resources.getString(R.string.settings_language_chinese)
        AppLanguage.INDONESIAN -> resources.getString(R.string.settings_language_indonesian)
    }
}
