package com.obscura.wallpapers.ui.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
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
import com.obscura.wallpapers.features.browser.BrowserScreen
import com.obscura.wallpapers.features.cycler.WallpaperCyclerScreen
import com.obscura.wallpapers.features.diamond.DiamondPurchaseScreen
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
import com.obscura.wallpapers.features.subscription.SubscriptionScreen
import com.obscura.wallpapers.features.support.SupportScreen
import com.obscura.wallpapers.features.test.ApiTestScreen
import com.obscura.wallpapers.features.test.TestScreen
import com.obscura.wallpapers.features.video.VideoLibraryScreen
import com.obscura.wallpapers.ui.theme.LocalAppResources
import com.skydoves.cloudy.liquidGlass
import java.net.URLDecoder

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainNavigation(navController: NavHostController = rememberNavController()) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isMainScreen = currentRoute in NavDestination.entries.map { it.route }
    var navigationBarSize: Size? by remember { mutableStateOf(null) }
    var navigationBarOffset: Offset by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = Modifier
        .fillMaxSize()
        .then(navigationBarSize?.let { size ->
            if (isMainScreen) {
                Modifier.liquidGlass(
                    lensCenter = navigationBarOffset,
                    lensSize = size,
                    cornerRadius = 100f,
                    edge = 0.6f,
                    refraction = 0.3f,
                    curve = 0.5f,
                )
            } else {
                null
            }
        } ?: Modifier)) {
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
                        onDiamondPurchaseClick = { navController.navigate("diamond_purchase") },
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
                composable("diamond_purchase") {
                    DiamondPurchaseScreen(
                        onNavigateBack = { navController.navigateUp() })
                }
                composable("cycler") {
                    WallpaperCyclerScreen(
                        onBackPressed = { navController.navigateUp() },
                        onNavigateToLogin = { navController.navigate("auth") })
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
        if (isMainScreen) {
            BottomNavBar(
                navController = navController,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 18.dp, end = 18.dp, bottom = 20.dp),
                onSizeChange = { size, offset ->
                    navigationBarSize = size
                    navigationBarOffset = offset
                })
        }
    }
}

@Composable
fun BottomNavBar(
    navController: NavController,
    modifier: Modifier = Modifier,
    onSizeChange: (Size, Offset) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    NavigationBar(
        modifier = modifier
            .navigationBarsPadding()
            .onGloballyPositioned { layoutCoordinates ->
                if (layoutCoordinates.size.width > 10) {
                    val bounds = layoutCoordinates.boundsInRoot()
                    onSizeChange(
                        bounds.size,
                        bounds.center,
                    )
                }
            }, containerColor = Color.Transparent, tonalElevation = 0.dp
    ) {
        NavDestination.entries.forEach { destination ->
            val selected =
                currentDestination?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(icon = {
                when (destination) {
                    NavDestination.Discover -> {
                        Icon(
                            ImageVector.vectorResource(id = R.drawable.ic_discover),
                            contentDescription = destination.getTitle()
                        )
                    }

                    NavDestination.PhotoWallpapers -> {
                        Icon(
                            ImageVector.vectorResource(id = R.drawable.ic_photo),
                            contentDescription = destination.getTitle()
                        )
                    }

                    NavDestination.VideoWallpapers -> {
                        Icon(
                            ImageVector.vectorResource(id = R.drawable.ic_video),
                            contentDescription = destination.getTitle()
                        )
                    }

                    NavDestination.Profile -> {
                        Icon(
                            ImageVector.vectorResource(id = R.drawable.ic_profile),
                            contentDescription = destination.getTitle()
                        )
                    }
                }
            }, label = { }, alwaysShowLabel = false, selected = selected, onClick = {
                navController.navigate(destination.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            })
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
