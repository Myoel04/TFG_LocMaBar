package com.example.locmabar.vista

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VentanaDetalles(
    navController: NavController,
    lugarId: String,
    latitudUsuario: Double?,
    longitudUsuario: Double?
) {
    val scope = rememberCoroutineScope()
    var lugar by remember { mutableStateOf<Lugar?>(null) }
    var comentarios by remember { mutableStateOf<List<Comentario>>(emptyList()) }
    var usuariosMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var cargando by remember { mutableStateOf(true) }
    var errorMensaje by remember { mutableStateOf("") }
    var mensajeExito by remember { mutableStateOf("") }
    var debugMensaje by remember { mutableStateOf("") } // Para depuración

    // Estados para el formulario de comentarios
    var nuevoComentario by remember { mutableStateOf("") }
    var nuevaValoracion by remember { mutableStateOf(0f) }
    var cargandoComentario by remember { mutableStateOf(false) }

    // Estado para el desplazamiento
    val scrollState = rememberScrollState()

    // Cargar datos del local desde Firestore
    LaunchedEffect(lugarId) {
        scope.launch {
            try {
                println("Attempting to load lugar with ID: $lugarId from collection 'Locales'")
                val documentSnapshot = FirebaseFirestore.getInstance()
                    .collection("Locales")
                    .document(lugarId)
                    .get()
                    .await()
                if (documentSnapshot.exists()) {
                    lugar = documentSnapshot.toObject(Lugar::class.java)
                    if (lugar == null || !lugar!!.isValid()) {
                        errorMensaje = "No se pudo convertir el documento a objeto Lugar válido para ID: $lugarId"
                    } else {
                        println("Lugar loaded successfully: ${lugar?.nombre}")
                    }
                } else {
                    errorMensaje = "No se encontró el local con ID: $lugarId en la colección 'Locales'"
                }
            } catch (e: Exception) {
                errorMensaje = "Error al cargar el local: ${e.message}"
            } finally {
                cargando = false
            }
        }
    }

    // Cargar comentarios aprobados desde Firebase
    LaunchedEffect(lugarId) {
        scope.launch {
            try {
                println("Attempting to load comments for lugarId: $lugarId")
                val querySnapshot = FirebaseFirestore.getInstance()
                    .collection("Comentarios")
                    .whereEqualTo("lugarId", lugarId)
                    .whereEqualTo("estado", "APROBADO")
                    .get()
                    .await()

                println("Query returned ${querySnapshot.size()} documents")
                querySnapshot.documents.forEach { doc ->
                    println("Document: ${doc.id} -> ${doc.data}")
                }

                comentarios = querySnapshot.toObjects(Comentario::class.java)
                debugMensaje = "Se encontraron ${querySnapshot.size()} comentarios aprobados."

                // Cargar nombres de usuarios para los comentarios
                val usuarioIds = comentarios.map { it.usuarioId }.distinct()
                val usuarios = mutableMapOf<String, String>()
                for (usuarioId in usuarioIds) {
                    try {
                        val userDoc = FirebaseFirestore.getInstance()
                            .collection("Usuarios")
                            .document(usuarioId)
                            .get(Source.SERVER)
                            .await()
                        val nombre = userDoc.get("nombre") as String?
                        usuarios[usuarioId] = nombre ?: "Usuario Desconocido"
                        println("Loaded user $usuarioId: $nombre")
                    } catch (e: Exception) {
                        usuarios[usuarioId] = "Usuario Desconocido"
                        println("Error al cargar nombre de usuario $usuarioId: ${e.message}")
                    }
                }
                usuariosMap = usuarios
            } catch (e: Exception) {
                errorMensaje = if (e.message?.contains("PERMISSION_DENIED") == true) {
                    "No tienes permiso para ver los comentarios de este lugar."
                } else {
                    "Error al cargar comentarios: ${e.message}"
                }
                debugMensaje = "Error al cargar comentarios: ${e.message}"
            }
        }
    }

    // Envolver el Column en un Modifier.verticalScroll para habilitar el desplazamiento
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState), // Habilitar desplazamiento vertical
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (cargando) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Cargando datos del local...", fontSize = 14.sp)
        } else if (errorMensaje.isNotEmpty()) {
            Text(
                text = errorMensaje,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Volver")
            }
        } else if (lugar != null && lugar!!.isValid()) {
            // Configuración del mapa
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(LatLng(lugar!!.latitudDouble!!, lugar!!.longitudDouble!!), 15f)
            }

            // Título: Nombre del bar
            Text(
                text = lugar!!.nombre!!,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Mapa de Google
            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = latitudUsuario != null && longitudUsuario != null),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = true,
                    scrollGesturesEnabled = true,
                    zoomGesturesEnabled = true,
                    tiltGesturesEnabled = true,
                    rotationGesturesEnabled = true
                )
            ) {
                Marker(
                    state = MarkerState(position = LatLng(lugar!!.latitudDouble!!, lugar!!.longitudDouble!!)),
                    title = lugar!!.nombre,
                    snippet = lugar!!.direccion
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Dirección
            Text(
                text = "Dirección: ${lugar!!.direccion}",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Distancia (si se tienen las coordenadas del usuario)
            if (latitudUsuario != null && longitudUsuario != null) {
                val distancia = calcularDistancia(latitudUsuario, longitudUsuario, lugar!!.latitudDouble!!, lugar!!.longitudDouble!!)
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
                text = "Teléfono: ${lugar!!.telefono ?: "No disponible"}",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Horario (si está disponible)
            Text(
                text = "Horario: ${lugar!!.horario ?: "No disponible"}",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Valoración (si está disponible)
            Text(
                text = "Valoración: ${lugar!!.valoracion ?: "No disponible"}",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Botón para abrir en Google Maps
            Button(
                onClick = {
                    val uri = Uri.parse("geo:${lugar!!.latitud},${lugar!!.longitud}?q=${lugar!!.latitud},${lugar!!.longitud}(${lugar!!.nombre})")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.setPackage("com.google.android.apps.maps")
                    try {
                        navController.context.startActivity(intent)
                    } catch (e: Exception) {
                        println("Error opening Google Maps: ${e.message}")
                    }
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

            // Selector de valoración con estrellas
            Text(
                text = "Valoración (opcional, toca las estrellas):",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                for (i in 1..5) {
                    Icon(
                        imageVector = if (i <= nuevaValoracion) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "$i estrellas",
                        tint = if (i <= nuevaValoracion) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                nuevaValoracion = i.toFloat()
                            }
                    )
                }
            }

            if (cargandoComentario) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        if (nuevoComentario.isBlank()) {
                            errorMensaje = "El comentario no puede estar vacío"
                            return@Button
                        }

                        // Validar que la valoración esté entre 1 y 5 (si se ingresó)
                        if (nuevaValoracion > 0 && (nuevaValoracion < 1 || nuevaValoracion > 5)) {
                            errorMensaje = "La valoración debe estar entre 1 y 5"
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
                                    .document()
                                    .id

                                val nuevoComentarioData = Comentario(
                                    id = comentarioId,
                                    texto = nuevoComentario,
                                    usuarioId = usuarioActual.uid,
                                    lugarId = lugarId,
                                    estado = "PENDIENTE",
                                    fechaCreacion = Timestamp.now(),
                                    valoracion = if (nuevaValoracion > 0) "${nuevaValoracion}/5" else null
                                )

                                FirebaseFirestore.getInstance()
                                    .collection("ComentariosPendientes")
                                    .document(comentarioId)
                                    .set(nuevoComentarioData)
                                    .await()

                                mensajeExito = "Comentario enviado. Será revisado por un administrador."
                                nuevoComentario = ""
                                nuevaValoracion = 0f
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

            // Mensaje de depuración
            if (debugMensaje.isNotEmpty()) {
                Text(
                    text = debugMensaje,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp) // Limitar la altura para que no ocupe demasiado espacio
                    ) {
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
                                        text = "Usuario: ${usuariosMap[comentario.usuarioId] ?: "Usuario Desconocido"}",
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

            // Añadir un Spacer al final para asegurar que el contenido sea completamente desplazable
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}