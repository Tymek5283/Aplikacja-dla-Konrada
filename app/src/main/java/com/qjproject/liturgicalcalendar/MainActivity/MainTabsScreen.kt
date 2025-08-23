// Ścieżka: C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\MainActivity\MainTabsScreen.kt
// Opis: Ten plik definiuje główny ekran aplikacji z dolnym paskiem nawigacyjnym. Zarządza przełączaniem między głównymi sekcjami: Wyszukaj, Przeglądaj, Kalendarz i Ustawienia.
package com.qjproject.liturgicalcalendar.MainActivity

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
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarScreen
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarViewModel
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarViewModelFactory
import com.qjproject.liturgicalcalendar.ui.screens.search.SearchScreen
import com.qjproject.liturgicalcalendar.ui.screens.search.SearchViewModel
import com.qjproject.liturgicalcalendar.ui.screens.search.SearchViewModelFactory
import com.qjproject.liturgicalcalendar.ui.screens.search.SongSortMode
import com.qjproject.liturgicalcalendar.ui.screens.settings.SettingsScreen
import com.qjproject.liturgicalcalendar.ui.screens.settings.SettingsViewModel
import com.qjproject.liturgicalcalendar.ui.screens.settings.SettingsViewModelFactory
import com.qjproject.liturgicalcalendar.ui.theme.NavBarBackground
import com.qjproject.liturgicalcalendar.ui.theme.SaturatedNavy
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun MainTabsScreen(navController: NavController) {
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
                    onNavigateToCategoryManagement = {
                        navController.navigate(Screen.CategoryManagement.route)
                    }
                )
                else -> {}
            }
        }
    }
}