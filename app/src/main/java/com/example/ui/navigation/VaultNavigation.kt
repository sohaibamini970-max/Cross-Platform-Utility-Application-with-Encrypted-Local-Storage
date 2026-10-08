package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.AddEditRecordScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.RecordDetailsScreen
import com.example.ui.screens.RecordsListScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VaultLockScreen
import com.example.ui.screens.WelcomeScreen
import com.example.viewmodel.VaultViewModel

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object Home : Screen("home")
    data object Records : Screen("records?category={category}") {
        fun createRoute(category: String? = null): String {
            return if (category != null) "records?category=$category" else "records"
        }
    }
    data object AddRecord : Screen("add_record")
    data object EditRecord : Screen("edit_record/{recordId}") {
        fun createRoute(recordId: String) = "edit_record/$recordId"
    }
    data object Details : Screen("details/{recordId}") {
        fun createRoute(recordId: String) = "details/$recordId"
    }
    data object Settings : Screen("settings")
}

@Composable
fun VaultNavGraph(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val isOnboardingCompleted by viewModel.preferences.onboardingCompleted.collectAsStateWithLifecycle()
    val isAppLockEnabled by viewModel.preferences.appLockEnabled.collectAsStateWithLifecycle()
    val isSessionUnlocked by viewModel.isSessionUnlocked.collectAsStateWithLifecycle()

    if (isAppLockEnabled && !isSessionUnlocked) {
        VaultLockScreen(viewModel = viewModel, modifier = modifier)
        return
    }

    val startDestination = if (isOnboardingCompleted) Screen.Home.route else Screen.Welcome.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onContinue = {
                    viewModel.completeOnboarding()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onSeedDataAndContinue = {
                    viewModel.seedSampleRecords()
                    viewModel.completeOnboarding()
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToAllRecords = { category ->
                    viewModel.onCategoryFilterChange(category)
                    navController.navigate(Screen.Records.createRoute(category))
                },
                onNavigateToAddRecord = {
                    navController.navigate(Screen.AddRecord.route)
                },
                onNavigateToRecordDetails = { recordId ->
                    navController.navigate(Screen.Details.createRoute(recordId))
                },
                onNavigateToEditRecord = { recordId ->
                    navController.navigate(Screen.EditRecord.createRoute(recordId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onLockApp = {
                    viewModel.lockVault()
                }
            )
        }

        composable(
            route = Screen.Records.route,
            arguments = listOf(
                navArgument("category") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val initialCategory = backStackEntry.arguments?.getString("category")
            RecordsListScreen(
                viewModel = viewModel,
                initialCategory = initialCategory,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddRecord = { navController.navigate(Screen.AddRecord.route) },
                onNavigateToRecordDetails = { recordId ->
                    navController.navigate(Screen.Details.createRoute(recordId))
                },
                onNavigateToEditRecord = { recordId ->
                    navController.navigate(Screen.EditRecord.createRoute(recordId))
                }
            )
        }

        composable(Screen.AddRecord.route) {
            AddEditRecordScreen(
                viewModel = viewModel,
                recordIdToEdit = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditRecord.route,
            arguments = listOf(
                navArgument("recordId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getString("recordId")
            AddEditRecordScreen(
                viewModel = viewModel,
                recordIdToEdit = recordId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Details.route,
            arguments = listOf(
                navArgument("recordId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getString("recordId") ?: ""
            RecordDetailsScreen(
                viewModel = viewModel,
                recordId = recordId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { editId ->
                    navController.navigate(Screen.EditRecord.createRoute(editId))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onLockAppNow = { viewModel.lockVault() }
            )
        }
    }
}
