package com.obscura.wallpapers.core.common

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StringProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getString(@StringRes resId: Int): String {
        val currentLocale = getCurrentLocale()
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(currentLocale)
        } else {
            config.locale = currentLocale
        }
        val updatedContext = context.createConfigurationContext(config)
        return updatedContext.getString(resId)
    }

    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String {
        val currentLocale = getCurrentLocale()
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(currentLocale)
        } else {
            config.locale = currentLocale
        }
        val updatedContext = context.createConfigurationContext(config)
        return updatedContext.getString(resId, *formatArgs)
    }
    fun apiString(@StringRes resId: Int): String {
        println("apiString")
        return getString(resId)
    }
    fun apiString(@StringRes resId: Int, vararg formatArgs: Any): String {
        println("apiStringFmt")
        return getString(resId, *formatArgs)
    }
    fun apiCurrentLocale(): Locale {
        println("apiCurrentLocale")
        return getCurrentLocale()
    }

    private fun getCurrentLocale(): Locale {
        val currentLocale = AppCompatDelegate.getApplicationLocales()
        return if (currentLocale.isEmpty) {
            Locale.getDefault()
        } else {
            currentLocale[0] ?: Locale.getDefault()
        }
    }
}
