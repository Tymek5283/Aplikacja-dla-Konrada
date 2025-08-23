// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/main/MainAppHost.kt
// Opis: Ten plik zawiera główny kontener nawigacyjny aplikacji (AnimatedNavHost), który zarządza wszystkimi ekranami i przejściami między nimi. Został przeniesiony do pakietu ui.main.
package com.qjproject.liturgicalcalendar.ui.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.qjproject.liturgicalcalendar.navigation.Screen
import com.qjproject.liturgicalcalendar.ui.screens.category.CategoryManagementScreen
import com.qjproject.liturgicalcalendar.ui.screens.category.CategoryManagementViewModelFactory
import com.qjproject.liturgicalcalendar.ui.screens.dateevents.DateEventsScreen
import com.qjproject.liturgicalcalendar.ui.screens.daydetails.daydetailsscreen.DayDetailsScreen
import com.qjproject.liturgicalcalendar.ui.screens.songcontent.SongContentScreen
import com.qjproject.liturgicalcalendar.ui.screens.songcontent.SongContentViewModel
import com.qjproject.liturgicalcalendar.ui.screens.songcontent.SongContentViewModelFactory
import com.qjproject.liturgicalcalendar.ui.screens.songdetails.SongDetailsScreen
import com.qjproject.liturgicalcalendar.ui.screens.songdetails.SongDetailsViewModel
import com.qjproject.liturgicalcalendar.ui.screens.songdetails.SongDetailsViewModelFactory
import java.net.URLDecoder

@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun MainAppHost() {
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
    }
}