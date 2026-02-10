package com.obscura.wallpapers

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.obscura.wallpapers.core.data.repository.UserRepository
import com.obscura.wallpapers.settings.LocaleManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ObscuraApp : Application() {

    companion object {
        private const val TAG = "ObscuraApp"
    }

    @Inject
    lateinit var localeManager: LocaleManager

    @Inject
    lateinit var userRepository: UserRepository

    private val appScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        refreshUser()
        AppCompatDelegate.setApplicationLocales(AppCompatDelegate.getApplicationLocales())
    }

    private fun refreshUser() {
        appScope.launch {
            runCatching {
                userRepository.refreshUserProfile()
                Log.d(TAG, "用户资料刷新完成")
            }.getOrElse {
                Log.e(TAG, "刷新用户资料失败: ${it.message}")
            }
        }
    }
}
