package com.qjproject.liturgicalcalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val browseViewModel: BrowseViewModel = viewModel(factory = BrowseViewModelFactory(context))

    // --- POCZĄTEK ZMIANY: Logika widoczności BottomBar ---
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarVisible = currentRoute != Screen.DayDetails.route
    // --- KONIEC ZMIANY ---

    Scaffold(
        bottomBar = {
            if (bottomBarVisible) {
                BottomNavigationBar(navController = navController, browseViewModel = browseViewModel)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Browse.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Search.route) { SearchScreen() }
            composable(Screen.Browse.route) {
                BrowseScreen(
                    viewModel = browseViewModel,
                    onNavigateToDay = { dayPath ->
                        val encodedPath = java.net.URLEncoder.encode(dayPath, "UTF-8")
                        navController.navigate(Screen.DayDetails.createRoute(encodedPath))
                    }
                )
            }
            composable(Screen.Calendar.route) { CalendarScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(
                route = Screen.DayDetails.route,
                arguments = listOf(navArgument("dayId") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("dayId")
                val decodedPath = encodedPath?.let { java.net.URLDecoder.decode(it, "UTF-8") }
                DayDetailsScreen(
                    dayId = decodedPath,
                    // Przekazujemy funkcję do nawigacji wstecz
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavController, browseViewModel: BrowseViewModel) {
    val bottomNavItems = listOf(
        Screen.Search,
        Screen.Browse,
        Screen.Calendar,
        Screen.Settings
    )
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        bottomNavItems.forEach { screen ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (screen.route == Screen.Browse.route && isSelected) {
                        browseViewModel.onResetToRoot()
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