package com.obscura.wallpapers.features.about

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.common.AppConstants.PRIVACY_POLICY_URL
import com.obscura.wallpapers.core.common.AppConstants.TERMS_OF_SERVICE_URL
import com.obscura.wallpapers.core.common.AppConstants.USER_AGREEMENT_URL
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URLEncoder
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "AboutViewModel"

    }

    private val _appVersion = MutableStateFlow(getAppVersion())
    val appVersion: StateFlow<String> = _appVersion.asStateFlow()

    private val _openSourceLibraries = MutableStateFlow(getOpenSourceLibraries())
    val openSourceLibraries: StateFlow<List<Library>> = _openSourceLibraries.asStateFlow()

    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "Ver ${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting app version: ${e.message}")
            "Ver 1.0.0"
        }
    }

    private fun getOpenSourceLibraries(): List<Library> {
        return listOf(
            Library(
                name = "Jetpack Compose",
                description = "Modern UI toolkit for Android",
                url = "https://developer.android.com/jetpack/compose"
            ),
            Library(
                name = "Kotlin Coroutines",
                description = "Asynchronous programming library for Kotlin",
                url = "https://github.com/Kotlin/kotlinx.coroutines"
            ),
            Library(
                name = "Hilt",
                description = "Dependency injection library for Android",
                url = "https://dagger.dev/hilt/"
            ),
            Library(
                name = "Coil",
                description = "Image loading library for Kotlin",
                url = "https://github.com/coil-kt/coil"
            ),
            Library(
                name = "Retrofit",
                description = "Type-safe HTTP client for Android and Java",
                url = "https://square.github.io/retrofit/"
            ),
            Library(
                name = "OkHttp",
                description = "HTTP client for Android and Java",
                url = "https://square.github.io/okhttp/"
            ),
            Library(
                name = "Room",
                description = "SQLite object mapping library for Android",
                url = "https://developer.android.com/training/data-storage/room"
            ),
            Library(
                name = "ExoPlayer",
                description = "Media player for Android",
                url = "https://github.com/google/ExoPlayer"
            )
        )
    }

    private var navController: NavController? = null

    fun setNavController(controller: NavController) {
        navController = controller
    }

    fun openPrivacyPolicy() {
        openInWebView(PRIVACY_POLICY_URL, context.getString(R.string.privacy_policy))
    }

    fun openTermsOfService() {
        openInWebView(TERMS_OF_SERVICE_URL, context.getString(R.string.terms_of_service))
    }

    fun openUserAgreement() {
        openInWebView(USER_AGREEMENT_URL, context.getString(R.string.user_agreement))
    }

    private fun openInWebView(url: String, title: String) {
        try {
            navController?.let { nav ->
                val encodedUrl = URLEncoder.encode(url, "UTF-8")
                val encodedTitle = URLEncoder.encode(title, "UTF-8")
                val route = "webview?url=$encodedUrl"
                Log.d(TAG, "Navigating to WebView with route: $route")
                nav.navigate(route)
            } ?: run {
                Log.d(TAG, "NavController is null, opening URL in external browser: $url")
                openExternalUrl(url)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to WebView: $url, ${e.message}")
            openExternalUrl(url)
        }
    }

    private fun openExternalUrl(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error opening URL in external browser: $url, ${e.message}")
            false
        }
    }

    fun openLibraryUrl(url: String) {
        openInWebView(url, context.getString(R.string.open_source_libraries))
    }
}

data class Library(
    val name: String,
    val description: String,
    val url: String
)
