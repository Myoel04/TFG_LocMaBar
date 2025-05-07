package com.example.locmabar.navegacion

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.locmabar.vista.Login
import com.example.locmabar.vista.Registro
import com.example.locmabar.vista.Ventana2
import com.example.locmabar.vista.VentanaDetalles
import com.example.locmabar.vista.SolicitudNuevo
import com.example.locmabar.vista.AdminSolicitudes
import com.google.gson.Gson
import java.net.URLDecoder

@Composable
fun Navigation(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = "login", modifier = modifier) {
        composable("login") { Login(navController = navController) }
        composable("ventana2") { Ventana2(navController = navController) }
        composable("registro") { Registro(navController = navController) }
        composable(
            route = "detallesBar/{lugarJson}/{latitudUsuario}/{longitudUsuario}",
            arguments = listOf(
                navArgument("lugarJson") { type = NavType.StringType },
                navArgument("latitudUsuario") { type = NavType.FloatType },
                navArgument("longitudUsuario") { type = NavType.FloatType }
            )
        ) { backStackEntry ->
            val lugarJson = backStackEntry.arguments?.getString("lugarJson") ?: ""
            val decodedLugarJson = URLDecoder.decode(lugarJson, "UTF-8")
            val lugar = Gson().fromJson(decodedLugarJson, com.example.locmabar.modelo.Lugar::class.java)
            val latitudUsuario = backStackEntry.arguments?.getFloat("latitudUsuario")?.toDouble()
            val longitudUsuario = backStackEntry.arguments?.getFloat("longitudUsuario")?.toDouble()

            VentanaDetalles(
                navController = navController,
                lugar = lugar,
                latitudUsuario = if (latitudUsuario != 0.0) latitudUsuario else null,
                longitudUsuario = if (longitudUsuario != 0.0) longitudUsuario else null
            )
        }
        composable(
            route = "solicitarRestaurante/{comunidadesJson}",
            arguments = listOf(
                navArgument("comunidadesJson") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val comunidadesJson = backStackEntry.arguments?.getString("comunidadesJson") ?: ""
            val decodedComunidadesJson = URLDecoder.decode(comunidadesJson, "UTF-8")
            SolicitudNuevo(
                navController = navController,
                comunidadesJson = decodedComunidadesJson
            )
        }
        composable("adminSolicitudes") { AdminSolicitudes(navController = navController) }
    }
}