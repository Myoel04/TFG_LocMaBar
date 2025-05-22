package com.example.locmabar.controlador

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.locmabar.vista.AdminComentarios
import com.example.locmabar.vista.AdminDatosComentarios
import com.example.locmabar.vista.AdminDatosSolicitudes
import com.example.locmabar.vista.AdminDatosUsuarios
import com.example.locmabar.vista.AdminSolicitudes
import com.example.locmabar.vista.AdminUsuarios
import com.example.locmabar.vista.AdminComentariosAprobados
import com.example.locmabar.vista.AdminLugares
import com.example.locmabar.vista.Login
import com.example.locmabar.vista.Registro
import com.example.locmabar.vista.SolicitudNuevo
import com.example.locmabar.vista.VentHomeAdmin
import com.example.locmabar.vista.Ventana2
import com.example.locmabar.vista.VentanaDetalles
import com.example.locmabar.vista.VentanaPerfil
import java.net.URLDecoder

@Composable
fun Navigation(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = "login", modifier = modifier) {
        composable("login") { Login(navController = navController) }
        composable("ventana2") { Ventana2(navController = navController) }
        composable("ventHomeAdmin") { VentHomeAdmin(navController = navController) }
        composable("ventanaPerfil") { VentanaPerfil(navController = navController) }
        composable("registro") { Registro(navController = navController) }
        composable(
            route = "detallesBar/{lugarId}?latitudUsuario={latitudUsuario}&longitudUsuario={longitudUsuario}",
            arguments = listOf(
                navArgument("lugarId") { type = NavType.StringType },
                navArgument("latitudUsuario") { type = NavType.StringType; nullable = true },
                navArgument("longitudUsuario") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val lugarId = backStackEntry.arguments?.getString("lugarId") ?: ""
            val latitudUsuarioStr = backStackEntry.arguments?.getString("latitudUsuario")
            val longitudUsuarioStr = backStackEntry.arguments?.getString("longitudUsuario")
            val latitudUsuario = latitudUsuarioStr?.toDoubleOrNull() ?: 0.0
            val longitudUsuario = longitudUsuarioStr?.toDoubleOrNull() ?: 0.0

            VentanaDetalles(
                navController = navController,
                lugarId = lugarId,
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
        composable("adminSolicitudes") { // Eliminado el argumento tab
            AdminSolicitudes(navController = navController) // Eliminado initialTab
        }
        composable("adminComentarios") { AdminComentarios(navController = navController) }
        composable(
            route = "adminDatosComentarios/{comentarioId}",
            arguments = listOf(
                navArgument("comentarioId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val comentarioId = backStackEntry.arguments?.getString("comentarioId") ?: ""
            AdminDatosComentarios(navController = navController, comentarioId = comentarioId)
        }
        composable(
            route = "adminDatosSolicitudes/{solicitudId}",
            arguments = listOf(
                navArgument("solicitudId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val solicitudId = backStackEntry.arguments?.getString("solicitudId") ?: ""
            AdminDatosSolicitudes(navController = navController, solicitudId = solicitudId)
        }
        composable("adminUsuarios") { AdminUsuarios(navController = navController) }
        composable(
            route = "adminDatosUsuarios/{usuarioId}",
            arguments = listOf(
                navArgument("usuarioId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val usuarioId = backStackEntry.arguments?.getString("usuarioId") ?: ""
            AdminDatosUsuarios(navController = navController, usuarioId = usuarioId)
        }
        composable("adminComentariosAprobados") { AdminComentariosAprobados(navController = navController) }
        composable("adminLugares") { AdminLugares(navController = navController) }
    }
}