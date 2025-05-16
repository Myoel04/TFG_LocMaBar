package com.example.locmabar.vista

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.locmabar.modelo.Comentario
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.tasks.await

@Composable
fun AdminComentarios(navController: NavHostController) {
    var comentarios by remember { mutableStateOf<List<Comentario>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var errorMensaje by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Cargar comentarios pendientes desde Firebase
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val querySnapshot = FirebaseFirestore.getInstance()
                    .collection("ComentariosPendientes")
                    .whereEqualTo("estado", "PENDIENTE")
                    .get()
                    .await()
                comentarios = querySnapshot.toObjects(Comentario::class.java)
            } catch (e: Exception) {
                errorMensaje = "Error al cargar comentarios: ${e.message}"
            } finally {
                cargando = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Comentarios Pendientes",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (cargando) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Cargando comentarios...", fontSize = 14.sp)
        } else {
            if (errorMensaje.isNotEmpty()) {
                Text(
                    text = errorMensaje,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            if (comentarios.isEmpty()) {
                Text(
                    text = "No hay comentarios pendientes.",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(comentarios) { comentario ->
                        Card(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth()
                                .clickable {
                                    navController.navigate("adminDatosComentarios/${comentario.id}")
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = comentario.texto,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Usuario: ${comentario.usuarioId}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Fecha: ${
                                        comentario.fechaCreacion?.toDate()?.let {
                                            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                                        } ?: "Desconocida"
                                    }",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}