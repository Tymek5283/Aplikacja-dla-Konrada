package com.qjproject.liturgicalcalendar.ui.main

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.qjproject.liturgicalcalendar.navigation.Screen
import com.qjproject.liturgicalcalendar.ui.screens.browse.BrowseScreen
import com.qjproject.liturgicalcalendar.ui.screens.browse.BrowseViewModel
import com.qjproject.liturgicalcalendar.ui.screens.browse.BrowseViewModelFactory
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarScreen.CalendarScreen
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarViewModel.CalendarViewModel
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarViewModel.CalendarViewModelFactory
import com.qjproject.liturgicalcalendar.ui.screens.search.SearchScreen
import com.qjproject.liturgicalcalendar.ui.screens.search.SearchViewModel
import com.qjproject.liturgicalcalendar.ui.screens.search.SearchViewModelFactory
import com.qjproject.liturgicalcalendar.ui.screens.search.SongSortMode
import com.qjproject.liturgicalcalendar.ui.screens.settings.SettingsScreen
import com.qjproject.liturgicalcalendar.ui.screens.settings.SettingsViewModel
import com.qjproject.liturgicalcalendar.ui.screens.settings.SettingsViewModelFactory
import com.qjproject.liturgicalcalendar.ui.theme.NavBarBackground
import com.qjproject.liturgicalcalendar.ui.theme.SaturatedNavy
import com.qjproject.liturgicalcalendar.data.FirstRunManager
import com.qjproject.liturgicalcalendar.data.NeumyManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun MainTabsScreen(
    navController: NavController,
    selectedTag: String? = null
) {
    val context = LocalContext.current
    val activity = (LocalContext.current as? Activity)
    val browseViewModel = viewModel<BrowseViewModel>(factory = BrowseViewModelFactory(context))
    val calendarViewModel = viewModel<CalendarViewModel>(factory = CalendarViewModelFactory(context))
    val settingsViewModel = viewModel<SettingsViewModel>(factory = SettingsViewModelFactory(context))
    val searchViewModel = viewModel<SearchViewModel>(factory = SearchViewModelFactory(context))

    val bottomNavItems = listOf(Screen.Search, Screen.Browse, Screen.Calendar, Screen.Settings)
    val pagerState = rememberPagerState(initialPage = 0) // Start on Search screen
    val coroutineScope = rememberCoroutineScope()

    val browseUiState by browseViewModel.uiState.collectAsState()
    val searchUiState by searchViewModel.uiState.collectAsState()

    val isSearchScreenActive = pagerState.currentPage == bottomNavItems.indexOf(Screen.Search)
    val isBrowseScreenActive = pagerState.currentPage == bottomNavItems.indexOf(Screen.Browse)
    val isCalendarScreenActive = pagerState.currentPage == bottomNavItems.indexOf(Screen.Calendar)

    var isCalendarMenuExpanded by remember { mutableStateOf(false) }

    // Logika pierwszego uruchomienia - kopiowanie plików PDF z assets
    LaunchedEffect(Unit) {
        val firstRunManager = FirstRunManager(context)
        val neumyManager = NeumyManager(context)
        
        if (firstRunManager.isFirstRun() && !firstRunManager.areAssetsCopied()) {
            try {
                val result = neumyManager.copyAssetsToInternalStorage()
                if (result.isSuccess) {
                    val copiedCount = result.getOrNull() ?: 0
                    android.util.Log.i("FirstRun", "Skopiowano $copiedCount plików PDF z assets do pamięci wewnętrznej")
                    firstRunManager.markAssetsCopied()
                } else {
                    android.util.Log.e("FirstRun", "Błąd podczas kopiowania plików PDF: ${result.exceptionOrNull()?.message}")
                }
                firstRunManager.markFirstRunCompleted()
            } catch (e: Exception) {
                android.util.Log.e("FirstRun", "Nieoczekiwany błąd podczas pierwszego uruchomienia: ${e.message}")
            }
        }
    }

    // Obsługa preselektowanego tagu
    LaunchedEffect(selectedTag) {
        selectedTag?.let { tag ->
            searchViewModel.onTagSelected(tag)
        }
    }

    BackHandler(enabled = true) {
        when {
            isBrowseScreenActive -> {
                if (browseUiState.isEditMode) {
                    browseViewModel.onTryExitEditMode {}
                } else {
                    browseViewModel.onBackPress()
                }
            }
            isSearchScreenActive && searchUiState.isBackButtonVisible -> {
                searchViewModel.onNavigateBack()
            }
            else -> {
                // Allow default back behavior for other cases
            }
        }
    }

    Scaffold(
        topBar = {
            val title = when (bottomNavItems.getOrNull(pagerState.currentPage)) {
                Screen.Search -> when {
                    searchUiState.selectedCategory != null -> searchUiState.selectedCategory?.nazwa ?: "Pieśni"
                    searchUiState.selectedTag != null -> searchUiState.selectedTag ?: "Tag"
                    else -> "Wyszukaj"
                }
                Screen.Browse -> browseUiState.screenTitle
                Screen.Calendar -> "Szukaj po dacie"
                Screen.Settings -> "Ustawienia"
                else -> ""
            }
            MainTopAppBar(
                title = title,
                showBackButton = (isBrowseScreenActive && browseUiState.isBackArrowVisible) || (isSearchScreenActive && searchUiState.isBackButtonVisible),
                onBackClick = {
                    if (isBrowseScreenActive) {
                        browseViewModel.onBackPress()
                    } else if (isSearchScreenActive) {
                        searchViewModel.onNavigateBack()
                    }
                },
                isBrowseScreenInEditMode = isBrowseScreenActive && browseUiState.isEditMode,
                showEditButton = isBrowseScreenActive,
                onEditClick = { browseViewModel.onEnterEditMode() },
                onSaveClick = { browseViewModel.onSaveEditMode() },
                onCancelClick = { browseViewModel.onTryExitEditMode {} },
                isSaveEnabled = browseUiState.hasChanges,
                isCalendarScreenActive = isCalendarScreenActive,
                isSearchScreenActive = isSearchScreenActive,
                showCategoryIcon = isSearchScreenActive && searchUiState.selectedCategory != null,
                showTagIcon = isSearchScreenActive && searchUiState.selectedTag != null,
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
                            if (searchUiState.songResults.isNotEmpty()) {
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                                Text("Sortuj:", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
                                Column(Modifier.selectableGroup()) {
                                    SongSortMode.values().forEach { mode ->
                                        DropdownMenuItem(
                                            text = { Text(if (mode == SongSortMode.Kategoria) "Kategoria" else "Alfabetycznie") },
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
                                        is Screen.Search -> searchViewModel.onResetToRoot()
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
            userScrollEnabled = !browseUiState.isEditMode && !searchUiState.isBackButtonVisible
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
                    onNavigateToCategoryManagement = {
                        navController.navigate(Screen.CategoryManagement.route)
                    },
                    onNavigateToTagManagement = {
                        navController.navigate(Screen.TagManagement.route)
                    },
                    onNavigateToNotes = {
                        navController.navigate(Screen.Notes.route)
                    }
                )
                else -> {}
            }
        }
    }
}