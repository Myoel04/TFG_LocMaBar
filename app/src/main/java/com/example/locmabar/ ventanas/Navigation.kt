package com.example.locmabar.navegacion

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.locmabar.ventanas.Login
import com.example.locmabar.ventanas.Registro
import com.example.locmabar.ventanas.Ventana2

@Composable
fun Navigation(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = "login") {
        composable("login") { Login(navController = navController) }
        composable("ventana2") { Ventana2(navController = navController) }
        composable("registro") {
            Registro(navController = navController)
        }
    }
}