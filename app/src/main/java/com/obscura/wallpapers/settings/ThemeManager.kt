package com.obscura.wallpapers.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import com.obscura.wallpapers.core.data.repository.UserPrefsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeManager @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository
) {
    private val darkThemeFlow: Flow<Boolean> = userPrefsRepository.getUserSettingsFlow().map { it.darkTheme }
    private val dynamicColorsFlow: Flow<Boolean> = userPrefsRepository.getUserSettingsFlow().map { it.dynamicColors }
    @Composable
    fun darkTheme(): State<Boolean> { return darkThemeFlow.collectAsState(initial = false) }
    @Composable
    fun dynamicColors(): State<Boolean> { return dynamicColorsFlow.collectAsState(initial = true) }
    fun isDarkTheme(): Flow<Boolean> { return darkThemeFlow }
    suspend fun updateDarkTheme(enabled: Boolean) {
        val currentSettings = userPrefsRepository.getUserSettings()
        val updatedSettings = currentSettings.copy(darkTheme = enabled)
        userPrefsRepository.saveUserSettings(updatedSettings)
    }
    suspend fun updateDynamicColors(enabled: Boolean) {
        val currentSettings = userPrefsRepository.getUserSettings()
        val updatedSettings = currentSettings.copy(dynamicColors = enabled)
        userPrefsRepository.saveUserSettings(updatedSettings)
    }
}
