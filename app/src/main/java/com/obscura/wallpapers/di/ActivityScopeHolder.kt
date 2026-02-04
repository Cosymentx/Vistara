package com.obscura.wallpapers.di

import android.app.Activity
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 当前前台 Activity 作用域持有者，由 DI 提供。
 * EntryActivity 在 onStart 时绑定、onStop 时解绑；支付/设壁纸等处通过 current() 获取实例。
 */
@Singleton
class ActivityScopeHolder @Inject constructor() {
    private var hostRef: WeakReference<Activity>? = null

    fun attach(activity: Activity?) {
        hostRef = activity?.let { WeakReference(it) }
    }

    fun current(): Activity? = hostRef?.get()
}
