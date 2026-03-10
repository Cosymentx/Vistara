package com.obscura.wallpapers.ui.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.obscura.wallpapers.ui.icons.ObscuraIcons
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.obscura.wallpapers.R
import com.obscura.wallpapers.core.data.model.BannerActionType
import com.obscura.wallpapers.features.billing.CoinStoreScreen
import com.obscura.wallpapers.features.billing.SubscriptionScreen
import com.obscura.wallpapers.features.browser.BrowserScreen
import com.obscura.wallpapers.features.cycler.WallpaperCyclerScreen
import com.obscura.wallpapers.features.discover.DiscoverScreen
import com.obscura.wallpapers.features.editor.WallpaperEditScreen
import com.obscura.wallpapers.features.info.InfoScreen
import com.obscura.wallpapers.features.library.LibraryScreen
import com.obscura.wallpapers.features.likes.LikesScreen
import com.obscura.wallpapers.features.photo.PhotoLibraryScreen
import com.obscura.wallpapers.features.preferences.PreferencesScreen
import com.obscura.wallpapers.features.preview.WallpaperPreviewScreen
import com.obscura.wallpapers.features.profile.ProfileScreen
import com.obscura.wallpapers.features.search.SearchScreen
import com.obscura.wallpapers.features.signin.SignInScreen
import com.obscura.wallpapers.features.support.SupportScreen
import com.obscura.wallpapers.features.test.ApiTestScreen
import com.obscura.wallpapers.features.test.TestScreen
import com.obscura.wallpapers.features.video.VideoLibraryScreen
import com.obscura.wallpapers.ui.theme.LocalAppResources
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import java.net.URLDecoder

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainNavigation(navController: NavHostController = rememberNavController()) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isMainScreen = currentRoute in NavDestination.entries.map { it.route }
    val hazeState = remember { HazeState() }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        ) {
            SharedTransitionLayout {
                NavHost(
                    navController = navController,
                    startDestination = NavDestination.Discover.route,
                    modifier = Modifier.fillMaxSize(),
                    enterTransition = {
                        fadeIn(animationSpec = tween(300))
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(300))
                    }) {
                    composable("auth") {
                        SignInScreen(onLoginSuccess = {
                            navController.popBackStack()
                        }, onSkipLogin = {
                            navController.popBackStack()
                        })
                    }
                    composable(NavDestination.Discover.route) {
                        DiscoverScreen(
                            onWallpaperClick = { wallpaper ->
                                navController.navigate("preview/${wallpaper.id}")
                            },
                            onSearch = { query ->
                                navController.navigate("search?query=$query")
                            },
                            onBannerClick = { banner ->
                                when (banner.actionType) {
                                    BannerActionType.WALLPAPER, BannerActionType.COLLECTION -> {
                                        banner.actionTarget?.let { wallpaperId ->
                                            navController.navigate("preview/$wallpaperId")
                                        }
                                    }

                                    BannerActionType.PREMIUM -> {
                                        navController.navigate("premium")
                                    }

                                    BannerActionType.URL -> {
                                    }
                                }
                            },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@composable
                        )
                    }
                    composable(NavDestination.PhotoWallpapers.route) {
                        PhotoLibraryScreen(
                            onWallpaperClick = { wallpaper ->
                                navController.navigate("preview/${wallpaper.id}")
                            },
                            onSearchClick = {
                                navController.navigate("search")
                            },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@composable
                        )
                    }
                    composable(NavDestination.VideoWallpapers.route) {
                        VideoLibraryScreen(
                            onWallpaperClick = { wallpaper ->
                                navController.navigate("preview/${wallpaper.id}")
                            },
                            onSearchClick = {
                                navController.navigate("search")
                            },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@composable
                        )
                    }
                    composable(NavDestination.Profile.route) {
                        ProfileScreen(
                            onFavoritesClick = { navController.navigate("favorites") },
                            onDownloadsClick = { navController.navigate("downloads") },
                            onAutoChangeClick = { navController.navigate("cycler") },
                            onSettingsClick = { navController.navigate("settings") },
                            onFeedbackClick = { navController.navigate("feedback") },
                            onAboutClick = { navController.navigate("about") },
                            onLoginClick = { navController.navigate("auth") },
                            onSubscriptionClick = { navController.navigate("subscription") },
                            onCoinPurchaseClick = { navController.navigate("coin_store") },
                            onTestToolsClick = { navController.navigate("test") })
                    }
                    composable(
                        route = "search?query={query}", arguments = listOf(navArgument("query") {
                            type = NavType.StringType
                            defaultValue = ""
                        })
                    ) { backStackEntry ->
                        val query = backStackEntry.arguments?.getString("query") ?: ""
                        SearchScreen(
                            onWallpaperClick = { wallpaper ->
                                navController.navigate("preview/${wallpaper.id}")
                            },
                            onBackClick = { navController.navigateUp() },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@composable
                        )
                    }
                    composable(
                        route = "edit/{wallpaperId}",
                        arguments = listOf(navArgument("wallpaperId") { type = NavType.StringType })
                    ) {
                        val wallpaperId = it.arguments?.getString("wallpaperId") ?: ""
                        WallpaperEditScreen(
                            onBackPressed = { navController.navigateUp() },
                            onSaveComplete = { navController.navigateUp() })
                    }
                    composable(
                        route = "preview/{wallpaperId}",
                        arguments = listOf(navArgument("wallpaperId") { type = NavType.StringType })
                    ) {
                        WallpaperPreviewScreen(
                            onBackPressed = { navController.navigateUp() },
                            onNavigateToEdit = { wallpaperId ->
                                navController.navigate("edit/$wallpaperId")
                            },
                            onNavigateToLogin = { navController.navigate("auth") },
                            onNavigateToSubscription = { navController.navigate("subscription") },
                            onNavigateToCoinStore = { navController.navigate("coin_store") },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@composable
                        )
                    }
                    composable("favorites") {
                        LikesScreen(
                            onBackPressed = { navController.navigateUp() },
                            onWallpaperClick = { wallpaper ->
                                navController.navigate("preview/${wallpaper.id}")
                            },
                            onNavigateToLogin = { navController.navigate("auth") },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@composable
                        )
                    }
                    composable("downloads") {
                        LibraryScreen(
                            onBackPressed = { navController.navigateUp() },
                            onWallpaperClick = { wallpaper ->
                                navController.navigate("preview/${wallpaper.id}")
                            },
                            onNavigateToLogin = { navController.navigate("auth") },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@composable
                        )
                    }
                    composable("settings") {
                        PreferencesScreen(
                            onBackPressed = { navController.navigateUp() },
                            onNavigateToSubscription = { navController.navigate("subscription") })
                    }
                    composable("subscription") {
                        SubscriptionScreen(
                            onNavigateBack = { navController.navigateUp() })
                    }
                    composable("coin_store") {
                        CoinStoreScreen(
                            onNavigateBack = { navController.navigateUp() })
                    }
                    composable("cycler") {
                        WallpaperCyclerScreen(
                            onBackPressed = { navController.navigateUp() },
                            onNavigateToLogin = { navController.navigate("auth") },
                            onNavigateToSubscription = { navController.navigate("subscription") })
                    }
                    composable("feedback") {
                        SupportScreen(onBackPressed = { navController.navigateUp() })
                    }
                    composable("about") {
                        InfoScreen(
                            onBackPressed = { navController.navigateUp() },
                            navController = navController
                        )
                    }
                    composable(
                        route = "browser?url={url}", arguments = listOf(navArgument("url") {
                            type = NavType.StringType
                            nullable = false
                        }, navArgument("title") {
                            type = NavType.StringType
                            defaultValue = ""
                        })
                    ) { backStackEntry ->
                        val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
                        val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""
                        val url = URLDecoder.decode(encodedUrl, "UTF-8")
                        val title = URLDecoder.decode(encodedTitle, "UTF-8")
                        BrowserScreen(
                            url = url, title = title, onBackPressed = { navController.navigateUp() })
                    }
                    composable("test") {
                        TestScreen(
                            onBackPressed = { navController.navigateUp() },
                            onNavigateToApiTest = { navController.navigate("test/api") })
                    }
                    composable("test/api") {
                        ApiTestScreen(onBackPressed = { navController.navigateUp() })
                    }
                }
            }
        }
        if (isMainScreen) {
            BottomNavBar(
                navController = navController,
                hazeState = hazeState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 32.dp, end = 32.dp, bottom = 32.dp)
            )
        }
    }
}

@Composable
fun BottomNavBar(
    navController: NavController,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    NavigationBar(
        modifier = modifier
            .height(64.dp)
            .clip(CircleShape)
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    tint = HazeTint(MaterialTheme.colorScheme.surface.copy(alpha = 0.15f)),
                    blurRadius = 25.dp,
                    noiseFactor = 0.1f
                )
            ),
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        NavDestination.entries.forEach { destination ->
            val selected =
                currentDestination?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                icon = {
                    val icon = when (destination) {
                        NavDestination.Discover -> ObscuraIcons.Discover
                        NavDestination.PhotoWallpapers -> ObscuraIcons.Photo
                        NavDestination.VideoWallpapers -> ObscuraIcons.Video
                        NavDestination.Profile -> ObscuraIcons.Person
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = destination.getTitle(),
                            modifier = Modifier.size(24.dp),
                            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .offset(y = 18.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                },
                label = { },
                alwaysShowLabel = false,
                selected = selected,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                ),
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

enum class NavDestination(val route: String, val titleResId: Int) {
    Discover("discover", R.string.nav_discover) {
        @Composable
        override fun getTitle(): String = LocalAppResources.current.getString(R.string.nav_discover)
    },
    PhotoWallpapers("photo", R.string.nav_photo) {
        @Composable
        override fun getTitle(): String = LocalAppResources.current.getString(R.string.nav_photo)
    },
    VideoWallpapers("video", R.string.nav_video) {
        @Composable
        override fun getTitle(): String = LocalAppResources.current.getString(R.string.nav_video)
    },
    Profile("profile", R.string.nav_profile) {
        @Composable
        override fun getTitle(): String = LocalAppResources.current.getString(R.string.nav_profile)
    };

    @Composable
    abstract fun getTitle(): String
}
