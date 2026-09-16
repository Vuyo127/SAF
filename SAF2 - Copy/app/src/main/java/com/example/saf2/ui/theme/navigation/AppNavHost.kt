package com.example.saf2.ui.theme.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.saf2.ui.theme.screen.AddListingScreen
import com.example.saf2.ui.theme.screen.DetailScreen
import com.example.saf2.ui.theme.screen.ForgotPasswordScreen
import com.example.saf2.ui.theme.screen.LoginScreen
import com.example.saf2.ui.theme.screen.OwnerDashboardScreen
import com.example.saf2.ui.theme.screen.ProfileScreen
import com.example.saf2.ui.theme.screen.ReportScreen
import com.example.saf2.ui.theme.screen.ReviewScreen
import com.example.saf2.ui.theme.screen.SignUpScreen
import com.example.saf2.ui.theme.screen.AccommodationScreen
import com.example.saf2.ui.theme.screen.AccommodationScreen1
import com.example.saf2.ui.theme.screen.OwnerProfile

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "accommodation1",
        modifier = modifier
    ) {

        composable("accommodation1") {
            AccommodationScreen1(navController)
        }

        composable("accommodation") {
            AccommodationScreen(navController)
        }

        composable("login") {
            LoginScreen(navController)
        }

        composable("signup") {
            SignUpScreen(navController)
        }

        composable("forgotPassword") {
            ForgotPasswordScreen(navController)
        }


        composable(
            route = "detail/{listingId}",
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            DetailScreen(navController, listingId)
        }


        composable(
            route = "report/{listingId}",
            arguments = listOf(navArgument("listingId") { type = NavType.StringType })
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            ReportScreen(navController, listingId)
        }

        composable(
            route = "review/{listingId}/{propertyTitle}",
            arguments = listOf(
                navArgument("listingId") { type = NavType.StringType },
                navArgument("propertyTitle") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val listingId = backStackEntry.arguments?.getString("listingId") ?: ""
            val propertyTitle = backStackEntry.arguments?.getString("propertyTitle") ?: ""
            ReviewScreen(navController, listingId, propertyTitle)
        }

        composable("addListing") {
            AddListingScreen(navController)
        }

        composable("ownerDashboard") {
            OwnerDashboardScreen(navController)
        }

        composable("profile") {
            ProfileScreen(navController)
        }

        composable("ownerprofile") {
            OwnerProfile(navController)
        }
    }
}
