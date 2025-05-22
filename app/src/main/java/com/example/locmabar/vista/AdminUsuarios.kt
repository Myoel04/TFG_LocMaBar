package com.example.locmabar.vista

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.locmabar.modelo.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AdminUsuarios(navController: NavHostController) {
    var usuarios by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var errorMensaje by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Cargar usuarios desde Firebase
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val querySnapshot = FirebaseFirestore.getInstance()
                    .collection("Usuarios")
                    .get()
                    .await()
                usuarios = querySnapshot.documents.mapNotNull { doc ->
                    val usuario = doc.toObject(Usuario::class.java)
                    usuario?.copy(uid = doc.id)
                }
            } catch (e: Exception) {
                errorMensaje = "Error al cargar usuarios: ${e.message}"
            } finally {
                cargando = false
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(90.dp)) // Espacio para el encabezado

            Text(
                text = "Usuarios",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (cargando) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Cargando usuarios...", fontSize = 14.sp)
            } else {
                if (errorMensaje.isNotEmpty()) {
                    Text(
                        text = errorMensaje,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                if (usuarios.isEmpty()) {
                    Text(
                        text = "No hay usuarios registrados.",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(usuarios) { usuario ->
                            Card(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("adminDatosUsuarios/${usuario.uid}")
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = usuario.nombre,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Email: ${usuario.email}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Rol: ${usuario.rol}",
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
                            navController.navigate("adminSolicitudes?tab=3")
                        }
                )
            }
        }
    }
}