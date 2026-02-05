package com.obscura.wallpapers

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.obscura.wallpapers.core.data.repository.UserRepository
import com.obscura.wallpapers.manager.LocaleManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ObscuraApp : Application() {

    companion object {
        private const val AF_DEV_KEY = "pYBryM6RbSH6fTTYXAQ4xM"
        private const val TAG = "ObscuraApp"
    }

    @Inject
    lateinit var localeManager: LocaleManager

    @Inject
    lateinit var userRepository: UserRepository

    private val appScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        syncUserProfile()
        AppCompatDelegate.setApplicationLocales(AppCompatDelegate.getApplicationLocales())
        attachAttributionSdk()
    }

    private fun syncUserProfile() {
        appScope.launch {
            try {
                userRepository.refreshUserProfile()
                Log.d(TAG, "用户资料刷新完成")
            } catch (e: Exception) {
                Log.e(TAG, "刷新用户资料失败: ${e.message}", e)
            }
        }
    }

    private fun attachAttributionSdk() {
        val conversionDataListener = object : AppsFlyerConversionListener {
            override fun onAttributionFailure(error: String?) {
                Log.e(TAG, "归因失败: $error")
            }

            override fun onConversionDataFail(error: String?) {
                Log.e(TAG, "转化数据失败: $error")
            }

            override fun onAppOpenAttribution(data: MutableMap<String, String>?) {
                data?.let { attributionData ->
                    attributionData["deep_link_value"]?.let { resolveDeepLink(it) }
                    Log.d(TAG, "归因: $attributionData")
                }
            }

            override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {
                data?.let { conversionData ->
                    conversionData["campaign"]?.let { applyCampaign(it.toString()) }
                    val afStatus = conversionData["af_status"]
                    if (afStatus == "Organic") Log.d(TAG, "自然安装")
                    else if (afStatus == "Non-organic") Log.d(TAG, "来自广告的安装")
                    Log.d(TAG, "转化数据: $conversionData")
                }
            }
        }

        AppsFlyerLib.getInstance().apply {
            setCollectAndroidID(true)
            setDebugLog(BuildConfig.DEBUG)
            init(AF_DEV_KEY, conversionDataListener, this@ObscuraApp)
            start(this@ObscuraApp)
        }
    }

    private fun resolveDeepLink(deepLinkValue: String?) {
        Log.d(TAG, "深度链接: $deepLinkValue")
        when {
            deepLinkValue?.startsWith("premium/") == true -> {}
            deepLinkValue?.startsWith("category/") == true -> {}
            deepLinkValue?.startsWith("wallpaper/") == true -> {}
            else -> {}
        }
    }

    private fun applyCampaign(campaign: String) {
        Log.d(TAG, "Campaign: $campaign")
    }
}
