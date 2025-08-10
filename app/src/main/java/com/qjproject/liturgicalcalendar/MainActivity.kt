package com.qjproject.liturgicalcalendar

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
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
import com.qjproject.liturgicalcalendar.ui.screens.songcontent.SongContentScreen
import com.qjproject.liturgicalcalendar.ui.screens.songdetails.SongDetailsScreen

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
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSongContent = { songNumber ->
                    navController.navigate(Screen.SongContent.createRoute(songNumber))
                }
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
        composable(
            route = Screen.SongDetails.route,
            arguments = listOf(navArgument("songNumber") { type = NavType.StringType }),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val songNumber = backStackEntry.arguments?.getString("songNumber")?.let { URLDecoder.decode(it, "UTF-8") }
            SongDetailsScreen(
                songNumber = songNumber,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToContent = { num -> navController.navigate(Screen.SongContent.createRoute(num)) }
            )
        }
        composable(
            route = Screen.SongContent.route,
            arguments = listOf(navArgument("songNumber") { type = NavType.StringType }),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val songNumber = backStackEntry.arguments?.getString("songNumber")?.let { URLDecoder.decode(it, "UTF-8") }
            SongContentScreen(
                songNumber = songNumber,
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

    BackHandler(enabled = true) {
    }

    Scaffold(
        topBar = {
            val title = when (bottomNavItems.getOrNull(pagerState.currentPage)) {
                Screen.Search -> "Wyszukaj frazę"
                Screen.Browse -> browseUiState.screenTitle
                Screen.Calendar -> "Szukaj po dacie"
                Screen.Settings -> "Ustawienia"
                else -> ""
            }
            MainTopAppBar(
                title = title,
                showBackButton = isBrowseScreenActive && browseUiState.isBackArrowVisible,
                onBackClick = { browseViewModel.onBackPress() },
                isBrowseScreenInEditMode = isBrowseScreenActive && browseUiState.isEditMode,
                showEditButton = isBrowseScreenActive,
                onEditClick = { browseViewModel.onEnterEditMode() },
                onSaveClick = { browseViewModel.onSaveEditMode() },
                onCancelClick = { browseViewModel.onTryExitEditMode {} },
                isSaveEnabled = browseUiState.hasChanges
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
                                    // --- POCZĄTEK ZMIANY: Poprawka wywołania ---
                                    is Screen.Calendar -> calendarViewModel.resetToCurrentMonth()
                                    // --- KONIEC ZMIANY ---
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
            userScrollEnabled = !browseUiState.isEditMode
        ) { pageIndex ->
            when (bottomNavItems[pageIndex]) {
                is Screen.Search -> SearchScreen(
                    onNavigateToDay = { dayPath ->
                        navController.navigate(Screen.DayDetails.createRoute(dayPath))
                    },
                    onNavigateToSong = { songNumber ->
                        navController.navigate(Screen.SongDetails.createRoute(songNumber))
                    }
                )
                is Screen.Browse -> BrowseScreen(
                    viewModel = browseViewModel,
                    onNavigateToDay = { dayPath ->
                        navController.navigate(Screen.DayDetails.createRoute(dayPath))
                    }
                )
                // --- POCZĄTEK ZMIANY: Poprawka wywołania ---
                is Screen.Calendar -> CalendarScreen(
                    onNavigateToDay = { dayPath ->
                        navController.navigate(Screen.DayDetails.createRoute(dayPath))
                    }
                )
                // --- KONIEC ZMIANY ---
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
    onBackClick: () -> Unit = {},
    isBrowseScreenInEditMode: Boolean = false,
    showEditButton: Boolean = false,
    onEditClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onCancelClick: () -> Unit = {},
    isSaveEnabled: Boolean = false
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
                if (isBrowseScreenInEditMode) {
                    IconButton(onClick = onCancelClick) {
                        Icon(Icons.Default.Close, "Anuluj edycję")
                    }
                } else if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć")
                    }
                } else {
                    Spacer(Modifier.width(48.dp))
                }
            },
            actions = {
                if (isBrowseScreenInEditMode) {
                    IconButton(onClick = onSaveClick, enabled = isSaveEnabled) {
                        Icon(Icons.Default.Check, "Zapisz zmiany", tint = if (isSaveEnabled) MaterialTheme.colorScheme.primary else Color.Gray)
                    }
                } else if (showEditButton) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, "Edytuj")
                    }
                } else {
                    Spacer(Modifier.width(48.dp))
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