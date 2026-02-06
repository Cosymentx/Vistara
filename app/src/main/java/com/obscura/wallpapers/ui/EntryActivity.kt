package com.obscura.wallpapers.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.obscura.wallpapers.ObscuraApp
import com.obscura.wallpapers.core.data.model.AppLanguage
import com.obscura.wallpapers.di.ActivityScopeHolder
import com.obscura.wallpapers.manager.LocaleManager
import com.obscura.wallpapers.manager.ThemeManager
import com.obscura.wallpapers.ui.components.MainNavigation
import com.obscura.wallpapers.ui.theme.VistaraTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class EntryActivity : ComponentActivity() {

    private var pendingRoute: String? = null

    @Inject
    lateinit var themeManager: ThemeManager

    @Inject
    lateinit var localeManager: LocaleManager

    @Inject
    lateinit var activityScopeHolder: ActivityScopeHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val splashScreen = installSplashScreen()
        var contentReady = false
        splashScreen.setKeepOnScreenCondition { !contentReady }
        enableEdgeToEdge()
        applyIntentDestination(intent)
        bindUi()
        contentReady = true
    }

    override fun onStart() {
        super.onStart()
        activityScopeHolder.attach(this)
    }

    override fun onStop() {
        activityScopeHolder.attach(null)
        super.onStop()
    }

    private fun bindUi() {
        setContent {
            val language by localeManager.appLanguageFlow.collectAsState(initial = AppLanguage.SYSTEM)
            val darkTheme by themeManager.darkTheme()
            val dynamicColors by themeManager.dynamicColors()

            com.obscura.wallpapers.ui.theme.LocaleProvider(language = language) {
                VistaraTheme(darkTheme = darkTheme, dynamicColor = dynamicColors) {
                    val navController = rememberNavController()
                    var startDestination by rememberSaveable { mutableStateOf(pendingRoute) }

                    if (startDestination != null) {
                        LaunchedEffect(startDestination) {
                            navController.navigate(startDestination!!) { launchSingleTop = true }
                            startDestination = null
                            pendingRoute = null
                        }
                    }

                    MainNavigation(navController = navController)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyIntentDestination(intent)
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        val app = newBase.applicationContext as ObscuraApp
        val settings = kotlinx.coroutines.runBlocking { app.localeManager.appLanguageFlow.first() }
        val config = android.content.res.Configuration(newBase.resources.configuration)
        if (settings != AppLanguage.SYSTEM) {
            val locale = java.util.Locale(settings.code)
            java.util.Locale.setDefault(locale)
            config.setLocales(android.os.LocaleList(locale))
        } else {
            config.setLocales(android.os.LocaleList.getDefault())
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
        Log.d("EntryActivity", "attachBaseContext language: $settings")
    }

    private fun applyIntentDestination(intent: Intent) {
        val navigation = intent.getStringExtra("navigation")
        if (!navigation.isNullOrEmpty()) {
            pendingRoute = if (navigation != "settings") navigation else "settings"
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EntryActivityPreview() {
    VistaraTheme {
        MainNavigation()
    }
}
