package com.enjoy.app

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun EnjoyApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "map") {
        composable("map") { MapScreen(navController) }
        composable("history") { HistoryScreen(navController) }
    }
}
