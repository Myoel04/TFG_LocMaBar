package com.example.locmabar.vista

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.locmabar.modelo.Lugar
import com.example.locmabar.modelo.LugarRepository
import com.example.locmabar.modelo.SolicitudRestaurante
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDatosSolicitudes(
    navController: NavHostController,
    solicitudId: String
) {
    var solicitud by remember { mutableStateOf<SolicitudRestaurante?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var errorMensaje by remember { mutableStateOf("") }
    var mensajeExito by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Cargar datos de la solicitud desde Firebase
    LaunchedEffect(solicitudId) {
        scope.launch {
            try {
                val documentSnapshot = FirebaseFirestore.getInstance()
                    .collection("Solicitudes")
                    .document(solicitudId)
                    .get()
                    .await()
                solicitud = documentSnapshot.toObject(SolicitudRestaurante::class.java)
            } catch (e: Exception) {
                errorMensaje = "Error al cargar solicitud: ${e.message}"
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
            text = "Detalles de la Solicitud",
            fontSize = 20.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (cargando) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Cargando detalles...", fontSize = 14.sp)
        } else {
            if (errorMensaje.isNotEmpty()) {
                Text(
                    text = errorMensaje,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else if (mensajeExito) {
                Text(
                    text = "Solicitud actualizada con éxito.",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else if (solicitud != null) {
                if (!solicitud!!.isValid()) {
                    Text(
                        text = "La solicitud no tiene todos los datos necesarios.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                } else {
                    OutlinedTextField(
                        value = solicitud!!.nombre ?: "",
                        onValueChange = { /* Read-only */ },
                        label = { Text("Nombre del Restaurante") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = solicitud!!.direccion ?: "",
                        onValueChange = { /* Read-only */ },
                        label = { Text("Dirección") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = solicitud!!.municipio ?: "",
                        onValueChange = { /* Read-only */ },
                        label = { Text("Municipio") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = solicitud!!.provincia ?: "",
                        onValueChange = { /* Read-only */ },
                        label = { Text("Provincia") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = "Latitud: ${solicitud!!.latitud ?: "0.0"}, Longitud: ${solicitud!!.longitud ?: "0.0"}",
                        onValueChange = { /* Read-only */ },
                        label = { Text("Coordenadas") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    solicitud?.telefono?.let { telefono ->
                        OutlinedTextField(
                            value = telefono,
                            onValueChange = { /* Read-only */ },
                            label = { Text("Teléfono (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    solicitud?.horario?.let { horario ->
                        OutlinedTextField(
                            value = horario,
                            onValueChange = { /* Read-only */ },
                            label = { Text("Horario (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    solicitud?.valoracion?.let { valoracion ->
                        OutlinedTextField(
                            value = valoracion,
                            onValueChange = { /* Read-only */ },
                            label = { Text("Valoración (opcional)") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botones "Aceptar Solicitud" y "Cancelar"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        // Generar un nuevo ID único para el Lugar
                                        val newLugarId = FirebaseFirestore.getInstance()
                                            .collection("Locales")
                                            .document()
                                            .id

                                        // Crear el Lugar con el nuevo ID
                                        val lugar = Lugar(
                                            id = newLugarId,
                                            nombre = solicitud!!.nombre!!,
                                            direccion = solicitud!!.direccion!!,
                                            provincia = solicitud!!.provincia!!,
                                            municipio = solicitud!!.municipio!!,
                                            latitud = solicitud!!.latitud, // Usar directamente latitud como String?
                                            longitud = solicitud!!.longitud, // Usar directamente longitud como String?
                                            telefono = solicitud!!.telefono,
                                            horario = solicitud!!.horario,
                                            valoracion = solicitud!!.valoracion
                                        )

                                        // Agregar el Lugar a la colección "Locales"
                                        LugarRepository().agregarLugar(lugar) { success ->
                                            if (success) {
                                                // Eliminar la solicitud de la colección "Solicitudes"
                                                FirebaseFirestore.getInstance()
                                                    .collection("Solicitudes")
                                                    .document(solicitud!!.id!!)
                                                    .delete()
                                                    .addOnSuccessListener {
                                                        mensajeExito = true
                                                        navController.popBackStack()
                                                    }
                                                    .addOnFailureListener { e ->
                                                        errorMensaje = "Error al eliminar solicitud: ${e.message}"
                                                    }
                                            } else {
                                                errorMensaje = "Error al agregar lugar a Locales."
                                            }
                                        }
                                    } catch (e: Exception) {
                                        errorMensaje = "Error al aceptar solicitud: ${e.message}"
                                    }
                                }
                            },
                            enabled = solicitud!!.isValid(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Aceptar Solicitud")
                        }

                        Button(
                            onClick = {
                                // Regresar a AdminSolicitudes sin hacer cambios
                                navController.popBackStack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Cancelar")
                        }
                    }
                }
            } else {
                Text(
                    text = "Solicitud no encontrada.",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}