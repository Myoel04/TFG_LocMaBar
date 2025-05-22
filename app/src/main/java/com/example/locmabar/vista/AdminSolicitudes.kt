package com.example.locmabar.vista

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.locmabar.modelo.SolicitudRestaurante
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSolicitudes(navController: NavHostController) {
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser
    var isAdmin by remember { mutableStateOf(false) }
    var cargandoAdmin by remember { mutableStateOf(true) }
    var cargando by remember { mutableStateOf(true) }
    var errorMensaje by remember { mutableStateOf("") }

    // Estados para datos
    var solicitudes by remember { mutableStateOf(listOf<SolicitudRestaurante>()) }

    val contexto = LocalContext.current

    // Coroutine scope para manejar la carga de datos
    val coroutineScope = rememberCoroutineScope()

    // Verificar si el usuario es administrador y cargar datos
    LaunchedEffect(user) {
        if (user == null) {
            errorMensaje = "Debes iniciar sesión como administrador."
            cargandoAdmin = false
            return@LaunchedEffect
        }

        coroutineScope.launch {
            try {
                // Verificar si el usuario es administrador
                val userDoc = FirebaseFirestore.getInstance()
                    .collection("Usuarios")
                    .document(user.uid)
                    .get()
                    .await()
                isAdmin = userDoc.getString("rol") == "admin"
                if (!isAdmin) {
                    errorMensaje = "Acceso denegado. Solo para administradores."
                    cargandoAdmin = false
                    return@launch
                }

                // Cargar solicitudes
                val solicitudesSnapshot = FirebaseFirestore.getInstance()
                    .collection("Solicitudes")
                    .whereEqualTo("estado", "PENDIENTE")
                    .get()
                    .await()
                solicitudes = solicitudesSnapshot.documents.mapNotNull { document ->
                    try {
                        val solicitud = document.toObject(SolicitudRestaurante::class.java)?.copy(id = document.id)
                        if (solicitud != null && solicitud.isValid()) {
                            println("Solicitud cargada: ID=${solicitud.id}, Nombre=${solicitud.nombre}")
                            solicitud
                        } else {
                            println("No se pudo convertir el documento ${document.id} a SolicitudRestaurante: objeto no válido")
                            null
                        }
                    } catch (e: Exception) {
                        println("Error al convertir el documento ${document.id} a SolicitudRestaurante: ${e.message}")
                        null
                    }
                }

                cargando = false
                cargandoAdmin = false
            } catch (e: Exception) {
                errorMensaje = "Error al cargar datos: ${e.message}"
                cargando = false
                cargandoAdmin = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Encabezado (igual que en VentHomeAdmin)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(90.dp)
                .background(Color.DarkGray)
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(90.dp)) // Espacio para el encabezado

            if (cargando) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Cargando solicitudes...", fontSize = 14.sp)
            } else {
                if (errorMensaje.isNotEmpty()) {
                    Text(
                        text = errorMensaje,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                if (solicitudes.isEmpty()) {
                    Text(
                        text = "No hay solicitudes pendientes.",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f) // Para que ocupe el espacio disponible
                    ) {
                        items(solicitudes) { solicitud ->
                            Card(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("adminDatosSolicitudes/${solicitud.id}")
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = solicitud.nombre ?: "Sin nombre",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Dirección: ${solicitud.direccion ?: "Sin dirección"}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${solicitud.municipio ?: "Sin municipio"}, ${solicitud.provincia ?: "Sin provincia"}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Barra de navegación inferior (igual que en VentHomeAdmin)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .requiredHeight(63.dp)
                .background(Color.LightGray)
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
                            navController.navigate("adminSolicitudes")
                        }
                )
            }
        }
    }
}