package com.obscura.wallpapers.settings

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.obscura.wallpapers.core.data.model.AppLanguage
import com.obscura.wallpapers.core.data.repository.UserPrefsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocaleManager @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository
) {
    companion object { private const val TAG = "LocaleManager" }
    val appLanguageFlow: Flow<AppLanguage> = userPrefsRepository.getUserSettingsFlow().map { it.appLanguage }
    suspend fun updateAppLanguage(language: AppLanguage) {
        val currentSettings = userPrefsRepository.getUserSettings()
        val updatedSettings = currentSettings.copy(appLanguage = language)
        userPrefsRepository.saveUserSettings(updatedSettings)
        applyLanguage(language)
    }
    fun applyLanguage(language: AppLanguage): Boolean {
        val currentLocale = AppCompatDelegate.getApplicationLocales()
        val settings = runBlocking { userPrefsRepository.getUserSettings() }
        val savedLanguage = settings.appLanguage
        Log.d(TAG, "当前语言设置: $savedLanguage")
        Log.d(TAG, "当前应用语言: $currentLocale")
        try {
            if (language == AppLanguage.SYSTEM) {
                Log.d(TAG, "Setting to SYSTEM language")
                forceSystemLanguage()
            } else {
                val locale = Locale(language.code)
                Locale.setDefault(locale)
                val localeList = LocaleListCompat.create(locale)
                AppCompatDelegate.setApplicationLocales(localeList)
                try {
                    val config = Resources.getSystem().configuration
                    val localeList2 = android.os.LocaleList(locale)
                    config.setLocales(localeList2)
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting locale: ${e.message}")
                }
            }
            val afterLocale = AppCompatDelegate.getApplicationLocales()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error applying language: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
    suspend fun applyCurrentLanguage() {
        val settings = userPrefsRepository.getUserSettings()
        val language = settings.appLanguage
        val currentLocale = AppCompatDelegate.getApplicationLocales()
        try {
            if (language == AppLanguage.SYSTEM) {
            } else {
                val locale = Locale(language.code)
                val localeList = LocaleListCompat.create(locale)
                AppCompatDelegate.setApplicationLocales(localeList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying current language: ${e.message}")
            e.printStackTrace()
        }
    }
    fun getSystemLanguage(context: Context): String { return getSystemLocale().language }
    fun forceSystemLanguage() {
        Log.d(TAG, "强制重置应用语言到系统语言")
        try {
            val systemLocale = getSystemLocale()
            Log.d(TAG, "获取到系统语言: $systemLocale")
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            Log.d(TAG, "设置空的语言列表，使应用使用系统语言")
            val afterLocales = AppCompatDelegate.getApplicationLocales()
            Log.d(TAG, "设置后的应用语言: $afterLocales, isEmpty: ${afterLocales.isEmpty}")
        } catch (e: Exception) {
            Log.e(TAG, "强制设置系统语言时出错: ${e.message}")
        }
    }
    fun getSystemLocale(): Locale {
        val configuration = Resources.getSystem().configuration
        val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales.get(0)
        } else { @Suppress("DEPRECATION") configuration.locale }
        Log.d(TAG, "从 Resources.getSystem() 获取的系统语言: $systemLocale")
        return systemLocale
    }
    fun createConfigurationContext(baseContext: Context, language: AppLanguage): Context {
        if (language == AppLanguage.SYSTEM) { return baseContext }
        val locale = Locale.Builder().setLanguage(language.code).build()
        Locale.setDefault(locale)
        val config = Configuration(baseContext.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { config.setLocale(locale) } else { @Suppress("DEPRECATION") config.locale = locale }
        return baseContext.createConfigurationContext(config)
    }
}
