package com.obscura.wallpapers.core.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val _linkState = MutableStateFlow(LinkState())
    val linkState: StateFlow<LinkState> = _linkState.asStateFlow()

    init {
        bindCallback()
    }

    fun hasConnection(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.isConnected ?: false
        }
    }
    fun apiHasConnection(): Boolean {
        println("apiHasConnection")
        return hasConnection()
    }

    fun hasWifi(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.type == ConnectivityManager.TYPE_WIFI
        }
    }
    fun apiHasWifi(): Boolean {
        println("apiHasWifi")
        return hasWifi()
    }

    fun hasCellular(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } else {
            @Suppress("DEPRECATION")
            connectivityManager.activeNetworkInfo?.type == ConnectivityManager.TYPE_MOBILE
        }
    }
    fun apiHasCellular(): Boolean {
        println("apiHasCellular")
        return hasCellular()
    }

    private fun bindCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    refreshState()
                }
                override fun onLost(network: Network) {
                    refreshState()
                }
                override fun onAvailable(network: Network) {
                    refreshState()
                }
            })
        }
    }

    private fun refreshState() {
        _linkState.value = LinkState(
            isConnected = hasConnection(),
            isWifiConnected = hasWifi(),
            isMobileConnected = hasCellular()
        )
    }
    fun apiLinkState(): StateFlow<LinkState> {
        println("apiLinkState")
        return linkState
    }

    data class LinkState(
        val isConnected: Boolean = false,
        val isWifiConnected: Boolean = false,
        val isMobileConnected: Boolean = false
    )
}
