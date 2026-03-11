package com.obscura.wallpapers

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.obscura.wallpapers.core.analytics.TrackingManager
import com.obscura.wallpapers.core.data.repository.UserRepository
import com.obscura.wallpapers.settings.LocaleManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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

    @Inject
    lateinit var trackingManager: TrackingManager

    private val appScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        trackingManager.init()
        refreshUser()
        AppCompatDelegate.setApplicationLocales(AppCompatDelegate.getApplicationLocales())
    }

    private fun refreshUser() {
        appScope.launch {
            runCatching {
                userRepository.refreshUserProfile()
                val uid = userRepository.userUid.first()
                if (!uid.isNullOrEmpty()) {
                    trackingManager.setCustomerUserId(uid)
                }
                Log.d(TAG, "用户资料刷新完成, UID: $uid")
            }.getOrElse {
                Log.e(TAG, "刷新用户资料失败: ${it.message}")
            }
        }
    }
}
