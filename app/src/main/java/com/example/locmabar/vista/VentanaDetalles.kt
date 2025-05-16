package com.example.locmabar.vista

import android.content.Intent
import android.net.Uri
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
import androidx.navigation.NavController
import com.example.locmabar.modelo.Comentario
import com.example.locmabar.modelo.Lugar
import com.example.locmabar.utils.calcularDistancia
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VentanaDetalles(
    navController: NavController,
    lugar: Lugar,
    latitudUsuario: Double?,
    longitudUsuario: Double?
) {
    val scope = rememberCoroutineScope()
    var comentarios by remember { mutableStateOf<List<Comentario>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var errorMensaje by remember { mutableStateOf("") }
    var mensajeExito by remember { mutableStateOf("") }

    // Estados para el formulario de comentarios
    var nuevoComentario by remember { mutableStateOf("") }
    var nuevaValoracion by remember { mutableStateOf("") }
    var cargandoComentario by remember { mutableStateOf(false) }

    // Cargar comentarios aprobados desde Firebase
    LaunchedEffect(lugar.id) {
        scope.launch {
            try {
                val querySnapshot = FirebaseFirestore.getInstance()
                    .collection("Comentarios")
                    .whereEqualTo("lugarId", lugar.id)
                    .whereEqualTo("estado", "APROBADO")
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
        // Título: Nombre del bar
        Text(
            text = lugar.nombre,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Dirección
        Text(
            text = "Dirección: ${lugar.direccion}",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Distancia (si se tienen las coordenadas del usuario)
        if (latitudUsuario != null && longitudUsuario != null) {
            val distancia = calcularDistancia(latitudUsuario, longitudUsuario, lugar.latitud, lugar.longitud)
            Text(
                text = "Distancia: %.2f km".format(distancia),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            Text(
                text = "Distancia: No disponible",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Teléfono (si está disponible)
        Text(
            text = "Teléfono: ${lugar.telefono ?: "No disponible"}",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Horario (si está disponible)
        Text(
            text = "Horario: ${lugar.horario ?: "No disponible"}",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Valoración (si está disponible)
        Text(
            text = "Valoración: ${lugar.valoracion ?: "No disponible"}",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Botón para abrir en Google Maps
        Button(
            onClick = {
                val uri = Uri.parse("geo:${lugar.latitud},${lugar.longitud}?q=${lugar.latitud},${lugar.longitud}(${lugar.nombre})")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.setPackage("com.google.android.apps.maps")
                navController.context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(
                text = "Abrir en Google Maps",
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        // Formulario para agregar un nuevo comentario
        Text(
            text = "Agregar Comentario",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        OutlinedTextField(
            value = nuevoComentario,
            onValueChange = { nuevoComentario = it },
            label = { Text("Escribe tu comentario") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            ),
            singleLine = false
        )

        OutlinedTextField(
            value = nuevaValoracion,
            onValueChange = { nuevaValoracion = it },
            label = { Text("Valoración (opcional, ej. 4.5/5)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            ),
            singleLine = true
        )

        if (cargandoComentario) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (nuevoComentario.isBlank()) {
                        errorMensaje = "El comentario no puede estar vacío"
                        return@Button
                    }

                    cargandoComentario = true
                    scope.launch {
                        try {
                            val usuarioActual = FirebaseAuth.getInstance().currentUser
                            if (usuarioActual == null) {
                                errorMensaje = "Debes iniciar sesión para comentar"
                                cargandoComentario = false
                                return@launch
                            }

                            val comentarioId = FirebaseFirestore.getInstance()
                                .collection("ComentariosPendientes")
                                .document().id

                            val nuevoComentarioData = Comentario(
                                id = comentarioId,
                                texto = nuevoComentario,
                                usuarioId = usuarioActual.uid,
                                lugarId = lugar.id,
                                estado = "PENDIENTE",
                                fechaCreacion = Timestamp.now(),
                                valoracion = nuevaValoracion.ifBlank { null }
                            )

                            FirebaseFirestore.getInstance()
                                .collection("ComentariosPendientes")
                                .document(comentarioId)
                                .set(nuevoComentarioData)
                                .await()

                            mensajeExito = "Comentario enviado. Será revisado por un administrador."
                            nuevoComentario = ""
                            nuevaValoracion = ""
                        } catch (e: Exception) {
                            errorMensaje = "Error al enviar comentario: ${e.message}"
                        } finally {
                            cargandoComentario = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "Enviar Comentario",
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        if (mensajeExito.isNotEmpty()) {
            Text(
                text = mensajeExito,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Sección de comentarios aprobados
        Text(
            text = "Comentarios",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (cargando) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cargando comentarios...",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
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
                    text = "No hay comentarios aprobados para este lugar.",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(comentarios) { comentario ->
                        Card(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = comentario.texto,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
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
                                comentario.valoracion?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Valoración: $it",
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

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text(
                text = "Volver",
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}