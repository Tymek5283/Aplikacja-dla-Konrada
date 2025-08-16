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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.SavedStateHandle
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
import com.qjproject.liturgicalcalendar.ui.screens.category.CategoryManagementScreen
import com.qjproject.liturgicalcalendar.ui.screens.category.CategoryManagementViewModelFactory
import com.qjproject.liturgicalcalendar.ui.screens.dateevents.DateEventsScreen
import com.qjproject.liturgicalcalendar.ui.screens.daydetails.DayDetailsScreen
import com.qjproject.liturgicalcalendar.ui.screens.search.SearchScreen
import com.qjproject.liturgicalcalendar.ui.screens.search.SearchViewModel
import com.qjproject.liturgicalcalendar.ui.screens.search.SearchViewModelFactory
import com.qjproject.liturgicalcalendar.ui.screens.search.SongSortMode
import com.qjproject.liturgicalcalendar.ui.screens.settings.SettingsScreen
import com.qjproject.liturgicalcalendar.ui.screens.settings.SettingsViewModel
import com.qjproject.liturgicalcalendar.ui.screens.settings.SettingsViewModelFactory
import com.qjproject.liturgicalcalendar.ui.screens.songcontent.SongContentScreen
import com.qjproject.liturgicalcalendar.ui.screens.songcontent.SongContentViewModel
import com.qjproject.liturgicalcalendar.ui.screens.songcontent.SongContentViewModelFactory
import com.qjproject.liturgicalcalendar.ui.screens.songdetails.SongDetailsScreen
import com.qjproject.liturgicalcalendar.ui.screens.songdetails.SongDetailsViewModel
import com.qjproject.liturgicalcalendar.ui.screens.songdetails.SongDetailsViewModelFactory
import com.qjproject.liturgicalcalendar.ui.theme.LiturgicalCalendarTheme
import com.qjproject.liturgicalcalendar.ui.theme.NavBarBackground
import com.qjproject.liturgicalcalendar.ui.theme.SaturatedNavy
import kotlinx.coroutines.launch
import java.net.URLDecoder
import kotlin.system.exitProcess

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

    val safePopBackStack: () -> Unit = {
        navController.navigateUp()
    }

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
                onNavigateBack = safePopBackStack,
                onNavigateToSongContent = { song, startInEdit ->
                    navController.navigate(Screen.SongContent.createRoute(song, startInEdit))
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
                onNavigateBack = safePopBackStack
            )
        }
        composable(
            route = Screen.SongDetails.route,
            arguments = listOf(
                navArgument("songTitle") { type = NavType.StringType },
                navArgument("siedlNum") { type = NavType.StringType; nullable = true },
                navArgument("sakNum") { type = NavType.StringType; nullable = true },
                navArgument("dnNum") { type = NavType.StringType; nullable = true }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val context = LocalContext.current
            val factory = SongDetailsViewModelFactory(context, SavedStateHandle.createHandle(null, backStackEntry.arguments))
            val viewModel: SongDetailsViewModel = viewModel(factory = factory)

            SongDetailsScreen(
                viewModel = viewModel,
                onNavigateBack = safePopBackStack,
                onNavigateToContent = { song, startInEdit -> navController.navigate(Screen.SongContent.createRoute(song, startInEdit)) }
            )
        }
        composable(
            route = Screen.SongContent.route,
            arguments = listOf(
                navArgument("songTitle") { type = NavType.StringType },
                navArgument("siedlNum") { type = NavType.StringType; nullable = true },
                navArgument("sakNum") { type = NavType.StringType; nullable = true },
                navArgument("dnNum") { type = NavType.StringType; nullable = true },
                navArgument("editOnStart") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            ),
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val startInEdit = backStackEntry.arguments?.getBoolean("editOnStart") ?: false
            val context = LocalContext.current
            val factory = SongContentViewModelFactory(context, SavedStateHandle.createHandle(null, backStackEntry.arguments))
            val viewModel: SongContentViewModel = viewModel(factory = factory)

            SongContentScreen(
                viewModel = viewModel,
                onNavigateBack = safePopBackStack,
                startInEditMode = startInEdit
            )
        }
        // --- POCZĄTEK ZMIANY ---
        composable(
            route = Screen.CategoryManagement.route,
            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) }
        ) {
            val context = LocalContext.current
            val factory = CategoryManagementViewModelFactory(context)
            CategoryManagementScreen(
                viewModel = viewModel(factory = factory),
                onNavigateBack = safePopBackStack
            )
        }
        // --- KONIEC ZMIANY ---
    }
}

@OptIn(ExperimentalPagerApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainTabsScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = (LocalContext.current as? Activity)
    val browseViewModel = viewModel<BrowseViewModel>(factory = BrowseViewModelFactory(context))
    val calendarViewModel = viewModel<CalendarViewModel>(factory = CalendarViewModelFactory(context))
    val settingsViewModel = viewModel<SettingsViewModel>(factory = SettingsViewModelFactory(context))
    val searchViewModel = viewModel<SearchViewModel>(factory = SearchViewModelFactory(context))


    val bottomNavItems = listOf(Screen.Search, Screen.Browse, Screen.Calendar, Screen.Settings)
    val pagerState = rememberPagerState(initialPage = 1)
    val coroutineScope = rememberCoroutineScope()

    val browseUiState by browseViewModel.uiState.collectAsState()
    val searchUiState by searchViewModel.uiState.collectAsState()

    val isSearchScreenActive = pagerState.currentPage == bottomNavItems.indexOf(Screen.Search)
    val isBrowseScreenActive = pagerState.currentPage == bottomNavItems.indexOf(Screen.Browse)
    val isCalendarScreenActive = pagerState.currentPage == bottomNavItems.indexOf(Screen.Calendar)

    var isCalendarMenuExpanded by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        when {
            // Logika dla ekranu przeglądania
            isBrowseScreenActive -> {
                if (browseUiState.isEditMode) {
                    browseViewModel.onTryExitEditMode {}
                } else {
                    browseViewModel.onBackPress()
                }
            }
            // Dla pozostałych przypadków (inne ekrany bez otwartych menu), nie rób nic,
            // aby zapobiec zamknięciu aplikacji
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            val title = when (bottomNavItems.getOrNull(pagerState.currentPage)) {
                Screen.Search -> "Wyszukaj pieśń"
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
                isSaveEnabled = browseUiState.hasChanges,
                isCalendarScreenActive = isCalendarScreenActive,
                isSearchScreenActive = isSearchScreenActive,
                searchActions = {
                    var showSearchOptions by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showSearchOptions = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opcje wyszukiwania")
                        }
                        DropdownMenu(
                            expanded = showSearchOptions,
                            onDismissRequest = { showSearchOptions = false }
                        ) {
                            Text("Szukaj w:", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
                            DropdownMenuItem(
                                text = { Text("Tytuł") },
                                onClick = { searchViewModel.onSearchInTitleChange(!searchUiState.searchInTitle) },
                                leadingIcon = { Checkbox(checked = searchUiState.searchInTitle, onCheckedChange = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Treść") },
                                onClick = { searchViewModel.onSearchInContentChange(!searchUiState.searchInContent) },
                                leadingIcon = { Checkbox(checked = searchUiState.searchInContent, onCheckedChange = null) }
                            )
                            if (searchUiState.results.isNotEmpty()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Text("Sortuj:", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
                                Column(Modifier.selectableGroup()) {
                                    SongSortMode.values().forEach { mode ->
                                        DropdownMenuItem(
                                            text = { Text(if (mode == SongSortMode.Kategoria) "Kategoria" else mode.name) },
                                            onClick = {
                                                searchViewModel.onSortModeChange(mode)
                                                showSearchOptions = false
                                            },
                                            leadingIcon = { RadioButton(selected = (searchUiState.sortMode == mode), onClick = null) },
                                            modifier = Modifier.selectable(
                                                selected = (searchUiState.sortMode == mode),
                                                onClick = {
                                                    searchViewModel.onSortModeChange(mode)
                                                    showSearchOptions = false
                                                },
                                                role = Role.RadioButton
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                calendarActions = {
                    Box {
                        IconButton(onClick = { isCalendarMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opcje")
                        }
                        DropdownMenu(
                            expanded = isCalendarMenuExpanded,
                            onDismissRequest = { isCalendarMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Pobierz aktualne dane") },
                                onClick = {
                                    calendarViewModel.forceRefreshData()
                                    isCalendarMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.2f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                NavigationBar(
                    containerColor = NavBarBackground
                ) {
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
                            label = { Text(screen.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SaturatedNavy,
                                selectedTextColor = SaturatedNavy,
                                indicatorColor = SaturatedNavy.copy(alpha = 0.15f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        )
                    }
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
                    viewModel = searchViewModel,
                    onNavigateToSong = { song ->
                        navController.navigate(Screen.SongDetails.createRoute(song))
                    }
                )
                is Screen.Browse -> BrowseScreen(
                    viewModel = browseViewModel,
                    onNavigateToDay = { dayPath ->
                        navController.navigate(Screen.DayDetails.createRoute(dayPath))
                    }
                )
                is Screen.Calendar -> CalendarScreen(
                    onNavigateToDay = { dayPath ->
                        navController.navigate(Screen.DayDetails.createRoute(dayPath))
                    },
                    onNavigateToDateEvents = { title, paths ->
                        navController.navigate(Screen.DateEvents.createRoute(title, paths))
                    }
                )
                is Screen.Settings -> SettingsScreen(
                    viewModel = settingsViewModel,
                    onRestartApp = {
                        if (activity != null) {
                            restartApp(activity)
                        }
                    },
                    // --- POCZĄTEK ZMIANY ---
                    onNavigateToCategoryManagement = {
                        navController.navigate(Screen.CategoryManagement.route)
                    }
                    // --- KONIEC ZMIANY ---
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
    isSaveEnabled: Boolean = false,
    isCalendarScreenActive: Boolean = false,
    isSearchScreenActive: Boolean = false,
    searchActions: @Composable RowScope.() -> Unit = {},
    calendarActions: @Composable RowScope.() -> Unit = {}
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
                if (isSearchScreenActive) {
                    searchActions()
                } else if (isBrowseScreenInEditMode) {
                    IconButton(onClick = onSaveClick, enabled = isSaveEnabled) {
                        Icon(Icons.Default.Check, "Zapisz zmiany", tint = if (isSaveEnabled) MaterialTheme.colorScheme.primary else Color.Gray)
                    }
                } else if (showEditButton) {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, "Edytuj")
                    }
                } else if (isCalendarScreenActive) {
                    calendarActions()
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