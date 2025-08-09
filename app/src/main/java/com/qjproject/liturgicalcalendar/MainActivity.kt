package com.qjproject.liturgicalcalendar

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope // WAŻNY IMPORT
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.qjproject.liturgicalcalendar.navigation.Screen
import com.qjproject.liturgicalcalendar.ui.screens.browse.BrowseScreen
import com.qjproject.liturgicalcalendar.ui.screens.browse.BrowseViewModel
import com.qjproject.liturgicalcalendar.ui.screens.browse.BrowseViewModelFactory
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarScreen
import com.qjproject.liturgicalcalendar.ui.screens.daydetails.DayDetailsScreen
import com.qjproject.liturgicalcalendar.ui.screens.search.SearchScreen
import com.qjproject.liturgicalcalendar.ui.screens.settings.SettingsScreen
import com.qjproject.liturgicalcalendar.ui.theme.LiturgicalCalendarTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        setContent {
            LiturgicalCalendarTheme {
                MainAppHost()
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class, ExperimentalPagerApi::class) // DODANA ADNOTACJA
@Composable
fun MainAppHost() {
    val navController = rememberAnimatedNavController()

    AnimatedNavHost(navController = navController, startDestination = "main_tabs") {
        composable("main_tabs") {
            MainTabsScreen(navController = navController)
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

@OptIn(ExperimentalPagerApi::class) // DODANA ADNOTACJA
@Composable
fun MainTabsScreen(navController: NavController) {
    val context = LocalContext.current
    val browseViewModel: BrowseViewModel = viewModel(factory = BrowseViewModelFactory(context))
    val bottomNavItems = listOf(Screen.Search, Screen.Browse, Screen.Calendar, Screen.Settings)
    val pagerState = rememberPagerState(initialPage = 1)
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            if (pagerState.currentPage == index && screen is Screen.Browse) {
                                browseViewModel.onResetToRoot()
                            } else {
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            }
                        },
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            count = bottomNavItems.size,
            state = pagerState,
            modifier = Modifier.padding(innerPadding),
            userScrollEnabled = true
        ) { pageIndex ->
            when (bottomNavItems[pageIndex]) {
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
}