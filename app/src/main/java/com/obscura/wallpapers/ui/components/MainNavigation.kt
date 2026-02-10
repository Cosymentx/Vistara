package com.obscura.wallpapers.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import java.net.URLDecoder

@Composable
fun MainNavigation(navController: NavHostController = rememberNavController()) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isMainScreen = currentRoute in NavDestination.entries.map { it.route }
    val hazeState = rememberHazeState()
    Box(modifier = Modifier.hazeSource(state = hazeState)) {
        NavHost(
            navController = navController,
            startDestination = NavDestination.Discover.route,
            modifier = Modifier
        ) {
            composable("auth") {
                SignInScreen(onLoginSuccess = {
                    navController.popBackStack()
                }, onSkipLogin = {
                    navController.popBackStack()
                })
            }
            composable(NavDestination.Discover.route) {
                DiscoverScreen(onWallpaperClick = { wallpaper ->
                    navController.navigate("preview/${wallpaper.id}")
                }, onSearch = { query ->
                    navController.navigate("search?query=$query")
                }, onBannerClick = { banner ->
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
                })
            }
            composable(NavDestination.PhotoWallpapers.route) {
                PhotoLibraryScreen(onWallpaperClick = { wallpaper ->
                    navController.navigate("preview/${wallpaper.id}")
                }, onSearchClick = {
                    navController.navigate("search")
                })
            }
            composable(NavDestination.VideoWallpapers.route) {
                VideoLibraryScreen(onWallpaperClick = { wallpaper ->
                    navController.navigate("preview/${wallpaper.id}")
                }, onSearchClick = {
                    navController.navigate("search")
                })
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
                    onTestToolsClick = { navController.navigate("test") })
            }
            composable(
                route = "search?query={query}", arguments = listOf(navArgument("query") {
                    type = NavType.StringType
                    defaultValue = ""
                })
            ) { backStackEntry ->
                val query = backStackEntry.arguments?.getString("query") ?: ""
                SearchScreen(onWallpaperClick = { wallpaper ->
                    navController.navigate("wallpaper/${wallpaper.id}")
                }, onBackClick = { navController.navigateUp() })
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
                    onNavigateToLogin = { navController.navigate("auth") })
            }
            composable("favorites") {
                LikesScreen(
                    onBackPressed = { navController.navigateUp() },
                    onWallpaperClick = { wallpaper ->
                        navController.navigate("preview/${wallpaper.id}")
                    },
                    onNavigateToLogin = { navController.navigate("auth") })
            }
            composable("downloads") {
                LibraryScreen(
                    onBackPressed = { navController.navigateUp() },
                    onWallpaperClick = { wallpaper ->
                        navController.navigate("preview/${wallpaper.id}")
                    },
                    onNavigateToLogin = { navController.navigate("auth") })
            }
            composable("settings") {
                PreferencesScreen(onBackPressed = { navController.navigateUp() })
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
                    onBackPressed = { navController.navigateUp() }, navController = navController
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
        if (isMainScreen) {
            BottomNavBar(
                navController = navController,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .hazeEffect(
                        state = hazeState, style = HazeDefaults.style(
                            backgroundColor = MaterialTheme.colorScheme.surface, blurRadius = 10.dp
                        )
                    )
            )
        }
    }
}

@Composable
fun BottomNavBar(navController: NavController, modifier: Modifier = Modifier) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    NavigationBar(modifier = modifier, containerColor = Color.Transparent, tonalElevation = 0.dp) {
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
            }, label = { }, selected = selected, onClick = {
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
