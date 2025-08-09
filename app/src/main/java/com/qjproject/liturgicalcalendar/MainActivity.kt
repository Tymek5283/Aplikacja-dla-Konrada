package com.qjproject.liturgicalcalendar

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import com.qjproject.liturgicalcalendar.ui.components.AutoResizingText
import com.qjproject.liturgicalcalendar.ui.screens.browse.BrowseScreen
import com.qjproject.liturgicalcalendar.ui.screens.browse.BrowseViewModel
import com.qjproject.liturgicalcalendar.ui.screens.browse.BrowseViewModelFactory
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarScreen
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarViewModel
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarViewModelFactory
import com.qjproject.liturgicalcalendar.ui.screens.daydetails.DayDetailsScreen
import com.qjproject.liturgicalcalendar.ui.screens.search.SearchScreen
import com.qjproject.liturgicalcalendar.ui.screens.settings.SettingsScreen
import com.qjproject.liturgicalcalendar.ui.screens.settings.SettingsViewModel
import com.qjproject.liturgicalcalendar.ui.screens.settings.SettingsViewModelFactory
import com.qjproject.liturgicalcalendar.ui.theme.LiturgicalCalendarTheme
import kotlinx.coroutines.launch
import java.net.URLDecoder
import kotlin.system.exitProcess
import com.qjproject.liturgicalcalendar.ui.screens.dateevents.DateEventsScreen
import java.time.format.TextStyle
import java.util.Locale

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

fun restartApp(activity: Activity) {
    val intent = Intent(activity, MainActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    activity.startActivity(intent)
    activity.finish()
    exitProcess(0)
}

@OptIn(ExperimentalAnimationApi::class)
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
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val encodedPath = backStackEntry.arguments?.getString("dayId")
            val decodedPath = encodedPath?.let { URLDecoder.decode(it, "UTF-8") }
            DayDetailsScreen(
                dayId = decodedPath,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.DateEvents.route,
            arguments = listOf(
                navArgument("dateTitle") { type = NavType.StringType },
                navArgument("filePaths") { type = NavType.StringType }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val dateTitle = backStackEntry.arguments?.getString("dateTitle")?.let { URLDecoder.decode(it, "UTF-8") } ?: ""
            val filePathsString = backStackEntry.arguments?.getString("filePaths") ?: ""
            val filePaths = Screen.DateEvents.decodePaths(filePathsString)

            DateEventsScreen(
                dateTitle = dateTitle,
                filePaths = filePaths,
                onNavigateToDay = { dayPath ->
                    navController.navigate(Screen.DayDetails.createRoute(dayPath))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@OptIn(ExperimentalPagerApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainTabsScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = (LocalContext.current as? Activity)
    val browseViewModel: BrowseViewModel = viewModel(factory = BrowseViewModelFactory(context))
    val calendarViewModel: CalendarViewModel = viewModel(factory = CalendarViewModelFactory(context))
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))

    val bottomNavItems = listOf(Screen.Search, Screen.Browse, Screen.Calendar, Screen.Settings)
    val pagerState = rememberPagerState(initialPage = 1)
    val coroutineScope = rememberCoroutineScope()

    val browseUiState by browseViewModel.uiState.collectAsState()
    val isBrowseScreenActive = pagerState.currentPage == bottomNavItems.indexOf(Screen.Browse)

    Scaffold(
        topBar = {
            // --- POCZĄTEK POPRAWKI: Zmieniono 'when' aby operował na obiekcie 'Screen' ---
            val title = when (bottomNavItems[pagerState.currentPage]) {
                Screen.Search -> "Wyszukaj frazę"
                Screen.Browse -> browseUiState.screenTitle
                Screen.Calendar -> "Szukaj po dacie"
                Screen.Settings -> "Ustawienia"
                // Gałąź 'else' jest wymagana, ponieważ 'when' operuje na klasie zapieczętowanej (Screen),
                // a kompilator musi mieć pewność, że wszystkie przypadki są obsłużone.
                // W praktyce ta gałąź nigdy nie zostanie wykonana dla tego Pager'a.
                else -> ""
            }
            // --- KONIEC POPRAWKI ---
            MainTopAppBar(
                title = title,
                showBackButton = isBrowseScreenActive && browseUiState.isBackArrowVisible,
                onBackClick = { browseViewModel.onBackPress() }
            )
        },
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEachIndexed { index, screen ->
                    val isSelected = pagerState.currentPage == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) {
                                when (screen) {
                                    is Screen.Browse -> browseViewModel.onResetToRoot()
                                    is Screen.Calendar -> calendarViewModel.resetToCurrentMonth()
                                    else -> {}
                                }
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
                        navController.navigate(Screen.DayDetails.createRoute(dayPath))
                    }
                )
                is Screen.Calendar -> CalendarScreen(
                    onDayClick = { day ->
                        when {
                            day.files.isEmpty() -> {
                                // Nie rób nic
                            }
                            day.files.size == 1 -> {
                                val path = day.files.first()
                                navController.navigate(Screen.DayDetails.createRoute(path))
                            }
                            else -> {
                                val monthName = day.month.month.getDisplayName(TextStyle.FULL, Locale("pl"))
                                val dateTitle = "${day.dayOfMonth} $monthName"
                                navController.navigate(Screen.DateEvents.createRoute(dateTitle, day.files))
                            }
                        }
                    }
                )
                is Screen.Settings -> SettingsScreen(
                    viewModel = settingsViewModel,
                    onRestartApp = {
                        if (activity != null) {
                            restartApp(activity)
                        }
                    }
                )
                else -> {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    title: String,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    Column {
        CenterAlignedTopAppBar(
            title = {
                AutoResizingText(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            },
            navigationIcon = {
                if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Wróć"
                        )
                    }
                }
            },
            actions = {
                if (showBackButton) {
                    Spacer(Modifier.width(68.dp))
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.primary,
                navigationIconContentColor = MaterialTheme.colorScheme.primary
            ),
            windowInsets = TopAppBarDefaults.windowInsets
        )
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
    }
}