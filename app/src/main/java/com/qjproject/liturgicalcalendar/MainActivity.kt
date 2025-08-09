package com.qjproject.liturgicalcalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen // POPRAWIONY IMPORT
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.qjproject.liturgicalcalendar.navigation.Screen
import com.qjproject.liturgicalcalendar.ui.screens.browse.BrowseScreen
import com.qjproject.liturgicalcalendar.ui.screens.browse.BrowseViewModel
import com.qjproject.liturgicalcalendar.ui.screens.browse.BrowseViewModelFactory
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarScreen
import com.qjproject.liturgicalcalendar.ui.screens.daydetails.DayDetailsScreen
import com.qjproject.liturgicalcalendar.ui.screens.search.SearchScreen
import com.qjproject.liturgicalcalendar.ui.screens.settings.SettingsScreen
import com.qjproject.liturgicalcalendar.ui.theme.LiturgicalCalendarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            LiturgicalCalendarTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen() {
    val navController = rememberAnimatedNavController()
    val context = LocalContext.current
    val browseViewModel: BrowseViewModel = viewModel(factory = BrowseViewModelFactory(context))

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val bottomBarVisible = currentRoute != Screen.DayDetails.route

    Scaffold(
        bottomBar = {
            if (bottomBarVisible) {
                BottomNavigationBar(
                    navController = navController,
                    browseViewModel = browseViewModel
                )
            }
        }
    ) { innerPadding ->
        AnimatedNavHost(
            navController = navController,
            startDestination = Screen.Browse.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            val bottomNavItems = listOf(Screen.Search, Screen.Browse, Screen.Calendar, Screen.Settings)

            bottomNavItems.forEach { screen ->
                composable(
                    route = screen.route,
                    enterTransition = {
                        val startIndex = bottomNavItems.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = bottomNavItems.indexOfFirst { it.route == targetState.destination.route }
                        if (startIndex == -1 || targetIndex == -1) {
                            fadeIn(animationSpec = tween(300))
                        } else if (targetIndex > startIndex) {
                            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
                        } else {
                            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
                        }
                    },
                    exitTransition = {
                        val startIndex = bottomNavItems.indexOfFirst { it.route == initialState.destination.route }
                        val targetIndex = bottomNavItems.indexOfFirst { it.route == targetState.destination.route }
                        if (startIndex == -1 || targetIndex == -1) {
                            fadeOut(animationSpec = tween(300))
                        } else if (targetIndex > startIndex) {
                            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
                        } else {
                            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
                        }
                    }
                ) {
                    when (screen) {
                        is Screen.Search -> SearchScreen()
                        is Screen.Browse -> BrowseScreen(
                            viewModel = browseViewModel,
                            onNavigateToDay = { dayPath ->
                                val encodedPath = java.net.URLEncoder.encode(dayPath, "UTF-8")
                                navController.navigate(Screen.DayDetails.createRoute(encodedPath))
                            }
                        )
                        is Screen.Calendar -> CalendarScreen()
                        is Screen.Settings -> SettingsScreen()
                        else -> {}
                    }
                }
            }

            composable(
                route = Screen.DayDetails.route,
                arguments = listOf(navArgument("dayId") { type = NavType.StringType }),
                enterTransition = { fadeIn(animationSpec = tween(350)) },
                exitTransition = { fadeOut(animationSpec = tween(350)) }
            ) { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("dayId")
                val decodedPath = encodedPath?.let { java.net.URLDecoder.decode(it, "UTF-8") }
                DayDetailsScreen(
                    dayId = decodedPath,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    navController: NavController,
    browseViewModel: BrowseViewModel
) {
    val bottomNavItems = listOf(Screen.Search, Screen.Browse, Screen.Calendar, Screen.Settings)

    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        bottomNavItems.forEachIndexed { index, screen ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (isSelected) {
                        if (screen.route == Screen.Browse.route) {
                            browseViewModel.onResetToRoot()
                        }
                    } else {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(imageVector = screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) }
            )
        }
    }
}