package com.agustinazorin.finanzas.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.agustinazorin.finanzas.feature.account.ui.AccountsScreen
import com.agustinazorin.finanzas.feature.capture.ui.CaptureReviewScreen
import com.agustinazorin.finanzas.feature.capture.ui.CaptureSettingsScreen
import com.agustinazorin.finanzas.feature.category.ui.CategoriesScreen
import com.agustinazorin.finanzas.feature.category.ui.CategoryRulesScreen
import com.agustinazorin.finanzas.feature.creditcard.ui.CreditCardDetailScreen
import com.agustinazorin.finanzas.feature.home.ui.HomeScreen
import com.agustinazorin.finanzas.feature.household.ui.HouseholdMembersScreen
import com.agustinazorin.finanzas.feature.household.ui.HouseholdReportScreen
import com.agustinazorin.finanzas.feature.income.ui.IncomeScreen
import com.agustinazorin.finanzas.feature.more.ui.MoreScreen
import com.agustinazorin.finanzas.feature.recurring.ui.RecurringScreen
import com.agustinazorin.finanzas.feature.summary.ui.SummaryScreen
import com.agustinazorin.finanzas.feature.transaction.ui.QuickAddScreen
import com.agustinazorin.finanzas.feature.transaction.ui.TransactionsScreen

private val FAB_DESTINATIONS = setOf(FinanzasDestination.Home.route, FinanzasDestination.Transactions.route)

@Composable
fun FinanzasNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                FinanzasDestination.bottomBarDestinations.forEach { destination ->
                    val selected = currentRoute?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = stringResource(destination.labelRes)) },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (currentRoute?.route?.let { it in FAB_DESTINATIONS } == true) {
                FloatingActionButton(onClick = { navController.navigate(SecondaryRoutes.QUICK_ADD) }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = FinanzasDestination.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(FinanzasDestination.Home.route) { HomeScreen() }
            composable(FinanzasDestination.Transactions.route) { TransactionsScreen() }
            composable(FinanzasDestination.Accounts.route) { AccountsScreen(onNavigate = { navController.navigate(it) }) }
            composable(FinanzasDestination.Summary.route) { SummaryScreen() }
            composable(FinanzasDestination.More.route) { MoreScreen(onNavigate = { navController.navigate(it) }) }

            composable(SecondaryRoutes.QUICK_ADD) { QuickAddScreen(onDone = { navController.popBackStack() }) }
            composable(SecondaryRoutes.CATEGORIES) { CategoriesScreen() }
            composable(SecondaryRoutes.HOUSEHOLD_MEMBERS) { HouseholdMembersScreen() }
            composable(SecondaryRoutes.HOUSEHOLD_REPORT) { HouseholdReportScreen() }
            composable(SecondaryRoutes.INCOME) { IncomeScreen() }
            composable(SecondaryRoutes.RECURRING) { RecurringScreen() }
            composable(SecondaryRoutes.CAPTURE_SETTINGS) { CaptureSettingsScreen() }
            composable(SecondaryRoutes.CAPTURE_REVIEW) { CaptureReviewScreen() }
            composable(SecondaryRoutes.CATEGORY_RULES) { CategoryRulesScreen() }
            composable(
                SecondaryRoutes.CREDIT_CARD_DETAIL_PATTERN,
                arguments = listOf(navArgument("accountId") { type = NavType.LongType }),
            ) { CreditCardDetailScreen() }
        }
    }
}
