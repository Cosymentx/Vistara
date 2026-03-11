package com.obscura.wallpapers.core.analytics

import android.content.Context
import android.util.Log
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib
import com.appsflyer.AFInAppEventParameterName
import com.appsflyer.AFInAppEventType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据统计管理类
 * 负责集成 AppsFlyer 等第三方统计 SDK
 */
@Singleton
class TrackingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "TrackingManager"
        private const val AF_DEV_KEY = "5jHYL4hXTPp7VHo3h8TwiX"
    }

    /**
     * 初始化 SDK
     */
    fun init() {
        val conversionListener = object : AppsFlyerConversionListener {
            override fun onConversionDataSuccess(data: MutableMap<String, Any>?) {
                Log.d(TAG, "onConversionDataSuccess: $data")
            }

            override fun onConversionDataFail(error: String?) {
                Log.e(TAG, "onConversionDataFail: $error")
            }

            override fun onAppOpenAttribution(data: MutableMap<String, String>?) {
                Log.d(TAG, "onAppOpenAttribution: $data")
            }

            override fun onAttributionFailure(error: String?) {
                Log.e(TAG, "onAttributionFailure: $error")
            }
        }

        AppsFlyerLib.getInstance().init(AF_DEV_KEY, conversionListener, context)
        AppsFlyerLib.getInstance().start(context)
        Log.d(TAG, "AppsFlyer initialized and started")
    }

    /**
     * 设置用户 ID
     */
    fun setCustomerUserId(userId: String) {
        AppsFlyerLib.getInstance().setCustomerUserId(userId)
    }

    /**
     * 追踪通用自定义事件
     */
    fun trackEvent(eventName: String, params: Map<String, Any>? = null) {
        AppsFlyerLib.getInstance().logEvent(context, eventName, params)
        Log.d(TAG, "Track Event: $eventName, Params: $params")
    }

    /**
     * 追踪登录/注册成功
     */
    fun trackLogin(method: String) {
        val params = mapOf<String, Any>(
            "af_registration_method" to method
        )
        trackEvent(AFInAppEventType.COMPLETE_REGISTRATION, params)
    }

    /**
     * 追踪订阅成功
     */
    fun trackSubscription(productId: String, price: Double, currency: String) {
        val params = mapOf<String, Any>(
            AFInAppEventParameterName.CONTENT_ID to productId,
            AFInAppEventParameterName.REVENUE to price,
            AFInAppEventParameterName.CURRENCY to currency
        )
        trackEvent(AFInAppEventType.SUBSCRIBE, params)
    }

    /**
     * 追踪金币购买成功
     */
    fun trackCoinPurchase(productId: String, amount: Int, price: Double, currency: String) {
        val params = mapOf<String, Any>(
            AFInAppEventParameterName.CONTENT_ID to productId,
            AFInAppEventParameterName.QUANTITY to amount,
            AFInAppEventParameterName.REVENUE to price,
            AFInAppEventParameterName.CURRENCY to currency
        )
        trackEvent(AFInAppEventType.PURCHASE, params)
    }

    /**
     * 追踪壁纸查看
     */
    fun trackWallpaperView(wallpaperId: String, category: String?) {
        val params = mapOf<String, Any>(
            AFInAppEventParameterName.CONTENT_ID to wallpaperId,
            AFInAppEventParameterName.CONTENT_TYPE to (category ?: "unknown")
        )
        trackEvent(AFInAppEventType.CONTENT_VIEW, params)
    }

    /**
     * 追踪收藏壁纸
     */
    fun trackFavorite(wallpaperId: String) {
        val params = mapOf<String, Any>(
            AFInAppEventParameterName.CONTENT_ID to wallpaperId
        )
        trackEvent(AFInAppEventType.ADD_TO_WISH_LIST, params)
    }
}
