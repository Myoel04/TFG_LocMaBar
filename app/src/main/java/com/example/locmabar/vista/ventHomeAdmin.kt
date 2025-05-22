package com.example.locmabar.vista

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun VentHomeAdmin(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    var isAdmin by remember { mutableStateOf(false) }
    var cargandoAdmin by remember { mutableStateOf(true) }
    var errorMensaje by remember { mutableStateOf("") }

    // Coroutine scope para manejar la carga de datos
    val coroutineScope = rememberCoroutineScope()

    // Verificar si el usuario es administrador
    LaunchedEffect(user) {
        if (user == null) {
            errorMensaje = "Debes iniciar sesión como administrador."
            cargandoAdmin = false
            return@LaunchedEffect
        }

        coroutineScope.launch {
            try {
                val userDoc = FirebaseFirestore.getInstance()
                    .collection("Usuarios")
                    .document(user.uid)
                    .get()
                    .await()
                isAdmin = userDoc.getString("rol") == "admin"
                if (!isAdmin) {
                    errorMensaje = "Acceso denegado. Solo para administradores."
                }
                cargandoAdmin = false
            } catch (e: Exception) {
                errorMensaje = "Error al verificar permisos: ${e.message}"
                cargandoAdmin = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Rectángulo de fondo del encabezado (Rectangle15) - Reemplazado por Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(90.dp)
                .background(Color.DarkGray) // Color temporal hasta que añadas la imagen
        )

        // Texto "LOCMABAR" en el encabezado
        Text(
            text = "LOCMABAR",
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 1.43.em,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 33.dp)
                .wrapContentHeight(align = Alignment.CenterVertically)
        )

        if (cargandoAdmin) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
            Text(
                text = "Verificando permisos...",
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 40.dp)
            )
            return@Box
        }

        if (user == null || !isAdmin) {
            Text(
                text = "Acceso denegado. Solo para administradores.",
                color = MaterialTheme.colorScheme.error,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
            Button(
                onClick = { navController.navigate("login") },
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 40.dp)
                    .fillMaxWidth(0.8f)
            ) {
                Text("Iniciar Sesión")
            }
            return@Box
        }

        // Botón "Solicitudes"
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 137.dp)
                .requiredWidth(351.dp)
                .requiredHeight(62.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color(0xFF4CAF50)) // Ajusta el color según tu tema
                .clickable { navController.navigate("adminSolicitudes") } // Eliminado ?tab=3
        ) {
            Text(
                text = "Solicitudes",
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 1.43.em,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .wrapContentHeight(align = Alignment.CenterVertically)
            )
        }

        // Botón "Usuarios"
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 227.dp)
                .requiredWidth(351.dp)
                .requiredHeight(62.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color(0xFF4CAF50)) // Ajusta el color según tu tema
                .clickable { navController.navigate("adminUsuarios") }
        ) {
            Text(
                text = "Usuarios",
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 1.43.em,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .wrapContentHeight(align = Alignment.CenterVertically)
            )
        }

        // Botón "Locales"
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 326.dp)
                .requiredWidth(351.dp)
                .requiredHeight(62.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color(0xFF4CAF50)) // Ajusta el color según tu tema
                .clickable { navController.navigate("adminLugares") }
        ) {
            Text(
                text = "Locales",
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 1.43.em,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .wrapContentHeight(align = Alignment.CenterVertically)
            )
        }

        // Botón "Comentarios"
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 427.dp)
                .requiredWidth(351.dp)
                .requiredHeight(62.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color(0xFF4CAF50)) // Ajusta el color según tu tema
                .clickable { navController.navigate("adminComentarios") }
        ) {
            Text(
                text = "Comentarios",
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 1.43.em,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.Center)
                    .wrapContentHeight(align = Alignment.CenterVertically)
            )
        }

        // Barra de navegación inferior (Rectangle16) - Reemplazado por Box
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .requiredHeight(63.dp)
                .background(Color.LightGray) // Color temporal hasta que añadas la imagen
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono "Home" (Volver a ventHomeAdmin)
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home",
                    tint = Color.Black,
                    modifier = Modifier
                        .requiredSize(48.dp)
                        .clickable {
                            navController.navigate("ventHomeAdmin") {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = false
                                }
                            }
                        }
                )

                // Icono "Image Avatar" (AdminUsuarios)
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Usuarios",
                    tint = Color.Black,
                    modifier = Modifier
                        .requiredSize(48.dp)
                        .clickable {
                            navController.navigate("adminUsuarios")
                        }
                )

                // Icono "Message Square" (AdminComentarios)
                Icon(
                    imageVector = Icons.Default.Message,
                    contentDescription = "Comentarios",
                    tint = Color.Black,
                    modifier = Modifier
                        .requiredSize(48.dp)
                        .clickable {
                            navController.navigate("adminComentarios")
                        }
                )

                // Icono "Pencil 01" (AdminSolicitudes)
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Solicitudes",
                    tint = Color.Black,
                    modifier = Modifier
                        .requiredSize(48.dp)
                        .clickable {
                            navController.navigate("adminSolicitudes") // Eliminado ?tab=3
                        }
                )
            }
        }
    }
}